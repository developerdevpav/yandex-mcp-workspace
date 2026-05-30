package com.sorface.mcp.yandex.auth.domain

import java.time.Instant

/**
 * Сводка о состоянии авторизации сервера.
 *
 * @property configured заданы ли обязательные настройки (`client_id`, `client_secret`, идентификатор организации)
 * @property authorized есть ли сохранённый токен доступа
 * @property expiresAt момент истечения токена доступа, если он есть
 * @property orgHeader имя используемого заголовка идентификатора организации
 * @property readOnly включён ли режим только для чтения
 *
 * @author Sorface Developer
 */
data class AuthStatus(
    val configured: Boolean,
    val authorized: Boolean,
    val expiresAt: Instant?,
    val orgHeader: String,
    val readOnly: Boolean,
)
