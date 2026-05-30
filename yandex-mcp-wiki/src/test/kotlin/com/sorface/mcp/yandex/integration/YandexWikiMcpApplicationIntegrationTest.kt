package com.sorface.mcp.yandex.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.sorface.mcp.yandex.wiki.YandexWikiMcpApplication
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

@SpringBootTest(classes = [YandexWikiMcpApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@DisplayName("Интеграционный тест независимого MCP-сервера Wiki")
class YandexWikiMcpApplicationIntegrationTest {

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
    @DisplayName("MockMvc: wiki_page_get_by_slug проходит цепочку Tool -> Service -> Client -> WireMock")
    fun `wiki page get by slug reaches wiki api with auth headers`() {
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
    @DisplayName("MockMvc: список инструментов содержит Wiki и не содержит Tracker")
    fun `tool list contains wiki but not tracker`() {
        val response = mockMvc.get("/integration/tools").andReturn().response.contentAsString
        val tools = objectMapper.readValue(response, List::class.java).map { it as String }

        assertThat(tools).contains("system_ping", "yandex_auth_status", "wiki_page_get_by_slug", "wiki_page_create")
        assertThat(tools).noneMatch { it.startsWith("tracker_") }
    }
}
