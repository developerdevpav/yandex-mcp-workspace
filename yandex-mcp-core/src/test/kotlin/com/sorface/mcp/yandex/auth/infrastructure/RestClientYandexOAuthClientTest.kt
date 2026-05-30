package com.sorface.mcp.yandex.auth.infrastructure

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.sorface.mcp.yandex.auth.application.TokenPollResult
import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.config.YandexProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DisplayName("Клиент Яндекс OAuth (RestClientYandexOAuthClient)")
class RestClientYandexOAuthClientTest {

    private val now: Instant = Instant.parse("2026-01-01T12:00:00Z")
    private lateinit var server: WireMockServer
    private lateinit var client: RestClientYandexOAuthClient

    @BeforeEach
    fun setUp() {
        server = WireMockServer(options().dynamicPort())
        server.start()
        val properties = YandexProperties(
            clientId = "client-id",
            clientSecret = "client-secret",
            oauth = YandexProperties.OAuthProperties(baseUrl = server.baseUrl()),
        )
        val restClient = RestClient.builder().baseUrl(server.baseUrl()).build()
        client = RestClientYandexOAuthClient(restClient, properties, Clock.fixed(now, ZoneOffset.UTC))
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }

    @Test
    @DisplayName("Запрос кода устройства разбирает поля ответа")
    fun `requestDeviceCode parses response`() {
        server.stubFor(
            post(urlEqualTo("/device/code")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """{"device_code":"dev-1","user_code":"ABCD-1234",
                           "verification_url":"https://ya.ru/device","interval":5,"expires_in":300}""",
                    ),
            ),
        )

        val result = client.requestDeviceCode("")

        assertThat(result.deviceCode).isEqualTo("dev-1")
        assertThat(result.userCode).isEqualTo("ABCD-1234")
        assertThat(result.verificationUrl).isEqualTo("https://ya.ru/device")
        assertThat(result.intervalSeconds).isEqualTo(5)
        assertThat(result.expiresInSeconds).isEqualTo(300)
    }

    @Test
    @DisplayName("Запрос кода устройства сообщает ошибку OAuth при неверном client_id")
    fun `requestDeviceCode reports oauth error`() {
        server.stubFor(
            post(urlEqualTo("/device/code")).willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"error":"invalid_client","error_description":"Client not found"}"""),
            ),
        )

        assertThatThrownBy { client.requestDeviceCode("") }
            .isInstanceOf(AuthorizationException::class.java)
            .hasMessageContaining("invalid_client")
            .hasMessageContaining("Client not found")
    }

    @Test
    @DisplayName("Опрос статуса возвращает ожидание при authorization_pending")
    fun `pollToken returns pending`() {
        server.stubFor(
            post(urlEqualTo("/token")).willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"error":"authorization_pending"}"""),
            ),
        )

        val result = client.pollToken("dev-1")

        assertThat(result).isEqualTo(TokenPollResult.Pending(slowDown = false))
    }

    @Test
    @DisplayName("Опрос статуса возвращает токены при успешном подтверждении")
    fun `pollToken returns success`() {
        server.stubFor(
            post(urlEqualTo("/token")).withRequestBody(containing("grant_type=device_code")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """{"access_token":"acc-1","refresh_token":"ref-1",
                           "token_type":"bearer","expires_in":3600}""",
                    ),
            ),
        )

        val result = client.pollToken("dev-1")

        assertThat(result).isInstanceOf(TokenPollResult.Success::class.java)
        val tokenSet = (result as TokenPollResult.Success).tokenSet
        assertThat(tokenSet.accessToken).isEqualTo("acc-1")
        assertThat(tokenSet.refreshToken).isEqualTo("ref-1")
        assertThat(tokenSet.expiresAt).isEqualTo(now.plusSeconds(3600))
    }

    @Test
    @DisplayName("Обновление токена возвращает новый набор токенов")
    fun `refresh returns new token set`() {
        server.stubFor(
            post(urlEqualTo("/token")).withRequestBody(containing("grant_type=refresh_token")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """{"access_token":"acc-2","refresh_token":"ref-2",
                           "token_type":"bearer","expires_in":7200}""",
                    ),
            ),
        )

        val tokenSet = client.refresh("ref-1")

        assertThat(tokenSet.accessToken).isEqualTo("acc-2")
        assertThat(tokenSet.expiresAt).isEqualTo(now.plusSeconds(7200))
    }
}
