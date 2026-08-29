package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.TokenSet

/**
 * Текущее состояние подтверждения OAuth 2.0 Device Flow.
 *
 * @author Sorface Developer
 */
sealed interface DeviceAuthorizationProgress {

    /**
     * Пользователь ещё не подтвердил доступ.
     *
     * @property slowDown требуется ли увеличить интервал следующего запроса
     */
    data class Pending(val slowDown: Boolean) : DeviceAuthorizationProgress

    /**
     * Авторизация завершена, токены получены и сохранены.
     *
     * @property tokenSet сохранённый набор токенов
     */
    data class Authorized(val tokenSet: TokenSet) : DeviceAuthorizationProgress

    /**
     * Авторизация завершилась ошибкой и продолжать опрос нельзя.
     *
     * @property error код ошибки OAuth
     * @property description человекочитаемое описание ошибки
     */
    data class Failed(val error: String, val description: String?) : DeviceAuthorizationProgress
}
