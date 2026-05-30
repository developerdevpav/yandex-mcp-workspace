package com.sorface.mcp.yandex.tracker.config

import com.sorface.mcp.yandex.auth.api.AuthTools
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.system.api.SystemTools
import com.sorface.mcp.yandex.tracker.api.TrackerTools
import com.sorface.mcp.yandex.tracker.api.TrackerWriteTools
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Регистрация инструментов независимого MCP-сервера Tracker.
 *
 * @author Sorface Developer
 */
@Configuration
class TrackerToolsConfiguration {

    /**
     * Собирает служебные, авторизационные и Tracker-инструменты в провайдер MCP.
     */
    @Bean
    fun trackerToolCallbackProvider(
        properties: YandexProperties,
        systemTools: SystemTools,
        authTools: AuthTools,
        trackerTools: TrackerTools,
        trackerWriteTools: TrackerWriteTools,
    ): ToolCallbackProvider {
        val toolObjects = buildList {
            add(systemTools)
            add(authTools)
            add(trackerTools)
            if (!properties.readOnly) {
                add(trackerWriteTools)
            }
        }
        return MethodToolCallbackProvider.builder()
            .toolObjects(*toolObjects.toTypedArray())
            .build()
    }
}
