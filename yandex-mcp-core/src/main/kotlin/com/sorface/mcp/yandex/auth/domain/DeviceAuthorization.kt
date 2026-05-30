package com.sorface.mcp.yandex.auth.domain

/**
 * Данные сценария OAuth 2.0 Device Flow, которые пользователь использует для подтверждения доступа.
 *
 * @property deviceCode код устройства; используется сервером для опроса статуса авторизации
 * @property userCode код пользователя, который нужно ввести на странице подтверждения
 * @property verificationUrl адрес страницы подтверждения Яндекс OAuth
 * @property intervalSeconds минимальный интервал опроса статуса в секундах
 * @property expiresInSeconds срок жизни кодов в секундах
 *
 * @author Sorface Developer
 */
data class DeviceAuthorization(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val intervalSeconds: Int,
    val expiresInSeconds: Int,
)
