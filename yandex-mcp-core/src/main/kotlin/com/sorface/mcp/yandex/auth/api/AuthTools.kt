package com.sorface.mcp.yandex.auth.api

import com.sorface.mcp.yandex.auth.application.AuthService
import com.sorface.mcp.yandex.auth.application.AuthSessionService
import com.sorface.mcp.yandex.auth.application.BrowserLauncher
import com.sorface.mcp.yandex.auth.domain.AuthSessionView
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

/**
 * Инструменты MCP для управления авторизацией без доступа к терминалу и логам сервера.
 *
 * @author Sorface Developer
 */
@Component
class AuthTools(
    private val authService: AuthService,
    private val authSessionService: AuthSessionService,
    private val browserLauncher: BrowserLauncher,
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

    /**
     * Запускает Device Flow и возвращает ссылку с пользовательским кодом.
     *
     * @return безопасное состояние сессии без внутренних кодов и токенов
     */
    @Tool(
        name = "yandex_auth_start",
        description = "Начинает авторизацию в Yandex OAuth. Возвращает ссылку, пользовательский код, " +
            "идентификатор сессии и срок действия. После подтверждения вызывайте yandex_auth_poll.",
    )
    fun startAuthorization(): AuthStartResult {
        val session = authSessionService.start()
        val browserOpened = session.verificationUrl?.let(browserLauncher::open) ?: false
        return AuthStartResult(session = session, browserOpened = browserOpened)
    }

    /**
     * Проверяет состояние ранее начатой авторизации.
     *
     * @param sessionId непрозрачный идентификатор из [startAuthorization]
     * @return актуальное состояние сессии
     */
    @Tool(
        name = "yandex_auth_poll",
        description = "Проверяет завершение авторизации. Не вызывайте раньше nextPollAt из ответа " +
            "yandex_auth_start или предыдущего ответа yandex_auth_poll.",
    )
    fun pollAuthorization(
        @ToolParam(description = "Идентификатор сессии из yandex_auth_start") sessionId: String,
    ): AuthSessionView = authSessionService.poll(sessionId)

    /**
     * Удаляет локальные токены текущего профиля.
     *
     * @return подтверждение удаления без раскрытия секретов
     */
    @Tool(
        name = "yandex_auth_logout",
        description = "Удаляет локальные OAuth-токены. Следующий запрос к API потребует новой авторизации.",
    )
    fun logout(): String {
        authService.logout()
        return "Локальные токены удалены. Для повторного подключения вызовите yandex_auth_start."
    }
}

/**
 * Результат запуска авторизации с признаком автоматического открытия браузера.
 *
 * @property session безопасные данные Device Flow
 * @property browserOpened удалось ли запустить системный браузер
 *
 * @author Sorface Developer
 */
data class AuthStartResult(
    val session: AuthSessionView,
    val browserOpened: Boolean,
)
