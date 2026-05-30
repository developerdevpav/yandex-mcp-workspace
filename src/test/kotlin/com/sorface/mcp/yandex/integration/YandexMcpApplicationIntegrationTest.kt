package com.sorface.mcp.yandex.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.sorface.mcp.yandex.YandexMcpApplication
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [YandexMcpApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@DisplayName("Интеграционный тест MCP-сервера (полный контекст Spring)")
class YandexMcpApplicationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            ExternalApiMockSupport.registerProperties(registry)
        }
    }

    @BeforeEach
    fun resetMocks() {
        ExternalApiMockSupport.resetServers()
    }

    @Test
    @DisplayName("MockMvc: system_ping не обращается к внешним API")
    fun `system ping does not call external apis`() {
        mockMvc.post("/integration/tools/system_ping") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isOk() }
            content { string(containsString("pong")) }
        }

        ExternalApiMockSupport.trackerServer.verify(0, getRequestedFor(urlEqualTo("/v3/myself")))
        ExternalApiMockSupport.wikiServer.verify(0, getRequestedFor(urlPathEqualTo("/v1/pages")))
    }

    @Test
    @DisplayName("MockMvc: yandex_auth_status читает локальное хранилище токенов")
    fun `auth status reports authorized without oauth calls`() {
        mockMvc.post("/integration/tools/yandex_auth_status") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isOk() }
            content { string(containsString("авторизован: да")) }
            content { string(containsString("настройки заданы: да")) }
        }

        ExternalApiMockSupport.trackerServer.verify(0, postRequestedFor(urlPathEqualTo("/token")))
    }

    @Test
    @DisplayName("MockMvc: tracker_myself проходит цепочку Tool → Service → Client → WireMock")
    fun `tracker myself reaches tracker api with auth headers`() {
        ExternalApiMockSupport.trackerServer.stubFor(
            get(urlEqualTo("/v3/myself")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"login":"integration-user","uid":1001}"""),
            ),
        )

        mockMvc.post("/integration/tools/tracker_myself") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isOk() }
            content { string(containsString("integration-user")) }
        }

        ExternalApiMockSupport.trackerServer.verify(
            getRequestedFor(urlEqualTo("/v3/myself"))
                .withHeader("Authorization", equalTo("OAuth ${ExternalApiMockSupport.ACCESS_TOKEN}"))
                .withHeader(ExternalApiMockSupport.ORG_HEADER, equalTo(ExternalApiMockSupport.ORG_ID)),
        )
    }

    @Test
    @DisplayName("MockMvc: tracker_issue_get запрашивает задачу у Tracker API")
    fun `tracker issue get calls tracker api`() {
        ExternalApiMockSupport.trackerServer.stubFor(
            get(urlPathEqualTo("/v3/issues/TREK-42")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"key":"TREK-42","summary":"Integration task"}"""),
            ),
        )

        mockMvc.post("/integration/tools/tracker_issue_get") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"key":"TREK-42"}"""
        }.andExpect {
            status { isOk() }
            content { string(containsString("TREK-42")) }
            content { string(containsString("Integration task")) }
        }

        ExternalApiMockSupport.trackerServer.verify(
            getRequestedFor(urlPathEqualTo("/v3/issues/TREK-42"))
                .withHeader("Authorization", equalTo("OAuth ${ExternalApiMockSupport.ACCESS_TOKEN}")),
        )
    }

    @Test
    @DisplayName("MockMvc: tracker_issue_create отправляет POST в Tracker API")
    fun `tracker issue create posts to tracker api`() {
        ExternalApiMockSupport.trackerServer.stubFor(
            post(urlEqualTo("/v3/issues"))
                .withRequestBody(equalToJson("""{"queue":"TREK","summary":"Created via integration test"}"""))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"key":"TREK-99","summary":"Created via integration test"}"""),
                ),
        )

        mockMvc.post("/integration/tools/tracker_issue_create") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"queue":"TREK","summary":"Created via integration test"}"""
        }.andExpect {
            status { isOk() }
            content { string(containsString("TREK-99")) }
        }

        ExternalApiMockSupport.trackerServer.verify(
            postRequestedFor(urlEqualTo("/v3/issues"))
                .withHeader("Authorization", equalTo("OAuth ${ExternalApiMockSupport.ACCESS_TOKEN}"))
                .withHeader(ExternalApiMockSupport.ORG_HEADER, equalTo(ExternalApiMockSupport.ORG_ID)),
        )
    }

    @Test
    @DisplayName("MockMvc: wiki_page_get_by_slug запрашивает страницу у Wiki API")
    fun `wiki page get by slug calls wiki api`() {
        ExternalApiMockSupport.wikiServer.stubFor(
            get(urlPathEqualTo("/v1/pages"))
                .withQueryParam("slug", equalTo("team/onboarding"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"slug":"team/onboarding","title":"Onboarding"}"""),
                ),
        )

        mockMvc.post("/integration/tools/wiki_page_get_by_slug") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"slug":"team/onboarding"}"""
        }.andExpect {
            status { isOk() }
            content { string(containsString("Onboarding")) }
        }

        ExternalApiMockSupport.wikiServer.verify(
            getRequestedFor(urlPathEqualTo("/v1/pages"))
                .withQueryParam("slug", equalTo("team/onboarding"))
                .withHeader("Authorization", equalTo("OAuth ${ExternalApiMockSupport.ACCESS_TOKEN}"))
                .withHeader(ExternalApiMockSupport.ORG_HEADER, equalTo(ExternalApiMockSupport.ORG_ID)),
        )
    }

    @Test
    @DisplayName("MockMvc: wiki_page_create отправляет POST в Wiki API")
    fun `wiki page create posts to wiki api`() {
        ExternalApiMockSupport.wikiServer.stubFor(
            post(urlEqualTo("/v1/pages"))
                .withRequestBody(equalToJson("""{"title":"Wiki integration","slug":"team/new"}"""))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id":"42","title":"Wiki integration"}"""),
                ),
        )

        mockMvc.post("/integration/tools/wiki_page_create") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"Wiki integration","slug":"team/new"}"""
        }.andExpect {
            status { isOk() }
            content { string(containsString("Wiki integration")) }
        }

        ExternalApiMockSupport.wikiServer.verify(
            postRequestedFor(urlEqualTo("/v1/pages"))
                .withHeader("Authorization", equalTo("OAuth ${ExternalApiMockSupport.ACCESS_TOKEN}")),
        )
    }

    @Test
    @DisplayName("MockMvc: список инструментов включает ключевые группы компонентов")
    fun `tool list includes major component groups`() {
        val response = mockMvc.get("/integration/tools").andReturn().response.contentAsString
        val tools = objectMapper.readValue(response, List::class.java).map { it as String }

        assertThat(tools).contains(
            "system_ping",
            "system_server_info",
            "yandex_auth_status",
            "tracker_myself",
            "tracker_issue_create",
            "wiki_page_get_by_slug",
            "wiki_page_create",
            "wiki_grid_get",
        )
    }
}
