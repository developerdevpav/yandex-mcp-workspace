package com.sorface.mcp.yandex.tracker.infrastructure

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.sorface.mcp.yandex.common.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@DisplayName("Низкоуровневый клиент Tracker (TrackerClient)")
class TrackerClientTest {

    private lateinit var server: WireMockServer
    private lateinit var client: TrackerClient

    @BeforeEach
    fun setUp() {
        server = WireMockServer(options().dynamicPort())
        server.start()
        // WireMock (Jetty) и JDK HttpClient по HTTP/2 иногда дают RST_STREAM, поэтому
        // в тесте принудительно используется HTTP/1.1.
        val httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
        val restClient = RestClient.builder()
            .baseUrl(server.baseUrl())
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .build()
        client = TrackerClient(restClient)
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }

    @Test
    @DisplayName("GET разбирает тело ответа в JSON")
    fun `get parses body`() {
        server.stubFor(
            get(urlEqualTo("/v3/myself")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"login":"john","uid":42}"""),
            ),
        )

        val body = client.get("/v3/myself")

        assertThat(body.path("login").asText()).isEqualTo("john")
        assertThat(body.path("uid").asInt()).isEqualTo(42)
    }

    @Test
    @DisplayName("GET прокидывает параметры строки запроса и пропускает null")
    fun `get passes query params`() {
        server.stubFor(
            get(urlPathEqualTo("/v3/queues")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("[]"),
            ),
        )

        client.get("/v3/queues", mapOf("perPage" to "10", "expand" to null))

        server.verify(
            com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlPathEqualTo("/v3/queues"))
                .withQueryParam("perPage", com.github.tomakehurst.wiremock.client.WireMock.equalTo("10"))
                .withoutQueryParam("expand"),
        )
    }

    @Test
    @DisplayName("Постраничный GET читает заголовки X-Total-Count и X-Total-Pages")
    fun `getPaged reads pagination headers`() {
        server.stubFor(
            get(urlPathEqualTo("/v3/queues")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withHeader("X-Total-Count", "120")
                    .withHeader("X-Total-Pages", "3")
                    .withBody("[{\"key\":\"TREK\"}]"),
            ),
        )

        val result = client.getPaged("/v3/queues")

        assertThat(result.totalCount).isEqualTo(120)
        assertThat(result.totalPages).isEqualTo(3)
        assertThat(result.items.isArray).isTrue()
    }

    @Test
    @DisplayName("POST отправляет тело JSON и возвращает ответ")
    fun `post sends json body`() {
        server.stubFor(
            post(urlEqualTo("/v3/issues/_count"))
                .withRequestBody(equalToJson("""{"query":"queue: TREK"}"""))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("7"),
                ),
        )

        val body = client.post("/v3/issues/_count", mapOf("query" to "queue: TREK"))

        assertThat(body.asLong()).isEqualTo(7)
    }

    @Test
    @DisplayName("PATCH отправляет тело JSON и прокидывает параметр version")
    fun `patch sends body and version`() {
        server.stubFor(
            patch(urlPathEqualTo("/v3/issues/TREK-1"))
                .withRequestBody(equalToJson("""{"summary":"new"}"""))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"key":"TREK-1","version":3}"""),
                ),
        )

        val body = client.patch(
            "/v3/issues/TREK-1",
            mapOf("summary" to "new"),
            mapOf("version" to "2"),
        )

        assertThat(body.path("version").asInt()).isEqualTo(3)
        server.verify(
            com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor(urlPathEqualTo("/v3/issues/TREK-1"))
                .withQueryParam("version", com.github.tomakehurst.wiremock.client.WireMock.equalTo("2")),
        )
    }

    @Test
    @DisplayName("DELETE с ответом 204 возвращает null-узел")
    fun `delete handles no content`() {
        server.stubFor(
            delete(urlEqualTo("/v3/issues/TREK-1/comments/10")).willReturn(
                aResponse().withStatus(204),
            ),
        )

        val body = client.delete("/v3/issues/TREK-1/comments/10")

        assertThat(body.isNull).isTrue()
    }

    @Test
    @DisplayName("Ошибка 404 переводится в ApiException с сообщением из тела")
    fun `maps 404 to ApiException`() {
        server.stubFor(
            get(urlEqualTo("/v3/issues/NONE-1")).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"statusCode":404,"errorMessages":["Issue does not exist."]}"""),
            ),
        )

        assertThatThrownBy { client.get("/v3/issues/NONE-1") }
            .isInstanceOf(ApiException::class.java)
            .hasMessageContaining("не найден")
            .hasMessageContaining("Issue does not exist.")
    }

    @Test
    @DisplayName("Ошибка 403 переводится в ApiException с верным статусом")
    fun `maps 403 to ApiException`() {
        server.stubFor(
            get(urlEqualTo("/v3/myself")).willReturn(
                aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"errors":{"access":"forbidden"}}"""),
            ),
        )

        val thrown = catchApiException { client.get("/v3/myself") }

        assertThat(thrown.status).isEqualTo(403)
        assertThat(thrown.message).contains("Недостаточно прав")
    }

    private fun catchApiException(block: () -> Unit): ApiException =
        runCatching { block() }.exceptionOrNull() as? ApiException
            ?: error("Ожидалось исключение ApiException")
}
