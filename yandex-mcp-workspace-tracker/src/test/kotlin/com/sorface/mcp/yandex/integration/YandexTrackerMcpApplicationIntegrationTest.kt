package com.sorface.mcp.yandex.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.sorface.mcp.yandex.tracker.YandexTrackerMcpApplication
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

@SpringBootTest(classes = [YandexTrackerMcpApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@DisplayName("Интеграционный тест независимого MCP-сервера Tracker")
class YandexTrackerMcpApplicationIntegrationTest {

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
    @DisplayName("MockMvc: tracker_myself проходит цепочку Tool -> Service -> Client -> WireMock")
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
    @DisplayName("MockMvc: список инструментов содержит Tracker и не содержит Wiki")
    fun `tool list contains tracker but not wiki`() {
        val response = mockMvc.get("/integration/tools").andReturn().response.contentAsString
        val tools = objectMapper.readValue(response, List::class.java).map { it as String }

        assertThat(tools).contains("system_ping", "yandex_auth_status", "tracker_myself", "tracker_issue_create")
        assertThat(tools).noneMatch { it.startsWith("wiki_") }
    }
}
