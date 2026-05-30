package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.AuthStatus
import com.sorface.mcp.yandex.auth.domain.DeviceAuthorization
import com.sorface.mcp.yandex.auth.domain.TokenSet

/**
 * Сервис авторизации сервера в Яндексе по сценарию OAuth 2.0 Device Flow.
 *
 * Отвечает за запуск авторизации, ожидание подтверждения пользователем, сохранение токенов
 * и выдачу действующего токена доступа для запросов к API. При истечении токена доступа
 * сервис автоматически обновляет его по токену обновления.
 *
 * @author Sorface Developer
 */
interface AuthService {

    /**
     * Запускает авторизацию: запрашивает коды устройства и пользователя.
     *
     * @return данные для подтверждения доступа пользователем
     */
    fun beginDeviceAuthorization(): DeviceAuthorization

    /**
     * Ожидает подтверждения доступа пользователем, опрашивая статус авторизации.
     *
     * Метод блокирующий: опрашивает сервис OAuth с интервалом до истечения срока кодов.
     * При успехе сохраняет полученные токены в хранилище.
     *
     * @param authorization данные авторизации, полученные в [beginDeviceAuthorization]
     * @return сохранённый набор токенов
     */
    fun completeDeviceAuthorization(authorization: DeviceAuthorization): TokenSet

    /**
     * Возвращает действующий токен доступа, при необходимости обновляя его.
     *
     * @return токен доступа для заголовка `Authorization`
     */
    fun currentAccessToken(): String

    /**
     * Возвращает сводку о состоянии авторизации без раскрытия секретов.
     *
     * @return сводка состояния
     */
    fun status(): AuthStatus
}
