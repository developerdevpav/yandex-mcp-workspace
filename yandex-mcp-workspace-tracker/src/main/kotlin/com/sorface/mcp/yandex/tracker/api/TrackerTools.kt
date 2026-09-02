package com.sorface.mcp.yandex.tracker.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.tracker.application.TrackerReadService
import com.sorface.mcp.yandex.tracker.domain.PagedResult
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

/**
 * Инструменты MCP для чтения данных Yandex Tracker.
 *
 * Тонкий слой: принимает параметры от агента, делегирует [TrackerReadService] и форматирует
 * результат в JSON-текст. Результаты постраничных запросов оборачиваются в объект с полями
 * пагинации, чтобы агент видел общее число объектов и мог перебирать страницы.
 *
 * @author Sorface Developer
 */
@Component
class TrackerTools(
    private val trackerReadService: TrackerReadService,
    private val objectMapper: ObjectMapper,
) {

    @Tool(
        name = "tracker_myself",
        description = "Возвращает данные текущего пользователя Tracker, которому принадлежит токен.",
    )
    fun myself(): String = render(trackerReadService.myself())

    @Tool(
        name = "tracker_issue_get",
        description = "Возвращает задачу Tracker по её ключу (например, TREK-42).",
    )
    fun issueGet(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(required = false, description = "Доп. блоки через запятую: transitions, attachments")
        expand: String?,
    ): String = render(trackerReadService.getIssue(key, expand))

    @Tool(
        name = "tracker_issue_search",
        description = "Ищет задачи Tracker. Укажите ровно один критерий: query, filter, queue или keys. " +
            "Для queue используйте id из nextPageId; для больших выдач доступны scroll-параметры.",
    )
    fun issueSearch(
        @ToolParam(required = false, description = "Строка на языке запросов Tracker")
        query: String?,
        @ToolParam(required = false, description = "JSON-объект фильтра, например {\"queue\":\"TREK\",\"assignee\":\"me\"}")
        filter: String?,
        @ToolParam(required = false, description = "Ключ очереди для ограничения поиска, например TREK")
        queue: String?,
        @ToolParam(required = false, description = "Ключи задач через запятую для точечной выборки")
        keys: String?,
        @ToolParam(required = false, description = "Сортировка, например +status или -updated")
        order: String?,
        @ToolParam(required = false, description = "Доп. блоки данных через запятую")
        expand: String?,
        @ToolParam(required = false, description = "Поля задачи в ответе через запятую")
        fields: String?,
        @ToolParam(required = false, description = "Число задач на страницу (по умолчанию 50)")
        perPage: Int?,
        @ToolParam(required = false, description = "Номер страницы, начиная с 1")
        page: Int?,
        @ToolParam(required = false, description = "Курсор id из nextPageId для поиска по queue")
        id: String?,
        @ToolParam(required = false, description = "Тип прокрутки больших результатов: sorted")
        scrollType: String?,
        @ToolParam(required = false, description = "Число задач в порции прокрутки")
        perScroll: Int?,
        @ToolParam(required = false, description = "Время жизни прокрутки в миллисекундах")
        scrollTTLMillis: Int?,
        @ToolParam(required = false, description = "X-Scroll-Id из предыдущего ответа")
        scrollId: String?,
    ): String {
        val result = trackerReadService.searchIssues(
            query, filter, queue, keys, order, expand, fields, perPage, page,
            id, scrollType, perScroll, scrollTTLMillis, scrollId,
        )
        return renderPaged(result, page, perPage)
    }

    @Tool(
        name = "tracker_issue_count",
        description = "Считает задачи Tracker. Укажите ровно один критерий: query, filter, queue или keys.",
    )
    fun issueCount(
        @ToolParam(required = false, description = "Строка на языке запросов Tracker")
        query: String?,
        @ToolParam(required = false, description = "JSON-объект фильтра, например {\"queue\":\"TREK\"}")
        filter: String?,
        @ToolParam(required = false, description = "Ключ очереди для ограничения подсчёта")
        queue: String?,
        @ToolParam(required = false, description = "Ключи задач через запятую")
        keys: String?,
    ): String = trackerReadService.countIssues(query, filter, queue, keys).toString()

    @Tool(
        name = "tracker_queue_list",
        description = "Возвращает список очередей Tracker с постраничной навигацией.",
    )
    fun queueList(
        @ToolParam(required = false, description = "Доп. блоки данных через запятую: all, projects")
        expand: String?,
        @ToolParam(required = false, description = "Число очередей на страницу (по умолчанию 50)")
        perPage: Int?,
        @ToolParam(required = false, description = "Номер страницы, начиная с 1")
        page: Int?,
    ): String = renderPaged(trackerReadService.listQueues(expand, perPage, page), page, perPage)

    @Tool(
        name = "tracker_queue_get",
        description = "Возвращает параметры очереди Tracker по идентификатору или ключу (например, TREK).",
    )
    fun queueGet(
        @ToolParam(description = "Идентификатор или ключ очереди, например TREK")
        id: String,
        @ToolParam(required = false, description = "Доп. блоки данных через запятую")
        expand: String?,
    ): String = render(trackerReadService.getQueue(id, expand))

    @Tool(
        name = "tracker_issuetype_list",
        description = "Возвращает список типов задач Tracker.",
    )
    fun issueTypeList(): String = render(trackerReadService.listIssueTypes())

    @Tool(
        name = "tracker_priority_list",
        description = "Возвращает список приоритетов Tracker.",
    )
    fun priorityList(): String = render(trackerReadService.listPriorities())

    @Tool(
        name = "tracker_status_list",
        description = "Возвращает список статусов задач Tracker.",
    )
    fun statusList(): String = render(trackerReadService.listStatuses())

    @Tool(
        name = "tracker_resolution_list",
        description = "Возвращает список резолюций задач Tracker.",
    )
    fun resolutionList(): String = render(trackerReadService.listResolutions())

    @Tool(
        name = "tracker_issue_transitions_list",
        description = "Возвращает доступные переходы по статусам для задачи Tracker.",
    )
    fun issueTransitionsList(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
    ): String = render(trackerReadService.listTransitions(key))

    @Tool(
        name = "tracker_issue_changelog",
        description = "Возвращает историю изменений задачи Tracker с постраничной навигацией.",
    )
    fun issueChangelog(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(required = false, description = "Идентификатор поля для фильтрации записей истории")
        field: String?,
        @ToolParam(required = false, description = "Тип изменения, например IssueUpdated или IssueWorkflow")
        type: String?,
        @ToolParam(required = false, description = "Число записей на страницу")
        perPage: Int?,
        @ToolParam(required = false, description = "Курсор id из nextPageId предыдущего ответа")
        id: String?,
    ): String = renderPaged(
        trackerReadService.getChangelog(key, field, type, perPage, id),
        page = null,
        perPage = perPage,
    )

    @Tool(
        name = "tracker_comment_list",
        description = "Возвращает страницу комментариев задачи Tracker и курсор nextPageId.",
    )
    fun commentList(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(required = false, description = "Доп. блоки данных через запятую: attachments, html")
        expand: String?,
        @ToolParam(required = false, description = "Число комментариев на страницу")
        perPage: Int?,
        @ToolParam(required = false, description = "Курсор id из nextPageId предыдущего ответа")
        id: String?,
    ): String = renderPaged(trackerReadService.listComments(key, expand, perPage, id), page = null, perPage = perPage)

    @Tool(
        name = "tracker_external_application_list",
        description = "Возвращает зарегистрированные внешние приложения Tracker. Используйте точный id " +
            "нужного приложения как origin для tracker_external_link_create; не угадывайте origin.",
    )
    fun externalApplicationList(): String = render(trackerReadService.listExternalApplications())

    @Tool(
        name = "tracker_link_list",
        description = "Возвращает список связей задачи Tracker.",
    )
    fun linkList(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
    ): String = render(trackerReadService.listLinks(key))

    @Tool(
        name = "tracker_external_link_list",
        description = "Возвращает связи задачи с объектами внешних приложений. Проверяйте этот список " +
            "перед tracker_external_link_create, чтобы не создавать дубликаты.",
    )
    fun externalLinkList(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
    ): String = render(trackerReadService.listExternalLinks(key))

    @Tool(
        name = "tracker_checklist_list",
        description = "Возвращает пункты чек-листа задачи Tracker.",
    )
    fun checklistList(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
    ): String = render(trackerReadService.listChecklistItems(key))

    @Tool(
        name = "tracker_worklog_list",
        description = "Возвращает записи учёта времени (worklog) задачи Tracker.",
    )
    fun worklogList(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
    ): String = render(trackerReadService.listWorklogs(key))

    @Tool(
        name = "tracker_user_list",
        description = "Возвращает список пользователей организации Tracker с постраничной навигацией.",
    )
    fun userList(
        @ToolParam(required = false, description = "Число пользователей на страницу")
        perPage: Int?,
        @ToolParam(required = false, description = "Номер страницы, начиная с 1")
        page: Int?,
    ): String = renderPaged(trackerReadService.listUsers(perPage, page), page, perPage)

    @Tool(
        name = "tracker_user_get",
        description = "Возвращает пользователя Tracker по логину или идентификатору.",
    )
    fun userGet(
        @ToolParam(description = "Логин или идентификатор пользователя")
        id: String,
    ): String = render(trackerReadService.getUser(id))

    @Tool(
        name = "tracker_field_list",
        description = "Возвращает глобальные поля организации Tracker (ключи, типы, схемы).",
    )
    fun fieldList(): String = render(trackerReadService.listFields())

    @Tool(
        name = "tracker_field_get",
        description = "Возвращает параметры поля Tracker по идентификатору или ключу.",
    )
    fun fieldGet(
        @ToolParam(description = "Идентификатор или ключ поля, например summary или customField")
        id: String,
    ): String = render(trackerReadService.getField(id))

    @Tool(
        name = "tracker_queue_field_list",
        description = "Возвращает поля очереди Tracker (обязательные и доступные для задач).",
    )
    fun queueFieldList(
        @ToolParam(description = "Идентификатор или ключ очереди, например TREK")
        queueId: String,
    ): String = render(trackerReadService.listQueueFields(queueId))

    /**
     * Форматирует объект ответа в читаемый JSON-текст.
     */
    private fun render(node: JsonNode): String =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)

    /**
     * Оборачивает постраничный результат в объект с полями пагинации и форматирует в JSON-текст.
     */
    private fun renderPaged(result: PagedResult, page: Int?, perPage: Int?): String {
        val wrapper = objectMapper.createObjectNode()
        result.totalCount?.let { wrapper.put("totalCount", it) }
        result.totalPages?.let { wrapper.put("totalPages", it) }
        result.nextPageId?.let { wrapper.put("nextPageId", it) }
        result.nextPageUrl?.let { wrapper.put("nextPageUrl", it) }
        result.scrollId?.let { wrapper.put("scrollId", it) }
        page?.let { wrapper.put("page", it) }
        perPage?.let { wrapper.put("perPage", it) }
        wrapper.set<JsonNode>("items", result.items)
        return render(wrapper)
    }
}
