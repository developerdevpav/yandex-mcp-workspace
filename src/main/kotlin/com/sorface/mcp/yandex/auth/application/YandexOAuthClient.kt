package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.DeviceAuthorization
import com.sorface.mcp.yandex.auth.domain.TokenSet

/**
 * Результат опроса статуса авторизации по сценарию Device Flow.
 *
 * @author Sorface Developer
 */
sealed interface TokenPollResult {

    /**
     * Авторизация подтверждена, получен набор токенов.
     *
     * @property tokenSet полученный набор токенов
     */
    data class Success(val tokenSet: TokenSet) : TokenPollResult

    /**
     * Пользователь ещё не подтвердил доступ; нужно продолжать опрос.
     *
     * @property slowDown требуется ли увеличить интервал опроса (ответ `slow_down`)
     */
    data class Pending(val slowDown: Boolean) : TokenPollResult

    /**
     * Авторизация завершилась ошибкой и продолжать опрос бессмысленно.
     *
     * @property error код ошибки OAuth
     * @property description человекочитаемое описание ошибки
     */
    data class Failure(val error: String, val description: String?) : TokenPollResult
}

/**
 * Клиент сервиса Яндекс OAuth для сценария Device Flow.
 *
 * Инкапсулирует обращения к endpoint получения кодов устройства, обмена кода на токен
 * и обновления токена.
 *
 * @author Sorface Developer
 */
interface YandexOAuthClient {

    /**
     * Запрашивает коды устройства и пользователя для подтверждения доступа.
     *
     * @param scopes запрашиваемые разрешения через пробел; пустая строка означает разрешения по умолчанию
     * @return данные для подтверждения доступа пользователем
     */
    fun requestDeviceCode(scopes: String): DeviceAuthorization

    /**
     * Опрашивает статус авторизации по коду устройства.
     *
     * @param deviceCode код устройства, полученный в [requestDeviceCode]
     * @return текущий результат опроса
     */
    fun pollToken(deviceCode: String): TokenPollResult

    /**
     * Обновляет токен доступа по токену обновления.
     *
     * @param refreshToken действующий токен обновления
     * @return новый набор токенов
     */
    fun refresh(refreshToken: String): TokenSet
}
