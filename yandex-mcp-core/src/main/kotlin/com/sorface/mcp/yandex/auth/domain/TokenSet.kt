package com.sorface.mcp.yandex.auth.domain

import java.time.Instant

/**
 * Набор токенов авторизации, сохраняемый между запусками сервера.
 *
 * @property accessToken токен доступа для обращения к API Tracker и Wiki
 * @property refreshToken токен обновления; может отсутствовать, если сервис его не выдал
 * @property tokenType тип токена (обычно `bearer`/`OAuth`)
 * @property expiresAt момент истечения срока действия токена доступа
 *
 * @author Sorface Developer
 */
data class TokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val tokenType: String,
    val expiresAt: Instant,
) {
    /**
     * Проверяет, истекает ли токен доступа в пределах указанного запаса времени.
     *
     * @param now текущий момент времени
     * @param skewSeconds запас в секундах, при котором токен считается требующим обновления
     */
    fun isExpiring(now: Instant, skewSeconds: Long): Boolean =
        !expiresAt.isAfter(now.plusSeconds(skewSeconds))
}
