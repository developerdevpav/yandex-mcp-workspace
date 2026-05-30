package com.sorface.mcp.yandex.system.api

import com.sorface.mcp.yandex.config.YandexProperties
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component

/**
 * Служебные инструменты MCP-сервера, не зависящие от внешних API.
 *
 * Используются для проверки того, что MCP-клиент корректно подключился к серверу, и для
 * получения сведений о текущем режиме работы (в частности, режим только для чтения).
 *
 * @author Sorface Developer
 */
@Component
class SystemTools(
    private val properties: YandexProperties,
) {

    /**
     * Проверка доступности MCP-сервера.
     *
     * @return строка `pong` как признак работоспособности сервера
     */
    @Tool(
        name = "system_ping",
        description = "Проверка доступности MCP-сервера. Возвращает pong, если сервер работает.",
    )
    fun ping(): String = "pong"

    /**
     * Сведения о текущем режиме работы сервера.
     *
     * Помогает агенту заранее понять, доступны ли изменяющие операции: в режиме только для чтения
     * изменяющие инструменты не зарегистрированы, и любые попытки записи будут отклонены.
     *
     * @return человекочитаемое описание режима работы
     */
    @Tool(
        name = "system_server_info",
        description = "Возвращает режим работы сервера: только чтение (read-only) или чтение и запись. " +
            "В режиме только для чтения изменяющие инструменты недоступны.",
    )
    fun serverInfo(): String =
        if (properties.readOnly) {
            "Режим: только чтение (read-only). Изменяющие инструменты Tracker и Wiki отключены."
        } else {
            "Режим: чтение и запись. Доступны все инструменты Tracker и Wiki."
        }
}
