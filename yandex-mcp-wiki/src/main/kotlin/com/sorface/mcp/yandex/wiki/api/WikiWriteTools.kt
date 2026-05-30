package com.sorface.mcp.yandex.wiki.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.wiki.application.WikiWriteService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

/**
 * Инструменты MCP для изменения данных Yandex Wiki: страницы, комментарии, вложения.
 *
 * Тонкий слой: принимает параметры от агента, делегирует [WikiWriteService] и форматирует
 * результат в JSON-текст. Содержимое страниц передаётся в формате Markdown. Все инструменты
 * являются изменяющими и блокируются в режиме только для чтения на уровне сервиса.
 *
 * @author Sorface Developer
 */
@Component
class WikiWriteTools(
    private val wikiWriteService: WikiWriteService,
    private val objectMapper: ObjectMapper,
) {

    @Tool(
        name = "wiki_page_create",
        description = "Создаёт страницу Wiki. Содержимое передаётся в формате Markdown. " +
            "Расположение задаётся через slug или parentId.",
    )
    fun pageCreate(
        @ToolParam(description = "Заголовок страницы")
        title: String,
        @ToolParam(required = false, description = "Адрес страницы, например team/onboarding")
        slug: String?,
        @ToolParam(required = false, description = "Идентификатор родительской страницы")
        parentId: String?,
        @ToolParam(required = false, description = "Содержимое страницы в формате Markdown")
        content: String?,
        @ToolParam(required = false, description = "JSON-объект дополнительных полей")
        fields: String?,
    ): String = render(wikiWriteService.createPage(title, slug, parentId, content, fields))

    @Tool(
        name = "wiki_page_update",
        description = "Изменяет заголовок и/или содержимое страницы Wiki. Содержимое в формате Markdown.",
    )
    fun pageUpdate(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
        @ToolParam(required = false, description = "Новый заголовок")
        title: String?,
        @ToolParam(required = false, description = "Новое содержимое в формате Markdown")
        content: String?,
        @ToolParam(required = false, description = "JSON-объект дополнительных изменяемых полей")
        fields: String?,
    ): String = render(wikiWriteService.updatePage(id, title, content, fields))

    @Tool(
        name = "wiki_page_delete",
        description = "Удаляет страницу Wiki. В ответе возвращается токен восстановления — сохраните его.",
    )
    fun pageDelete(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
    ): String = render(wikiWriteService.deletePage(id))

    @Tool(
        name = "wiki_page_recover",
        description = "Восстанавливает удалённую страницу Wiki по токену восстановления.",
    )
    fun pageRecover(
        @ToolParam(description = "Токен восстановления, полученный при удалении")
        recoveryToken: String,
    ): String = render(wikiWriteService.recoverPage(recoveryToken))

    @Tool(
        name = "wiki_page_clone",
        description = "Клонирует страницу Wiki в целевое расположение (slug или parentId).",
    )
    fun pageClone(
        @ToolParam(description = "Идентификатор исходной страницы")
        id: String,
        @ToolParam(required = false, description = "Адрес целевого расположения")
        slug: String?,
        @ToolParam(required = false, description = "Идентификатор целевой родительской страницы")
        parentId: String?,
        @ToolParam(required = false, description = "Заголовок клона")
        title: String?,
        @ToolParam(required = false, description = "JSON-объект дополнительных полей")
        fields: String?,
    ): String = render(wikiWriteService.clonePage(id, slug, parentId, title, fields))

    @Tool(
        name = "wiki_page_append_content",
        description = "Дописывает содержимое (Markdown) к странице Wiki: в конец, в начало или к якорю.",
    )
    fun pageAppendContent(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
        @ToolParam(description = "Добавляемое содержимое в формате Markdown")
        content: String,
        @ToolParam(required = false, description = "Место вставки: bottom или top")
        location: String?,
        @ToolParam(required = false, description = "Якорь, к которому выполняется вставка")
        anchor: String?,
    ): String = render(wikiWriteService.appendContent(id, content, location, anchor))

    @Tool(
        name = "wiki_page_comment_add",
        description = "Добавляет комментарий к странице Wiki или ответ на существующий комментарий.",
    )
    fun pageCommentAdd(
        @ToolParam(description = "Идентификатор страницы")
        id: String,
        @ToolParam(description = "Текст комментария")
        content: String,
        @ToolParam(required = false, description = "Идентификатор родительского комментария для ответа")
        parentId: String?,
    ): String = render(wikiWriteService.addComment(id, content, parentId))

    @Tool(
        name = "wiki_page_attachment_upload",
        description = "Загружает локальный файл (по пути, доступному серверу) и прикрепляет его к странице Wiki.",
    )
    fun pageAttachmentUpload(
        @ToolParam(description = "Идентификатор страницы")
        pageId: String,
        @ToolParam(description = "Путь к файлу, доступному серверу (например, в подключённом томе)")
        filePath: String,
        @ToolParam(required = false, description = "Имя вложения; по умолчанию имя файла")
        name: String?,
    ): String = render(wikiWriteService.uploadAttachment(pageId, filePath, name))

    @Tool(
        name = "wiki_page_attachment_attach",
        description = "Прикрепляет к странице Wiki уже завершённые сессии загрузки по их идентификаторам.",
    )
    fun pageAttachmentAttach(
        @ToolParam(description = "Идентификатор страницы")
        pageId: String,
        @ToolParam(description = "Идентификаторы сессий загрузки через запятую")
        sessionIds: String,
    ): String = render(wikiWriteService.attachUploadSessions(pageId, sessionIds))

    /**
     * Форматирует объект ответа в читаемый JSON-текст.
     */
    private fun render(node: JsonNode): String =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
}
