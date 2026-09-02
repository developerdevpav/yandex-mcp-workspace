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
    @DisplayName("MockMvc: tracker_external_link_create создаёт remotelink с backlink")
    fun `external link create reaches remotelinks api`() {
        ExternalApiMockSupport.trackerServer.stubFor(
            post(urlPathEqualTo("/v3/issues/TREK-1/remotelinks"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id":"51","object":{"key":"wiki-page-key"}}"""),
                ),
        )

        mockMvc.post("/integration/tools/tracker_external_link_create") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "key": "TREK-1",
                  "relationship": "RELATES",
                  "objectKey": "wiki-page-key",
                  "origin": "wiki-application-id",
                  "backlink": true
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            content { string(containsString("wiki-page-key")) }
        }

        ExternalApiMockSupport.trackerServer.verify(
            postRequestedFor(urlPathEqualTo("/v3/issues/TREK-1/remotelinks"))
                .withQueryParam("backlink", equalTo("true"))
                .withRequestBody(
                    equalToJson(
                        """{"relationship":"RELATES","key":"wiki-page-key","origin":"wiki-application-id"}""",
                    ),
                )
                .withHeader("Authorization", equalTo("OAuth ${ExternalApiMockSupport.ACCESS_TOKEN}"))
                .withHeader(ExternalApiMockSupport.ORG_HEADER, equalTo(ExternalApiMockSupport.ORG_ID)),
        )
    }

    @Test
    @DisplayName("MockMvc: tracker_entity_create создаёт цель через Entities API")
    fun `entity create reaches entities api`() {
        ExternalApiMockSupport.trackerServer.stubFor(
            post(urlPathEqualTo("/v3/entities/goal"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id":"goal-1","entityType":"goal","fields":{"summary":"Рост продукта"}}"""),
                ),
        )

        mockMvc.post("/integration/tools/tracker_entity_create") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "entityType": "goal",
                  "summary": "Рост продукта",
                  "entityStatus": "draft"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            content { string(containsString("goal-1")) }
        }

        ExternalApiMockSupport.trackerServer.verify(
            postRequestedFor(urlPathEqualTo("/v3/entities/goal"))
                .withRequestBody(
                    equalToJson(
                        """{"fields":{"summary":"Рост продукта","entityStatus":"draft"}}""",
                    ),
                )
                .withHeader("Authorization", equalTo("OAuth ${ExternalApiMockSupport.ACCESS_TOKEN}"))
                .withHeader(ExternalApiMockSupport.ORG_HEADER, equalTo(ExternalApiMockSupport.ORG_ID)),
        )
    }

    @Test
    @DisplayName("MockMvc: список инструментов содержит Tracker и не содержит Wiki")
    fun `tool list contains tracker but not wiki`() {
        val response = mockMvc.get("/integration/tools").andReturn().response.contentAsString
        val tools = objectMapper.readValue(response, List::class.java).map { it as String }

        assertThat(tools).contains(
            "system_ping",
            "yandex_auth_status",
            "yandex_auth_start",
            "yandex_auth_poll",
            "yandex_auth_logout",
            "tracker_myself",
            "tracker_issue_create",
            "tracker_external_application_list",
            "tracker_external_link_list",
            "tracker_external_link_create",
            "tracker_external_link_delete",
            "tracker_entity_get",
            "tracker_entity_search",
            "tracker_entity_create",
            "tracker_entity_checklist_replace",
            "tracker_temporary_attachment_upload",
            "tracker_entity_access_update",
            "tracker_goal_key_result_update",
            "tracker_entity_metric_clear",
        )
        assertThat(tools).noneMatch { it.startsWith("wiki_") }
    }
}
