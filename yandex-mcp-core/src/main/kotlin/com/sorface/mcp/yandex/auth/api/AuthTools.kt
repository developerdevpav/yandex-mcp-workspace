package com.sorface.mcp.yandex.auth.api

import com.sorface.mcp.yandex.auth.application.AuthService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component

/**
 * Инструменты MCP для проверки состояния авторизации.
 *
 * Получение токена выполняется отдельной командой `auth` при запуске контейнера, поэтому
 * среди инструментов нет действий, изменяющих авторизацию. Инструмент статуса помогает
 * агенту понять, готов ли сервер к запросам к API.
 *
 * @author Sorface Developer
 */
@Component
class AuthTools(
    private val authService: AuthService,
) {

    /**
     * Возвращает сводку о состоянии авторизации без раскрытия секретов.
     *
     * @return человекочитаемая сводка: заданы ли настройки, есть ли токен и когда он истекает
     */
    @Tool(
        name = "yandex_auth_status",
        description = "Показывает состояние авторизации Яндекса: заданы ли настройки, " +
            "есть ли действующий токен и когда он истекает.",
    )
    fun authStatus(): String {
        val status = authService.status()
        return buildString {
            appendLine("настройки заданы: ${if (status.configured) "да" else "нет"}")
            appendLine("авторизован: ${if (status.authorized) "да" else "нет"}")
            appendLine("токен истекает: ${status.expiresAt?.toString() ?: "—"}")
            appendLine("заголовок организации: ${status.orgHeader}")
            append("режим только для чтения: ${if (status.readOnly) "да" else "нет"}")
        }
    }
}
