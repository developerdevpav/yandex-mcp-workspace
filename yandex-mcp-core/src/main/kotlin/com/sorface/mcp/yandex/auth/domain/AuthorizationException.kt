package com.sorface.mcp.yandex.auth.domain

/**
 * Ошибка авторизации сервера в Яндексе.
 *
 * Бросается, когда невозможно получить или обновить токен доступа: отсутствуют настройки,
 * пользователь не подтвердил доступ, сервис OAuth вернул ошибку или истёк срок действия кода.
 *
 * @author Sorface Developer
 */
class AuthorizationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
