package com.sorface.mcp.yandex.integration

import com.sorface.mcp.yandex.YandexMcpApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [YandexMcpApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@TestPropertySource(properties = ["yandex.read-only=true"])
@DisplayName("Интеграционный тест режима только для чтения")
class YandexMcpReadOnlyIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            ExternalApiMockSupport.registerProperties(registry)
            registry.add("yandex.read-only") { "true" }
        }
    }

    @Test
    @DisplayName("MockMvc: изменяющие инструменты не регистрируются в read-only")
    fun `read only hides write tools from list`() {
        val response = mockMvc.get("/integration/tools").andReturn().response.contentAsString

        assertThat(response).doesNotContain("tracker_issue_create")
        assertThat(response).doesNotContain("wiki_page_create")
        assertThat(response).contains("tracker_myself")
        assertThat(response).contains("system_server_info")
    }

    @Test
    @DisplayName("MockMvc: вызов изменяющего инструмента возвращает 404")
    fun `read only rejects write tool invocation`() {
        mockMvc.post("/integration/tools/tracker_issue_create") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"queue":"TREK","summary":"blocked"}"""
        }.andExpect {
            status { isNotFound() }
        }

        ExternalApiMockSupport.trackerServer.verify(0, com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
            com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/v3/issues"),
        ))
    }
}
