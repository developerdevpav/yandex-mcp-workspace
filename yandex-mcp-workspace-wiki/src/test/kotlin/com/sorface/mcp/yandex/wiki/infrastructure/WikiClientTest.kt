package com.sorface.mcp.yandex.wiki.infrastructure

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
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

@DisplayName("Низкоуровневый клиент Wiki (WikiClient)")
class WikiClientTest {

    private lateinit var server: WireMockServer
    private lateinit var client: WikiClient

    @BeforeEach
    fun setUp() {
        server = WireMockServer(options().dynamicPort())
        server.start()
        val httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
        val restClient = RestClient.builder()
            .baseUrl(server.baseUrl())
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .build()
        client = WikiClient(restClient)
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }

    @Test
    @DisplayName("GET по slug прокидывает параметр и разбирает тело")
    fun `get by slug`() {
        server.stubFor(
            get(urlPathEqualTo("/v1/pages")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"id":10,"title":"Onboarding"}"""),
            ),
        )

        val body = client.get("/v1/pages", mapOf("slug" to "team/onboarding"))

        assertThat(body.path("title").asText()).isEqualTo("Onboarding")
        server.verify(
            com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlPathEqualTo("/v1/pages"))
                .withQueryParam("slug", equalTo("team/onboarding")),
        )
    }

    @Test
    @DisplayName("DELETE возвращает тело с токеном восстановления")
    fun `delete returns recovery token`() {
        server.stubFor(
            delete(urlEqualTo("/v1/pages/10")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"recovery_token":"rec-1"}"""),
            ),
        )

        val body = client.delete("/v1/pages/10")

        assertThat(body.path("recovery_token").asText()).isEqualTo("rec-1")
    }

    @Test
    @DisplayName("Бинарный PUT отправляет octet-stream и параметр part_number")
    fun `put binary uploads part`() {
        server.stubFor(
            put(urlPathEqualTo("/v1/upload_sessions/s-1/upload_part")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{}"),
            ),
        )

        client.putBinary("/v1/upload_sessions/s-1/upload_part", byteArrayOf(1, 2, 3), mapOf("part_number" to "1"))

        server.verify(
            putRequestedFor(urlPathEqualTo("/v1/upload_sessions/s-1/upload_part"))
                .withQueryParam("part_number", equalTo("1"))
                .withHeader("Content-Type", equalTo("application/octet-stream")),
        )
    }

    @Test
    @DisplayName("DELETE с телом отправляет JSON и возвращает ответ")
    fun `delete with body`() {
        server.stubFor(
            delete(urlEqualTo("/v1/grids/7/rows"))
                .withRequestBody(equalToJson("""{"row_ids":["r1","r2"]}"""))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"deleted":2}"""),
                ),
        )

        val body = client.deleteWithBody(
            "/v1/grids/7/rows",
            mapOf("row_ids" to listOf("r1", "r2")),
        )

        assertThat(body.path("deleted").asInt()).isEqualTo(2)
    }

    @Test
    @DisplayName("Ошибка 404 переводится в ApiException с сообщением Wiki")
    fun `maps 404`() {
        server.stubFor(
            get(urlEqualTo("/v1/pages/999")).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"human_message":"Страница не найдена"}"""),
            ),
        )

        assertThatThrownBy { client.get("/v1/pages/999") }
            .isInstanceOf(ApiException::class.java)
            .hasMessageContaining("Wiki")
            .hasMessageContaining("Страница не найдена")
    }
}
