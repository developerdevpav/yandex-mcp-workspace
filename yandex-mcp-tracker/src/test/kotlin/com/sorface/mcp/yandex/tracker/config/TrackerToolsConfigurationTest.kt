package com.sorface.mcp.yandex.tracker.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.auth.api.AuthTools
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.system.api.SystemTools
import com.sorface.mcp.yandex.tracker.api.TrackerTools
import com.sorface.mcp.yandex.tracker.api.TrackerWriteTools
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallbackProvider

@DisplayName("Регистрация инструментов MCP-сервера Tracker")
class TrackerToolsConfigurationTest {

    private val configuration = TrackerToolsConfiguration()
    private val objectMapper = ObjectMapper()

    private fun provider(readOnly: Boolean): ToolCallbackProvider =
        configuration.trackerToolCallbackProvider(
            properties = YandexProperties(readOnly = readOnly),
            systemTools = SystemTools(YandexProperties(readOnly = readOnly)),
            authTools = AuthTools(mockk(relaxed = true)),
            trackerTools = TrackerTools(mockk(relaxed = true), objectMapper),
            trackerWriteTools = TrackerWriteTools(mockk(relaxed = true), objectMapper),
        )

    private fun toolNames(provider: ToolCallbackProvider): List<String> =
        provider.toolCallbacks.map { it.toolDefinition.name() }

    @Test
    @DisplayName("Регистрируются только служебные, авторизационные и Tracker-инструменты")
    fun `registers only tracker tools`() {
        val names = toolNames(provider(readOnly = false))

        assertThat(names).contains("system_ping", "yandex_auth_status", "tracker_issue_get", "tracker_issue_create")
        assertThat(names).noneMatch { it.startsWith("wiki_") }
    }

    @Test
    @DisplayName("В режиме только для чтения изменяющие Tracker-инструменты не регистрируются")
    fun `hides tracker write tools in read-only`() {
        val names = toolNames(provider(readOnly = true))

        assertThat(names).contains("tracker_issue_get", "system_server_info")
        assertThat(names).doesNotContain("tracker_issue_create")
    }
}
