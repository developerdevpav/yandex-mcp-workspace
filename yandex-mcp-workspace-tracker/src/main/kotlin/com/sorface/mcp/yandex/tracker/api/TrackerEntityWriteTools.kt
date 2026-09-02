package com.sorface.mcp.yandex.tracker.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.tracker.application.TrackerEntityWriteService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

/**
 * MCP-инструменты изменения проектов, портфелей и целей через Entities API Tracker.
 *
 * @author Sorface Developer
 */
@Component
class TrackerEntityWriteTools(
    private val service: TrackerEntityWriteService,
    private val objectMapper: ObjectMapper,
) {

    @Tool(name = "tracker_entity_create", description = "Создаёт проект, портфель или цель с обязательным summary.")
    fun entityCreate(
        @ToolParam(description = "Тип сущности: project, portfolio или goal") entityType: String,
        @ToolParam(description = "Непустое название сущности") summary: String,
        @ToolParam(required = false, description = "Описание сущности") description: String?,
        @ToolParam(required = false, description = "Логин или id ответственного") lead: String?,
        @ToolParam(required = false, description = "Дата начала; только project/portfolio") start: String?,
        @ToolParam(required = false, description = "Дедлайн") end: String?,
        @ToolParam(required = false, description = "Статус, допустимый для выбранного типа") entityStatus: String?,
        @ToolParam(required = false, description = "JSON-объект parentEntity") parentEntity: String?,
        @ToolParam(required = false, description = "JSON-объект остальных полей") fields: String?,
        @ToolParam(required = false, description = "JSON-массив начальных связей") links: String?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
    ): String = render(
        service.create(
            entityType,
            summary,
            description,
            lead,
            start,
            end,
            entityStatus,
            parentEntity,
            fields,
            links,
            responseFields,
        ),
    )

    @Tool(name = "tracker_entity_update", description = "Изменяет поля, добавляет комментарий или связи сущности.")
    fun entityUpdate(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(required = false, description = "JSON-объект изменений fields") fields: String?,
        @ToolParam(required = false, description = "Комментарий к изменению") comment: String?,
        @ToolParam(required = false, description = "JSON-массив добавляемых связей") links: String?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(service.update(entityType, entityId, fields, comment, links, responseFields, expand))

    @Tool(
        name = "tracker_entity_delete",
        description = "Удаляет сущность. withBoard=true разрешён только для project. Операция необратима.",
    )
    fun entityDelete(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(required = false, description = "Удалить связанную доску проекта; по умолчанию false") withBoard: Boolean?,
    ): String = render(service.delete(entityType, entityId, withBoard ?: false))

    @Tool(name = "tracker_entity_bulk_update", description = "Запускает пакетное изменение сущностей одного типа.")
    fun entityBulkUpdate(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Уникальные идентификаторы через запятую") entityIds: String,
        @ToolParam(required = false, description = "JSON-объект изменений fields") fields: String?,
        @ToolParam(required = false, description = "Комментарий к изменению") comment: String?,
        @ToolParam(required = false, description = "JSON-массив добавляемых связей") links: String?,
    ): String = render(service.bulkUpdate(entityType, entityIds, fields, comment, links))

    @Tool(name = "tracker_entity_comment_add", description = "Добавляет комментарий к сущности.")
    fun entityCommentAdd(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Текст комментария") text: String,
        @ToolParam(required = false, description = "Идентификаторы временных файлов через запятую") attachmentIds: String?,
        @ToolParam(required = false, description = "Логины призываемых пользователей через запятую") summonees: String?,
        @ToolParam(required = false, description = "Почтовые рассылки через запятую") maillistSummonees: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(
        service.addComment(entityType, entityId, text, attachmentIds, summonees, maillistSummonees, expand),
    )

    @Tool(name = "tracker_entity_comment_update", description = "Изменяет комментарий сущности.")
    fun entityCommentUpdate(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Идентификатор комментария") commentId: String,
        @ToolParam(required = false, description = "Новый текст") text: String?,
        @ToolParam(required = false, description = "Идентификаторы временных файлов через запятую") attachmentIds: String?,
        @ToolParam(required = false, description = "Логины призываемых пользователей через запятую") summonees: String?,
        @ToolParam(required = false, description = "Почтовые рассылки через запятую") maillistSummonees: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(
        service.updateComment(
            entityType,
            entityId,
            commentId,
            text,
            attachmentIds,
            summonees,
            maillistSummonees,
            expand,
        ),
    )

    @Tool(name = "tracker_entity_comment_delete", description = "Удаляет комментарий сущности. Операция необратима.")
    fun entityCommentDelete(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Идентификатор комментария") commentId: String,
    ): String = render(service.deleteComment(entityType, entityId, commentId))

    @Tool(name = "tracker_entity_checklist_add", description = "Добавляет пункт в чек-лист проекта или портфеля.")
    fun entityChecklistAdd(
        @ToolParam(description = "Тип сущности: project или portfolio") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Текст пункта") text: String,
        @ToolParam(required = false, description = "Признак выполнения") checked: Boolean?,
        @ToolParam(required = false, description = "Исполнитель") assignee: String?,
        @ToolParam(required = false, description = "JSON-объект deadline") deadline: String?,
        @ToolParam(required = false, description = "Уведомить участников; по умолчанию true") notify: Boolean?,
        @ToolParam(required = false, description = "Уведомить автора; по умолчанию false") notifyAuthor: Boolean?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(
        service.addChecklistItem(
            entityType,
            entityId,
            text,
            checked,
            assignee,
            deadline,
            notify,
            notifyAuthor,
            responseFields,
            expand,
        ),
    )

    @Tool(
        name = "tracker_entity_checklist_replace",
        description = "Destructive: полностью заменяет актуальный чек-лист без изменения числа пунктов; " +
            "сервис предварительно перечитывает и сохраняет неуказанные свойства.",
    )
    fun entityChecklistReplace(
        @ToolParam(description = "Тип сущности: project или portfolio") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "JSON-массив полных пунктов с id и text") items: String,
        @ToolParam(required = false, description = "Уведомить участников; по умолчанию true") notify: Boolean?,
        @ToolParam(required = false, description = "Уведомить автора; по умолчанию false") notifyAuthor: Boolean?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(
        service.replaceChecklist(entityType, entityId, items, notify, notifyAuthor, responseFields, expand),
    )

    @Tool(name = "tracker_entity_checklist_item_update", description = "Изменяет один пункт чек-листа.")
    fun entityChecklistItemUpdate(
        @ToolParam(description = "Тип сущности: project или portfolio") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Идентификатор пункта") itemId: String,
        @ToolParam(description = "JSON-объект изменяемых полей пункта") fields: String,
        @ToolParam(required = false, description = "Уведомить участников; по умолчанию true") notify: Boolean?,
        @ToolParam(required = false, description = "Уведомить автора; по умолчанию false") notifyAuthor: Boolean?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(
        service.updateChecklistItem(
            entityType,
            entityId,
            itemId,
            fields,
            notify,
            notifyAuthor,
            responseFields,
            expand,
        ),
    )

    @Tool(name = "tracker_entity_checklist_item_move", description = "Перемещает пункт чек-листа перед другим пунктом.")
    fun entityChecklistItemMove(
        @ToolParam(description = "Тип сущности: project или portfolio") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Перемещаемый пункт") itemId: String,
        @ToolParam(description = "Пункт, перед которым нужно разместить") beforeItemId: String,
        @ToolParam(required = false, description = "Уведомить участников; по умолчанию true") notify: Boolean?,
        @ToolParam(required = false, description = "Уведомить автора; по умолчанию false") notifyAuthor: Boolean?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(
        service.moveChecklistItem(
            entityType,
            entityId,
            itemId,
            beforeItemId,
            notify,
            notifyAuthor,
            responseFields,
            expand,
        ),
    )

    @Tool(name = "tracker_entity_checklist_item_delete", description = "Удаляет пункт чек-листа. Операция необратима.")
    fun entityChecklistItemDelete(
        @ToolParam(description = "Тип сущности: project или portfolio") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Идентификатор пункта") itemId: String,
        @ToolParam(required = false, description = "Уведомить участников; по умолчанию true") notify: Boolean?,
        @ToolParam(required = false, description = "Уведомить автора; по умолчанию false") notifyAuthor: Boolean?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(
        service.deleteChecklistItem(
            entityType,
            entityId,
            itemId,
            notify,
            notifyAuthor,
            responseFields,
            expand,
        ),
    )

    @Tool(
        name = "tracker_entity_checklist_clear",
        description = "Destructive: удаляет весь чек-лист проекта или портфеля. Операция необратима.",
    )
    fun entityChecklistClear(
        @ToolParam(description = "Тип сущности: project или portfolio") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(required = false, description = "Уведомить участников; по умолчанию true") notify: Boolean?,
        @ToolParam(required = false, description = "Уведомить автора; по умолчанию false") notifyAuthor: Boolean?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(service.clearChecklist(entityType, entityId, notify, notifyAuthor, responseFields, expand))

    @Tool(
        name = "tracker_temporary_attachment_upload",
        description = "Загружает локальный файл во временное хранилище Tracker; id можно использовать только один раз.",
    )
    fun temporaryAttachmentUpload(
        @ToolParam(description = "Путь к файлу, доступному MCP-серверу") filePath: String,
        @ToolParam(required = false, description = "Новое имя файла; по умолчанию исходное") fileName: String?,
    ): String = render(service.uploadTemporaryAttachment(filePath, fileName))

    @Tool(name = "tracker_entity_attachment_attach", description = "Прикрепляет временный файл и подтверждает результат чтением.")
    fun entityAttachmentAttach(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Идентификатор временного файла") temporaryFileId: String,
        @ToolParam(required = false, description = "Уведомить участников; по умолчанию true") notify: Boolean?,
        @ToolParam(required = false, description = "Уведомить автора; по умолчанию false") notifyAuthor: Boolean?,
        @ToolParam(required = false, description = "Поля ответа через запятую") responseFields: String?,
        @ToolParam(required = false, description = "Расширения ответа") expand: String?,
    ): String = render(
        service.attachTemporaryFile(
            entityType,
            entityId,
            temporaryFileId,
            notify,
            notifyAuthor,
            responseFields,
            expand,
        ),
    )

    @Tool(name = "tracker_entity_attachment_delete", description = "Удаляет вложение сущности. Операция необратима.")
    fun entityAttachmentDelete(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "Идентификатор файла") fileId: String,
    ): String = render(service.deleteAttachment(entityType, entityId, fileId))

    @Tool(name = "tracker_entity_link_create", description = "Создаёт связь после проверки дубликата и подтверждает результат чтением.")
    fun entityLinkCreate(
        @ToolParam(description = "Тип исходной сущности") entityType: String,
        @ToolParam(description = "Идентификатор исходной сущности") entityId: String,
        @ToolParam(description = "Тип связи, совместимый с исходной сущностью") relationship: String,
        @ToolParam(description = "Идентификатор правой сущности") rightEntityId: String,
    ): String = render(service.createLink(entityType, entityId, relationship, rightEntityId))

    @Tool(name = "tracker_entity_link_delete", description = "Удаляет связь по rightEntityId и подтверждает результат чтением.")
    fun entityLinkDelete(
        @ToolParam(description = "Тип исходной сущности") entityType: String,
        @ToolParam(description = "Идентификатор исходной сущности") entityId: String,
        @ToolParam(description = "Идентификатор правой сущности") rightEntityId: String,
    ): String = render(service.deleteLink(entityType, entityId, rightEntityId))

    @Tool(name = "tracker_entity_access_update", description = "Изменяет ACL и наследование прав с проверкой permissionSources.")
    fun entityAccessUpdate(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(required = false, description = "JSON-массив источников прав; [] отключает наследование") permissionSources: String?,
        @ToolParam(required = false, description = "JSON-объект выдаваемых прав") grant: String?,
        @ToolParam(required = false, description = "JSON-объект отзываемых прав") revoke: String?,
        @ToolParam(required = false, description = "Использовать extendedPermissions; по умолчанию true") extended: Boolean?,
    ): String = render(
        service.updateAccess(entityType, entityId, permissionSources, grant, revoke, extended ?: true),
    )

    @Tool(name = "tracker_goal_key_result_add", description = "Добавляет ключевой результат цели оператором add.")
    fun goalKeyResultAdd(
        @ToolParam(description = "Идентификатор цели") goalId: String,
        @ToolParam(description = "JSON-объект ключевого результата") keyResult: String,
    ): String = render(service.addGoalKeyResult(goalId, keyResult))

    @Tool(name = "tracker_goal_key_result_update", description = "Перечитывает и заменяет выбранный ключевой результат цели.")
    fun goalKeyResultUpdate(
        @ToolParam(description = "Идентификатор цели") goalId: String,
        @ToolParam(description = "Идентификатор ключевого результата") keyResultId: String,
        @ToolParam(description = "JSON-объект новых полей ключевого результата") keyResult: String,
    ): String = render(service.updateGoalKeyResult(goalId, keyResultId, keyResult))

    @Tool(name = "tracker_goal_key_result_delete", description = "Удаляет ключевой результат, передавая Tracker полный актуальный объект.")
    fun goalKeyResultDelete(
        @ToolParam(description = "Идентификатор цели") goalId: String,
        @ToolParam(description = "Идентификатор ключевого результата") keyResultId: String,
    ): String = render(service.deleteGoalKeyResult(goalId, keyResultId))

    @Tool(name = "tracker_entity_metric_replace", description = "Полностью заменяет список метрик сущности.")
    fun entityMetricReplace(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
        @ToolParam(description = "JSON-массив метрик с обязательным text") metrics: String,
    ): String = render(service.replaceMetrics(entityType, entityId, metrics))

    @Tool(name = "tracker_entity_metric_clear", description = "Очищает все метрики сущности значением null. Операция destructive.")
    fun entityMetricClear(
        @ToolParam(description = "Тип сущности") entityType: String,
        @ToolParam(description = "Идентификатор сущности") entityId: String,
    ): String = render(service.clearMetrics(entityType, entityId))

    private fun render(node: JsonNode): String =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
}
