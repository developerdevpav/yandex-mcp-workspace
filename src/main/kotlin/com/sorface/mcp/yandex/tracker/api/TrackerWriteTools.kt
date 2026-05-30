package com.sorface.mcp.yandex.tracker.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.tracker.application.TrackerWriteService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

/**
 * Инструменты MCP для изменения данных Yandex Tracker: задачи, комментарии, связи.
 *
 * Тонкий слой: принимает параметры от агента, делегирует [TrackerWriteService] и форматирует
 * результат в JSON-текст. Все инструменты являются изменяющими и блокируются в режиме только
 * для чтения на уровне сервиса.
 *
 * @author Sorface Developer
 */
@Component
class TrackerWriteTools(
    private val trackerWriteService: TrackerWriteService,
    private val objectMapper: ObjectMapper,
) {

    @Tool(
        name = "tracker_issue_create",
        description = "Создаёт задачу в Tracker. Произвольные и пользовательские поля передаются " +
            "JSON-объектом в параметре fields.",
    )
    fun issueCreate(
        @ToolParam(description = "Ключ очереди, например TREK")
        queue: String,
        @ToolParam(description = "Название задачи")
        summary: String,
        @ToolParam(required = false, description = "Описание задачи")
        description: String?,
        @ToolParam(required = false, description = "Тип задачи (ключ или идентификатор), например bug")
        type: String?,
        @ToolParam(required = false, description = "Приоритет (ключ или идентификатор), например normal")
        priority: String?,
        @ToolParam(required = false, description = "Логин или идентификатор исполнителя")
        assignee: String?,
        @ToolParam(required = false, description = "Ключ родительской задачи")
        parent: String?,
        @ToolParam(required = false, description = "JSON-объект дополнительных полей, например {\"tags\":[\"backend\"]}")
        fields: String?,
    ): String = render(
        trackerWriteService.createIssue(queue, summary, description, type, priority, assignee, parent, fields),
    )

    @Tool(
        name = "tracker_issue_update",
        description = "Изменяет поля задачи Tracker. Для защиты от конфликтов можно передать version.",
    )
    fun issueUpdate(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(required = false, description = "Новое название задачи")
        summary: String?,
        @ToolParam(required = false, description = "Новое описание задачи")
        description: String?,
        @ToolParam(required = false, description = "Новый приоритет")
        priority: String?,
        @ToolParam(required = false, description = "Новый исполнитель")
        assignee: String?,
        @ToolParam(required = false, description = "JSON-объект дополнительных изменяемых полей")
        fields: String?,
        @ToolParam(required = false, description = "Номер версии задачи для защиты от конфликтов")
        version: Int?,
    ): String = render(
        trackerWriteService.updateIssue(key, summary, description, priority, assignee, fields, version),
    )

    @Tool(
        name = "tracker_issue_move",
        description = "Переносит задачу Tracker в другую очередь.",
    )
    fun issueMove(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(description = "Ключ очереди назначения, например NEWQ")
        targetQueue: String,
        @ToolParam(required = false, description = "JSON-объект полей при переносе, например новый тип")
        fields: String?,
    ): String = render(trackerWriteService.moveIssue(key, targetQueue, fields))

    @Tool(
        name = "tracker_issue_transition_execute",
        description = "Выполняет переход задачи Tracker по статусу. Идентификатор перехода берётся " +
            "из tracker_issue_transitions_list.",
    )
    fun issueTransitionExecute(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(description = "Идентификатор перехода, например close или start_progress")
        transitionId: String,
        @ToolParam(required = false, description = "Комментарий, добавляемый при переходе")
        comment: String?,
        @ToolParam(required = false, description = "JSON-объект полей при переходе, например {\"resolution\":\"fixed\"}")
        fields: String?,
    ): String = render(trackerWriteService.executeTransition(key, transitionId, comment, fields))

    @Tool(
        name = "tracker_comment_add",
        description = "Добавляет комментарий к задаче Tracker.",
    )
    fun commentAdd(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(description = "Текст комментария")
        text: String,
        @ToolParam(required = false, description = "Логины для призыва через запятую")
        summonees: String?,
    ): String = render(trackerWriteService.addComment(key, text, summonees))

    @Tool(
        name = "tracker_comment_update",
        description = "Изменяет текст комментария задачи Tracker.",
    )
    fun commentUpdate(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(description = "Идентификатор комментария")
        commentId: String,
        @ToolParam(description = "Новый текст комментария")
        text: String,
    ): String = render(trackerWriteService.updateComment(key, commentId, text))

    @Tool(
        name = "tracker_comment_delete",
        description = "Удаляет комментарий задачи Tracker.",
    )
    fun commentDelete(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(description = "Идентификатор комментария")
        commentId: String,
    ): String {
        trackerWriteService.deleteComment(key, commentId)
        return "Комментарий $commentId задачи $key удалён."
    }

    @Tool(
        name = "tracker_link_create",
        description = "Создаёт связь между задачами Tracker.",
    )
    fun linkCreate(
        @ToolParam(description = "Ключ исходной задачи, например TREK-42")
        key: String,
        @ToolParam(description = "Тип связи: relates, depends on, is dependent by, is subtask for, is parent task for")
        relationship: String,
        @ToolParam(description = "Ключ связываемой задачи, например TREK-43")
        issue: String,
    ): String = render(trackerWriteService.createLink(key, relationship, issue))

    @Tool(
        name = "tracker_link_delete",
        description = "Удаляет связь задачи Tracker.",
    )
    fun linkDelete(
        @ToolParam(description = "Ключ задачи, например TREK-42")
        key: String,
        @ToolParam(description = "Идентификатор связи")
        linkId: String,
    ): String {
        trackerWriteService.deleteLink(key, linkId)
        return "Связь $linkId задачи $key удалена."
    }

    /**
     * Форматирует объект ответа в читаемый JSON-текст.
     */
    private fun render(node: JsonNode): String =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
}
