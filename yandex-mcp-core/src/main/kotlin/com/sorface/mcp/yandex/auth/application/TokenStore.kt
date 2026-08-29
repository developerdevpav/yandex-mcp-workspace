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

    /**
     * Выполняет согласованное чтение и условную замену токена под эксклюзивной блокировкой.
     *
     * Реализация должна удерживать межпроцессную блокировку в течение всего [transform], чтобы
     * два MCP-процесса не обновляли один refresh token одновременно. Если результат равен
     * исходному значению, повторная запись не требуется.
     *
     * @param transform преобразование текущего значения; может выполнять сетевой refresh
     * @return итоговое значение токена
     */
    fun update(transform: (TokenSet?) -> TokenSet?): TokenSet? {
        val current = load()
        val updated = transform(current)
        if (updated != current) {
            if (updated == null) clear() else save(updated)
        }
        return updated
    }
}
