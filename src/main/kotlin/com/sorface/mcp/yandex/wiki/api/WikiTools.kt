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
    ): String = render(wikiReadService.getDescendants(slug))

    @Tool(
        name = "wiki_page_get_resources",
        description = "Возвращает ресурсы страницы Wiki: вложения и таблицы.",
    )
    fun pageGetResources(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
    ): String = render(wikiReadService.getResources(id))

    @Tool(
        name = "wiki_page_comments_list",
        description = "Возвращает список комментариев страницы Wiki.",
    )
    fun pageCommentsList(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
    ): String = render(wikiReadService.listComments(id))

    @Tool(
        name = "wiki_page_attachments_list",
        description = "Возвращает список вложений страницы Wiki.",
    )
    fun pageAttachmentsList(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
    ): String = render(wikiReadService.listAttachments(id))

    /**
     * Форматирует объект ответа в читаемый JSON-текст.
     */
    private fun render(node: JsonNode): String =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
}
