package com.sorface.mcp.yandex.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.auth.api.AuthTools
import com.sorface.mcp.yandex.system.api.SystemTools
import com.sorface.mcp.yandex.tracker.api.TrackerTools
import com.sorface.mcp.yandex.tracker.api.TrackerWriteTools
import com.sorface.mcp.yandex.wiki.api.WikiGridTools
import com.sorface.mcp.yandex.wiki.api.WikiTools
import com.sorface.mcp.yandex.wiki.api.WikiWriteTools
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallbackProvider

@DisplayName("Регистрация инструментов MCP (ToolsConfiguration)")
class ToolsConfigurationTest {

    private val configuration = ToolsConfiguration()
    private val objectMapper = ObjectMapper()

    private fun provider(readOnly: Boolean): ToolCallbackProvider =
        configuration.yandexToolCallbackProvider(
            properties = YandexProperties(readOnly = readOnly),
            systemTools = SystemTools(YandexProperties(readOnly = readOnly)),
            authTools = AuthTools(mockk(relaxed = true)),
            trackerTools = TrackerTools(mockk(relaxed = true), objectMapper),
            trackerWriteTools = TrackerWriteTools(mockk(relaxed = true), objectMapper),
            wikiTools = WikiTools(mockk(relaxed = true), objectMapper),
            wikiWriteTools = WikiWriteTools(mockk(relaxed = true), objectMapper),
            wikiGridTools = WikiGridTools(mockk(relaxed = true), objectMapper),
        )

    private fun toolNames(provider: ToolCallbackProvider): List<String> =
        provider.toolCallbacks.map { it.toolDefinition.name() }

    @Test
    @DisplayName("В обычном режиме регистрируются изменяющие инструменты")
    fun `registers write tools by default`() {
        val names = toolNames(provider(readOnly = false))

        assertThat(names).contains("tracker_issue_create", "wiki_page_create")
        assertThat(names).contains("tracker_issue_get", "system_ping", "system_server_info")
    }

    @Test
    @DisplayName("В режиме только для чтения изменяющие инструменты не регистрируются")
    fun `hides write tools in read-only`() {
        val names = toolNames(provider(readOnly = true))

        assertThat(names).noneMatch { it.startsWith("tracker_issue_create") }
        assertThat(names.none { it == "tracker_issue_create" }).isTrue()
        assertThat(names.none { it == "wiki_page_create" }).isTrue()
        assertThat(names).contains("tracker_issue_get", "system_server_info")
    }
}
