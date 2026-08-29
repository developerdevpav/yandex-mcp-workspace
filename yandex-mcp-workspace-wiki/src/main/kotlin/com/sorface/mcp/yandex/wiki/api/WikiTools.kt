package com.sorface.mcp.yandex.wiki.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.wiki.application.WikiReadService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

/**
 * Инструменты MCP для чтения данных Yandex Wiki.
 *
 * Тонкий слой: принимает параметры от агента, делегирует [WikiReadService] и форматирует
 * результат в JSON-текст.
 *
 * @author Sorface Developer
 */
@Component
class WikiTools(
    private val wikiReadService: WikiReadService,
    private val objectMapper: ObjectMapper,
) {

    @Tool(
        name = "wiki_page_get_by_slug",
        description = "Возвращает страницу Wiki по её адресу slug (например, team/onboarding).",
    )
    fun pageGetBySlug(
        @ToolParam(description = "Адрес страницы, например team/onboarding")
        slug: String,
        @ToolParam(required = false, description = "Доп. поля через запятую, например content")
        fields: String?,
    ): String = render(wikiReadService.getPageBySlug(slug, fields))

    @Tool(
        name = "wiki_page_get_by_id",
        description = "Возвращает страницу Wiki по её идентификатору.",
    )
    fun pageGetById(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
        @ToolParam(required = false, description = "Доп. поля через запятую, например content")
        fields: String?,
    ): String = render(wikiReadService.getPageById(id, fields))

    @Tool(
        name = "wiki_page_get_descendants",
        description = "Возвращает дерево вложенных страниц Wiki по адресу родительской страницы.",
    )
    fun pageGetDescendants(
        @ToolParam(description = "Адрес родительской страницы, например team")
        slug: String,
        @ToolParam(required = false, description = "Курсор next_cursor предыдущего ответа")
        cursor: String?,
        @ToolParam(required = false, description = "Размер страницы от 1 до 100")
        pageSize: Int?,
        @ToolParam(required = false, description = "Актуальность: actual или obsolete")
        actuality: String?,
        @ToolParam(required = false, description = "Включить родительскую страницу в результат")
        includeSelf: Boolean?,
        @ToolParam(required = false, description = "Показывать все доступные страницы")
        showAll: Boolean?,
    ): String = render(
        wikiReadService.getDescendantsBySlug(slug, cursor, pageSize, actuality, includeSelf, showAll),
    )

    @Tool(name = "wiki_page_get_descendants_by_id", description = "Возвращает подстраницы Wiki по ID родителя.")
    fun pageGetDescendantsById(
        @ToolParam(description = "Идентификатор родительской страницы") id: String,
        @ToolParam(required = false, description = "Курсор next_cursor предыдущего ответа") cursor: String?,
        @ToolParam(required = false, description = "Размер страницы от 1 до 100") pageSize: Int?,
        @ToolParam(required = false, description = "Актуальность: actual или obsolete") actuality: String?,
        @ToolParam(required = false, description = "Включить родительскую страницу") includeSelf: Boolean?,
        @ToolParam(required = false, description = "Показывать все доступные страницы") showAll: Boolean?,
    ): String = render(wikiReadService.getDescendantsById(id, cursor, pageSize, actuality, includeSelf, showAll))

    @Tool(
        name = "wiki_page_get_resources",
        description = "Возвращает ресурсы страницы Wiki: вложения и таблицы.",
    )
    fun pageGetResources(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
        @ToolParam(required = false, description = "Курсор next_cursor предыдущего ответа") cursor: String?,
        @ToolParam(required = false, description = "Тип ресурса: attachment или grid") type: String?,
    ): String = render(wikiReadService.getResources(id, cursor, type))

    @Tool(
        name = "wiki_page_comments_list",
        description = "Возвращает список комментариев страницы Wiki.",
    )
    fun pageCommentsList(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
        @ToolParam(required = false, description = "Курсор next_cursor предыдущего ответа") cursor: String?,
        @ToolParam(required = false, description = "Размер страницы от 1 до 100") pageSize: Int?,
        @ToolParam(required = false, description = "Поле сортировки: created_at") orderBy: String?,
        @ToolParam(required = false, description = "Направление: asc или desc") orderDirection: String?,
        @ToolParam(required = false, description = "Статус: resolved или unresolved") statusFilter: String?,
    ): String = render(wikiReadService.listComments(id, cursor, pageSize, orderBy, orderDirection, statusFilter))

    @Tool(name = "wiki_page_comment_thread", description = "Возвращает ветку ответов на комментарий Wiki.")
    fun pageCommentThread(
        @ToolParam(description = "Идентификатор страницы") id: String,
        @ToolParam(description = "Идентификатор корневого комментария") commentId: String,
        @ToolParam(required = false, description = "Курсор next_cursor предыдущего ответа") cursor: String?,
        @ToolParam(required = false, description = "Размер страницы от 1 до 100") pageSize: Int?,
    ): String = render(wikiReadService.getCommentThread(id, commentId, cursor, pageSize))

    @Tool(
        name = "wiki_page_attachments_list",
        description = "Возвращает список вложений страницы Wiki.",
    )
    fun pageAttachmentsList(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
        @ToolParam(required = false, description = "Курсор next_cursor предыдущего ответа") cursor: String?,
        @ToolParam(required = false, description = "Размер страницы от 1 до 100") pageSize: Int?,
        @ToolParam(required = false, description = "Сортировка: name, size или created_at") orderBy: String?,
        @ToolParam(required = false, description = "Направление: asc или desc") orderDirection: String?,
    ): String = render(wikiReadService.listAttachments(id, cursor, pageSize, orderBy, orderDirection))

    @Tool(name = "wiki_search", description = "Ищет страницы и файлы по тексту в Yandex Wiki.")
    fun search(
        @ToolParam(description = "Текст поискового запроса") query: String,
        @ToolParam(required = false, description = "JSON-фильтр: type, cluster, authors, created_at, modified_at") filters: String?,
        @ToolParam(required = false, description = "Курсор страницы от 1 до 500") cursor: Int?,
        @ToolParam(required = false, description = "Число результатов от 1 до 50") limit: Int?,
        @ToolParam(required = false, description = "Сортировка: relevancy, creation_date, modified_date") orderBy: String?,
        @ToolParam(required = false, description = "Подсвечивать совпадения") highlight: Boolean?,
    ): String = render(wikiReadService.search(query, filters, cursor, limit, orderBy, highlight))

    @Tool(name = "wiki_clone_operation_get", description = "Возвращает статус асинхронного клонирования страницы.")
    fun cloneOperationGet(
        @ToolParam(description = "Идентификатор operation.id из ответа wiki_page_clone") taskId: String,
    ): String = render(wikiReadService.getCloneOperationStatus(taskId))

    /**
     * Форматирует объект ответа в читаемый JSON-текст.
     */
    private fun render(node: JsonNode): String =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
}
