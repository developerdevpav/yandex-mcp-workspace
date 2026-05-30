package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.TokenSet

/**
 * Хранилище токенов авторизации.
 *
 * Реализация должна сохранять токены между запусками сервера и обеспечивать потокобезопасный доступ.
 *
 * @author Sorface Developer
 */
interface TokenStore {

    /**
     * Загружает сохранённый набор токенов.
     *
     * @return набор токенов или `null`, если токены ещё не сохранены
     */
    fun load(): TokenSet?

    /**
     * Сохраняет набор токенов, заменяя предыдущее значение.
     *
     * @param tokenSet набор токенов для сохранения
     */
    fun save(tokenSet: TokenSet)

    /**
     * Удаляет сохранённые токены.
     */
    fun clear()
}
