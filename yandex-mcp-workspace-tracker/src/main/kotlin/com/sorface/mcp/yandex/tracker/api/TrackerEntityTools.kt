package com.sorface.mcp.yandex.tracker.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.tracker.application.TrackerEntityReadService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

/**
 * MCP-инструменты чтения проектов, портфелей и целей через Entities API Tracker.
 *
 * @author Sorface Developer
 */
@Component
class TrackerEntityTools(
    private val service: TrackerEntityReadService,
    private val objectMapper: ObjectMapper,
) {

    @Tool(name = "tracker_entity_get", description = "Возвращает проект, портфель или цель по id либо shortId.")
    fun entityGet(
        @ToolParam(description = "Тип сущности: project, portfolio или goal") entityType: String,
        @ToolParam(description = "Строковый id или shortId сущности") entityId: String,
        @ToolParam(required = false, description = "Дополнительные поля через запятую") fields: String?,
        @ToolParam(required = false, description = "Расширения ответа, например attachments") expand: String?,
    ): String = render(service.get(entityType, entityId, fields, expand))

    @Tool(
        name = "tracker_entity_search",
        description = "Ищет проекты, портфели или цели и возвращает нормализованную страницу результатов.",
    )
    fun entitySearch(
        @ToolParam(description = "Тип сущности: project, portfolio или goal") entityType: String,
        @ToolParam(required = false, description = "Подстрока названия") input: String?,
        @ToolParam(required = false, description = "JSON-объект фильтра") filter: String?,
        @ToolParam(required = false, description = "Ключ поля сортировки") orderBy: String?,
        @ToolParam(required = false, description = "Сортировать по возрастанию") orderAsc: Boolean?,
        @ToolParam(required = false, description = "Возвращать только корневые сущности") rootOnly: Boolean?,
        @ToolParam(required = false, description = "Дополнительные поля ответа через запятую") fields: String?,
        @ToolParam(required = false, description = "Размер страницы, по умолчанию 50") perPage: Int?,
        @ToolParam(required = false, description = "Номер страницы, начиная с 1") page: Int?,
    ): String = render(service.search(entityType, input, filter, orderBy, orderAsc, rootOnly, fields, perPage, page))

    @Tool(
        name = "tracker_entity_event_list",
        description = "Возвращает историю событий сущности с относительной пагинацией.",
    )
    fun entityEventList(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(required = false, description = "Размер страницы, по умолчанию 50") perPage: Int?,
        @ToolParam(required = false, description = "Начать после указанного event id") from: String?,
        @ToolParam(required = false, description = "Сформировать страницу вокруг event id") selected: String?,
        @ToolParam(required = false, description = "Новые события сверху") newEventsOnTop: Boolean?,
        @ToolParam(required = false, description = "Направление: forward или backward") direction: String?,
    ): String = render(
        service.listEvents(entityType, entityId, perPage, from, selected, newEventsOnTop, direction),
    )

    @Tool(name = "tracker_bulk_operation_get", description = "Возвращает статус и прогресс пакетной операции Tracker.")
    fun bulkOperationGet(
        @ToolParam(description = "Идентификатор пакетной операции") operationId: String,
    ): String = render(service.getBulkOperation(operationId))

    @Tool(
        name = "tracker_bulk_operation_error_list",
        description = "Возвращает элементы пакетной операции Tracker, завершившиеся ошибкой.",
    )
    fun bulkOperationErrorList(
        @ToolParam(description = "Идентификатор пакетной операции") operationId: String,
    ): String = render(service.listBulkOperationErrors(operationId))

    @Tool(
        name = "tracker_entity_comment_list",
        description = "Возвращает комментарии сущности с относительной пагинацией.",
    )
    fun entityCommentList(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(required = false, description = "Расширения: all, html, attachments, reactions") expand: String?,
        @ToolParam(required = false, description = "Размер страницы, по умолчанию 50") perPage: Int?,
        @ToolParam(required = false, description = "Начать после указанного comment id") from: String?,
        @ToolParam(required = false, description = "Сформировать страницу вокруг comment id") selected: String?,
        @ToolParam(required = false, description = "Новые комментарии сверху") newCommentsOnTop: Boolean?,
        @ToolParam(required = false, description = "Направление: forward или backward") direction: String?,
    ): String = render(
        service.listComments(
            entityType,
            entityId,
            expand,
            perPage,
            from,
            selected,
            newCommentsOnTop,
            direction,
        ),
    )

    @Tool(name = "tracker_entity_comment_get", description = "Возвращает один комментарий сущности.")
    fun entityCommentGet(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Идентификатор комментария") commentId: String,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(service.getComment(entityType, entityId, commentId, expand))

    @Tool(name = "tracker_entity_checklist_list", description = "Возвращает чек-лист проекта или портфеля.")
    fun entityChecklistList(
        @ToolParam(description = "Тип сущности: project или portfolio") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
    ): String = render(service.listChecklist(entityType, entityId))

    @Tool(name = "tracker_entity_attachment_list", description = "Возвращает метаданные вложений сущности.")
    fun entityAttachmentList(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
    ): String = render(service.listAttachments(entityType, entityId))

    @Tool(name = "tracker_entity_attachment_get", description = "Возвращает метаданные одного вложения сущности.")
    fun entityAttachmentGet(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Идентификатор файла") fileId: String,
    ): String = render(service.getAttachment(entityType, entityId, fileId))

    @Tool(name = "tracker_entity_link_list", description = "Возвращает связи проекта, портфеля или цели.")
    fun entityLinkList(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(required = false, description = "Поля связанных сущностей через запятую") fields: String?,
    ): String = render(service.listLinks(entityType, entityId, fields))

    @Tool(name = "tracker_entity_access_get", description = "Возвращает ACL и, по умолчанию, источники наследования прав.")
    fun entityAccessGet(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(required = false, description = "Вернуть extendedPermissions; по умолчанию true") extended: Boolean?,
    ): String = render(service.getAccess(entityType, entityId, extended ?: true))

    @Tool(name = "tracker_goal_key_result_list", description = "Возвращает ключевые результаты цели.")
    fun goalKeyResultList(
        @ToolParam(description = "Идентификатор цели") goalId: String,
    ): String = render(service.listGoalKeyResults(goalId))

    @Tool(name = "tracker_entity_metric_list", description = "Возвращает метрики проекта, портфеля или цели.")
    fun entityMetricList(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
    ): String = render(service.listMetrics(entityType, entityId))

    private fun render(node: JsonNode): String =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
}
