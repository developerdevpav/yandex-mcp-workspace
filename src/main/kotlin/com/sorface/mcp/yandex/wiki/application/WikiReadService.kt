package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.JsonNode

/**
 * Сервис чтения данных из Yandex Wiki.
 *
 * Объединяет операции получения страниц по адресу (`slug`) и идентификатору, дерева вложенных
 * страниц, ресурсов страницы, а также списков комментариев и вложений. Возвращает данные в виде
 * [JsonNode], сохраняя структуру ответа API.
 *
 * @author Sorface Developer
 */
interface WikiReadService {

    /**
     * Возвращает страницу по её адресу (`slug`).
     *
     * @param slug человекочитаемый адрес страницы, например `team/onboarding`
     * @param fields дополнительные поля через запятую (например, `content`)
     * @return объект страницы
     */
    fun getPageBySlug(slug: String, fields: String?): JsonNode

    /**
     * Возвращает страницу по её идентификатору.
     *
     * @param id числовой идентификатор страницы
     * @param fields дополнительные поля через запятую (например, `content`)
     * @return объект страницы
     */
    fun getPageById(id: String, fields: String?): JsonNode

    /**
     * Возвращает дерево вложенных страниц по адресу родительской страницы.
     *
     * @param slug адрес родительской страницы
     * @return дерево потомков
     */
    fun getDescendants(slug: String): JsonNode

    /**
     * Возвращает ресурсы страницы: вложения и таблицы.
     *
     * @param id идентификатор страницы
     * @return ресурсы страницы
     */
    fun getResources(id: String): JsonNode

    /**
     * Возвращает список комментариев страницы.
     *
     * @param id идентификатор страницы
     * @return комментарии страницы
     */
    fun listComments(id: String): JsonNode

    /**
     * Возвращает список вложений страницы.
     *
     * @param id идентификатор страницы
     * @return вложения страницы
     */
    fun listAttachments(id: String): JsonNode
}
