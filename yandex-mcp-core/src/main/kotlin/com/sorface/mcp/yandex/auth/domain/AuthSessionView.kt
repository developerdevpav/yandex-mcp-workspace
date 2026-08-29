package com.sorface.mcp.yandex.auth.domain

import java.time.Instant

/**
 * Публичное состояние интерактивной сессии авторизации.
 *
 * В объект намеренно не включаются `device_code`, access- и refresh-токены. Его безопасно
 * возвращать из MCP-инструментов и показывать пользователю.
 *
 * @property sessionId непрозрачный идентификатор сессии для последующего опроса
 * @property state текущее состояние сессии
 * @property verificationUrl адрес страницы подтверждения
 * @property userCode код, который пользователь вводит на странице Yandex OAuth
 * @property expiresAt момент истечения кода подтверждения
 * @property nextPollAt момент, раньше которого повторный опрос Yandex OAuth выполняться не будет
 * @property message безопасное пояснение текущего состояния
 *
 * @author Sorface Developer
 */
data class AuthSessionView(
    val sessionId: String,
    val state: AuthSessionState,
    val verificationUrl: String?,
    val userCode: String?,
    val expiresAt: Instant?,
    val nextPollAt: Instant?,
    val message: String,
)

/**
 * Состояние интерактивной сессии авторизации.
 *
 * @author Sorface Developer
 */
enum class AuthSessionState {
    /** Ожидается подтверждение пользователя. */
    PENDING,

    /** Токены получены и сохранены. */
    AUTHORIZED,

    /** Время действия кода подтверждения истекло. */
    EXPIRED,

    /** Yandex OAuth отклонил авторизацию. */
    FAILED,
}
