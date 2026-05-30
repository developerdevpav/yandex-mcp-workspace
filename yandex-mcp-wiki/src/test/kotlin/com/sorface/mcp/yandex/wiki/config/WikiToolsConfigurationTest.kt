package com.sorface.mcp.yandex.wiki.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.auth.api.AuthTools
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.system.api.SystemTools
import com.sorface.mcp.yandex.wiki.api.WikiGridTools
import com.sorface.mcp.yandex.wiki.api.WikiTools
import com.sorface.mcp.yandex.wiki.api.WikiWriteTools
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallbackProvider

@DisplayName("Регистрация инструментов MCP-сервера Wiki")
class WikiToolsConfigurationTest {

    private val configuration = WikiToolsConfiguration()
    private val objectMapper = ObjectMapper()

    private fun provider(readOnly: Boolean): ToolCallbackProvider =
        configuration.wikiToolCallbackProvider(
            properties = YandexProperties(readOnly = readOnly),
            systemTools = SystemTools(YandexProperties(readOnly = readOnly)),
            authTools = AuthTools(mockk(relaxed = true)),
            wikiTools = WikiTools(mockk(relaxed = true), objectMapper),
            wikiWriteTools = WikiWriteTools(mockk(relaxed = true), objectMapper),
            wikiGridTools = WikiGridTools(mockk(relaxed = true), objectMapper),
        )

    private fun toolNames(provider: ToolCallbackProvider): List<String> =
        provider.toolCallbacks.map { it.toolDefinition.name() }

    @Test
    @DisplayName("Регистрируются только служебные, авторизационные и Wiki-инструменты")
    fun `registers only wiki tools`() {
        val names = toolNames(provider(readOnly = false))

        assertThat(names).contains("system_ping", "yandex_auth_status", "wiki_page_get_by_slug", "wiki_page_create")
        assertThat(names).noneMatch { it.startsWith("tracker_") }
    }

    @Test
    @DisplayName("В режиме только для чтения изменяющие Wiki-инструменты не регистрируются")
    fun `hides wiki write tools in read-only`() {
        val names = toolNames(provider(readOnly = true))

        assertThat(names).contains("wiki_page_get_by_slug", "wiki_grid_get", "system_server_info")
        assertThat(names).doesNotContain("wiki_page_create")
    }
}
