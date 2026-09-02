package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.JsonNode

/**
 * Сервис изменяющих операций унифицированного Entities API Yandex Tracker.
 *
 * Каждая операция защищена режимом `read-only`. Сложные поля принимаются JSON-строками,
 * валидируются и передаются без потери неизвестных свойств.
 *
 * @author Sorface Developer
 */
interface TrackerEntityWriteService {

    /** Создаёт проект, портфель или цель. */
    fun create(
        entityType: String,
        summary: String,
        description: String?,
        lead: String?,
        start: String?,
        end: String?,
        entityStatus: String?,
        parentEntity: String?,
        fields: String?,
        links: String?,
        responseFields: String?,
    ): JsonNode

    /** Изменяет поля, комментарий или связи сущности. */
    fun update(
        entityType: String,
        entityId: String,
        fields: String?,
        comment: String?,
        links: String?,
        responseFields: String?,
        expand: String?,
    ): JsonNode

    /** Удаляет сущность и возвращает структурированное подтверждение. */
    fun delete(entityType: String, entityId: String, withBoard: Boolean): JsonNode

    /** Запускает пакетное изменение сущностей одного типа. */
    fun bulkUpdate(
        entityType: String,
        entityIds: String,
        fields: String?,
        comment: String?,
        links: String?,
    ): JsonNode

    /** Добавляет комментарий к сущности. */
    fun addComment(
        entityType: String,
        entityId: String,
        text: String,
        attachmentIds: String?,
        summonees: String?,
        maillistSummonees: String?,
        expand: String?,
    ): JsonNode

    /** Изменяет комментарий сущности. */
    fun updateComment(
        entityType: String,
        entityId: String,
        commentId: String,
        text: String?,
        attachmentIds: String?,
        summonees: String?,
        maillistSummonees: String?,
        expand: String?,
    ): JsonNode

    /** Удаляет комментарий и возвращает структурированное подтверждение. */
    fun deleteComment(entityType: String, entityId: String, commentId: String): JsonNode

    /** Добавляет пункт в чек-лист проекта или портфеля. */
    fun addChecklistItem(
        entityType: String,
        entityId: String,
        text: String,
        checked: Boolean?,
        assignee: String?,
        deadline: String?,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode

    /** Полностью заменяет существующий чек-лист после проверки актуального состояния. */
    fun replaceChecklist(
        entityType: String,
        entityId: String,
        items: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode

    /** Изменяет один пункт чек-листа. */
    fun updateChecklistItem(
        entityType: String,
        entityId: String,
        itemId: String,
        fields: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode

    /** Перемещает пункт чек-листа перед другим пунктом. */
    fun moveChecklistItem(
        entityType: String,
        entityId: String,
        itemId: String,
        beforeItemId: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode

    /** Удаляет один пункт чек-листа. */
    fun deleteChecklistItem(
        entityType: String,
        entityId: String,
        itemId: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode

    /** Полностью очищает чек-лист проекта или портфеля. */
    fun clearChecklist(
        entityType: String,
        entityId: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode

    /** Загружает локальный файл во временное хранилище Tracker. */
    fun uploadTemporaryAttachment(filePath: String, fileName: String?): JsonNode

    /** Прикрепляет ранее загруженный временный файл к сущности с повторным чтением списка. */
    fun attachTemporaryFile(
        entityType: String,
        entityId: String,
        temporaryFileId: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode

    /** Удаляет прикреплённый файл и возвращает структурированное подтверждение. */
    fun deleteAttachment(entityType: String, entityId: String, fileId: String): JsonNode

    /** Создаёт связь, предварительно проверяя отсутствие дубликата. */
    fun createLink(entityType: String, entityId: String, relationship: String, rightEntityId: String): JsonNode

    /** Удаляет связь по идентификатору правой сущности. */
    fun deleteLink(entityType: String, entityId: String, rightEntityId: String): JsonNode

    /** Изменяет ACL и/или источники наследования прав. */
    fun updateAccess(
        entityType: String,
        entityId: String,
        permissionSources: String?,
        grant: String?,
        revoke: String?,
        extended: Boolean,
    ): JsonNode

    /** Добавляет ключевой результат к цели. */
    fun addGoalKeyResult(goalId: String, keyResult: String): JsonNode

    /** Заменяет выбранный ключевой результат цели с учётом актуального списка. */
    fun updateGoalKeyResult(goalId: String, keyResultId: String, keyResult: String): JsonNode

    /** Удаляет выбранный ключевой результат цели с учётом актуального списка. */
    fun deleteGoalKeyResult(goalId: String, keyResultId: String): JsonNode

    /** Полностью заменяет список метрик сущности. */
    fun replaceMetrics(entityType: String, entityId: String, metrics: String): JsonNode

    /** Очищает список метрик сущности значением `null`. */
    fun clearMetrics(entityType: String, entityId: String): JsonNode
}
