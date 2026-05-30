package com.sorface.mcp.yandex.wiki.config

import com.sorface.mcp.yandex.auth.api.AuthTools
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.system.api.SystemTools
import com.sorface.mcp.yandex.wiki.api.WikiGridTools
import com.sorface.mcp.yandex.wiki.api.WikiTools
import com.sorface.mcp.yandex.wiki.api.WikiWriteTools
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Регистрация инструментов независимого MCP-сервера Wiki.
 *
 * @author Sorface Developer
 */
@Configuration
class WikiToolsConfiguration {

    /**
     * Собирает служебные, авторизационные и Wiki-инструменты в провайдер MCP.
     */
    @Bean
    fun wikiToolCallbackProvider(
        properties: YandexProperties,
        systemTools: SystemTools,
        authTools: AuthTools,
        wikiTools: WikiTools,
        wikiWriteTools: WikiWriteTools,
        wikiGridTools: WikiGridTools,
    ): ToolCallbackProvider {
        val toolObjects = buildList {
            add(systemTools)
            add(authTools)
            add(wikiTools)
            add(wikiGridTools)
            if (!properties.readOnly) {
                add(wikiWriteTools)
            }
        }
        return MethodToolCallbackProvider.builder()
            .toolObjects(*toolObjects.toTypedArray())
            .build()
    }
}
