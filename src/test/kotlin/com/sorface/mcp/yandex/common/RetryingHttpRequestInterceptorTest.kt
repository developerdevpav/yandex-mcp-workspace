package com.sorface.mcp.yandex.common

import com.fasterxml.jackson.databind.JsonNode
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.config.YandexProperties.RetryProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.io.IOException
import java.net.http.HttpClient
import java.time.Duration

@DisplayName("Интерсептор повторных запросов (RetryingHttpRequestInterceptor)")
class RetryingHttpRequestInterceptorTest {

    private lateinit var server: WireMockServer
    private val recordedDelays = mutableListOf<Long>()

    @BeforeEach
    fun setUp() {
        server = WireMockServer(options().dynamicPort())
        server.start()
        recordedDelays.clear()
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }

    /**
     * Строит RestClient так же, как в продакшене: маркерный интерсептор (имитирует авторизацию)
     * добавляется первым, интерсептор повторов — последним. Реальные паузы заменены записью
     * длительностей, чтобы тест выполнялся мгновенно.
     */
    private fun restClient(retry: RetryProperties): RestClient {
        val properties = YandexProperties(retry = retry)
        val retryInterceptor = RetryingHttpRequestInterceptor(properties) { recordedDelays += it }
        val httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
        return RestClient.builder()
            .baseUrl(server.baseUrl())
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .requestInterceptor { request, body, execution ->
                request.headers.set("X-Marker", "set-once")
                execution.execute(request, body)
            }
            .requestInterceptor(retryInterceptor)
            .build()
    }

    private fun getBody(client: RestClient, path: String): JsonNode? =
        client.get().uri(path).exchange { _, response -> response.bodyTo(JsonNode::class.java) }

    private fun call(client: RestClient, path: String) {
        getBody(client, path)
    }

    @Test
    @DisplayName("Повтор после 429 и успешный ответ со второй попытки")
    fun `retries on 429 then succeeds`() {
        val scenario = "retry-429"
        server.stubFor(
            get(urlEqualTo("/v3/myself")).inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("second"),
        )
        server.stubFor(
            get(urlEqualTo("/v3/myself")).inScenario(scenario)
                .whenScenarioStateIs("second")
                .willReturn(
                    aResponse().withHeader("Content-Type", "application/json").withBody("""{"login":"john"}"""),
                ),
        )

        val body = getBody(restClient(RetryProperties()), "/v3/myself")

        assertThat(body?.path("login")?.asText()).isEqualTo("john")
        server.verify(2, getRequestedFor(urlEqualTo("/v3/myself")))
        assertThat(recordedDelays).hasSize(1)
    }

    @Test
    @DisplayName("Повтор после 503 с экспоненциальным ростом задержки")
    fun `retries on 503 with exponential backoff`() {
        server.stubFor(get(urlEqualTo("/v3/myself")).willReturn(aResponse().withStatus(503)))

        val retry = RetryProperties(
            maxAttempts = 3,
            initialDelay = Duration.ofMillis(100),
            multiplier = 2.0,
            maxDelay = Duration.ofSeconds(10),
        )

        call(restClient(retry), "/v3/myself")

        server.verify(3, getRequestedFor(urlEqualTo("/v3/myself")))
        assertThat(recordedDelays).containsExactly(100L, 200L)
    }

    @Test
    @DisplayName("Задержка не превышает верхнюю границу maxDelay")
    fun `caps delay at maxDelay`() {
        server.stubFor(get(urlEqualTo("/v3/myself")).willReturn(aResponse().withStatus(500)))

        val retry = RetryProperties(
            maxAttempts = 4,
            initialDelay = Duration.ofMillis(100),
            multiplier = 10.0,
            maxDelay = Duration.ofMillis(150),
        )

        call(restClient(retry), "/v3/myself")

        assertThat(recordedDelays).containsExactly(100L, 150L, 150L)
    }

    @Test
    @DisplayName("Заголовок Retry-After задаёт длительность паузы для 429")
    fun `honors Retry-After header`() {
        val scenario = "retry-after"
        server.stubFor(
            get(urlEqualTo("/v3/myself")).inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withHeader(HttpHeaders.RETRY_AFTER, "2"))
                .willSetStateTo("ok"),
        )
        server.stubFor(
            get(urlEqualTo("/v3/myself")).inScenario(scenario)
                .whenScenarioStateIs("ok")
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{}")),
        )

        call(restClient(RetryProperties()), "/v3/myself")

        assertThat(recordedDelays).containsExactly(2000L)
    }

    @Test
    @DisplayName("Повтор после сетевой ошибки (IOException) и успех со второй попытки")
    fun `retries on network failure then succeeds`() {
        val properties = YandexProperties(retry = RetryProperties(initialDelay = Duration.ofMillis(50)))
        val interceptor = RetryingHttpRequestInterceptor(properties) { recordedDelays += it }
        val okResponse = mockk<ClientHttpResponse>(relaxed = true)
        every { okResponse.statusCode } returns HttpStatusCode.valueOf(200)
        var calls = 0
        val execution = ClientHttpRequestExecution { _, _ ->
            calls++
            if (calls == 1) throw IOException("connection reset") else okResponse
        }

        val result = interceptor.intercept(mockk(relaxed = true), ByteArray(0), execution)

        assertThat(result).isSameAs(okResponse)
        assertThat(calls).isEqualTo(2)
        assertThat(recordedDelays).containsExactly(50L)
    }

    @Test
    @DisplayName("Сетевая ошибка пробрасывается после исчерпания попыток")
    fun `rethrows network failure after exhausting attempts`() {
        val properties = YandexProperties(retry = RetryProperties(maxAttempts = 2, initialDelay = Duration.ofMillis(50)))
        val interceptor = RetryingHttpRequestInterceptor(properties) { recordedDelays += it }
        val execution = ClientHttpRequestExecution { _, _ -> throw IOException("service down") }

        assertThatThrownBy { interceptor.intercept(mockk(relaxed = true), ByteArray(0), execution) }
            .isInstanceOf(IOException::class.java)
            .hasMessageContaining("service down")

        assertThat(recordedDelays).hasSize(1)
    }

    @Test
    @DisplayName("Статус 404 не повторяется")
    fun `does not retry on 404`() {
        server.stubFor(get(urlEqualTo("/v3/none")).willReturn(aResponse().withStatus(404)))

        call(restClient(RetryProperties()), "/v3/none")

        server.verify(1, getRequestedFor(urlEqualTo("/v3/none")))
        assertThat(recordedDelays).isEmpty()
    }

    @Test
    @DisplayName("При выключенных повторах запрос выполняется один раз")
    fun `disabled retry executes once`() {
        server.stubFor(get(urlEqualTo("/v3/myself")).willReturn(aResponse().withStatus(503)))

        call(restClient(RetryProperties(enabled = false)), "/v3/myself")

        server.verify(1, getRequestedFor(urlEqualTo("/v3/myself")))
        assertThat(recordedDelays).isEmpty()
    }

    @Test
    @DisplayName("Заголовки авторизации сохраняются на повторных попытках")
    fun `preserves headers across retries`() {
        val scenario = "headers"
        server.stubFor(
            get(urlEqualTo("/v3/myself")).inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("ok"),
        )
        server.stubFor(
            get(urlEqualTo("/v3/myself")).inScenario(scenario)
                .whenScenarioStateIs("ok")
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{}")),
        )

        call(restClient(RetryProperties()), "/v3/myself")

        server.verify(2, getRequestedFor(urlEqualTo("/v3/myself")).withHeader("X-Marker", equalTo("set-once")))
    }
}
