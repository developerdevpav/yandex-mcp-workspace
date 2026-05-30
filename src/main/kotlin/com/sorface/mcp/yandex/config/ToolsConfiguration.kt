package com.sorface.mcp.yandex.config

import com.sorface.mcp.yandex.auth.api.AuthTools
import com.sorface.mcp.yandex.system.api.SystemTools
import com.sorface.mcp.yandex.tracker.api.TrackerTools
import com.sorface.mcp.yandex.tracker.api.TrackerWriteTools
import com.sorface.mcp.yandex.wiki.api.WikiGridTools
import com.sorface.mcp.yandex.wiki.api.WikiTools
import com.sorface.mcp.yandex.wiki.api.WikiWriteTools
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Регистрация инструментов MCP-сервера.
 *
 * Spring AI публикует в MCP все методы, помеченные аннотацией [org.springframework.ai.tool.annotation.Tool],
 * у объектов, переданных в [MethodToolCallbackProvider]. По мере добавления групп инструментов
 * (Tracker, Wiki) их компоненты добавляются в этот провайдер.
 *
 * @author Sorface Developer
 */
@Configuration
class ToolsConfiguration {

    /**
     * Собирает все наборы инструментов сервера в единый провайдер для MCP.
     *
     * В режиме только для чтения (`yandex.read-only=true`) изменяющие инструменты Tracker и Wiki
     * не регистрируются вовсе: агент не видит их в списке `tools/list` и не может вызвать. Это
     * дополняет защиту [com.sorface.mcp.yandex.common.WriteGuard] на уровне сервиса, делая
     * безопасный режим явным для клиента. Инструменты таблиц Wiki содержат и чтение, и запись,
     * поэтому регистрируются всегда, а изменяющие методы внутри них защищены `WriteGuard`.
     *
     * @param properties настройки сервера, определяющие режим только для чтения
     * @param systemTools служебные инструменты (проверка доступности, сведения о сервере)
     * @param authTools инструменты состояния авторизации
     * @param trackerTools инструменты чтения данных Tracker
     * @param trackerWriteTools инструменты изменения данных Tracker
     * @param wikiTools инструменты чтения данных Wiki
     * @param wikiWriteTools инструменты изменения данных Wiki
     * @param wikiGridTools инструменты работы с динамическими таблицами Wiki
     * @return провайдер обратных вызовов инструментов для авто-конфигурации MCP-сервера
     */
    @Bean
    fun yandexToolCallbackProvider(
        properties: YandexProperties,
        systemTools: SystemTools,
        authTools: AuthTools,
        trackerTools: TrackerTools,
        trackerWriteTools: TrackerWriteTools,
        wikiTools: WikiTools,
        wikiWriteTools: WikiWriteTools,
        wikiGridTools: WikiGridTools,
    ): ToolCallbackProvider {
        val toolObjects = buildList {
            add(systemTools)
            add(authTools)
            add(trackerTools)
            add(wikiTools)
            add(wikiGridTools)
            if (!properties.readOnly) {
                add(trackerWriteTools)
                add(wikiWriteTools)
            }
        }
        return MethodToolCallbackProvider.builder()
            .toolObjects(*toolObjects.toTypedArray())
            .build()
    }
}
