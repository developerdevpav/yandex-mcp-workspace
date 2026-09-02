package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.JsonNode

/**
 * Сервис чтения унифицированного Entities API Yandex Tracker.
 *
 * Все методы сохраняют неизвестные поля ответа в [JsonNode]. Результаты поиска и
 * относительной пагинации нормализуются для MCP на уровне реализации сервиса.
 *
 * @author Sorface Developer
 */
interface TrackerEntityReadService {

    /** Возвращает сущность по типу и `id` либо `shortId`. */
    fun get(entityType: String, entityId: String, fields: String?, expand: String?): JsonNode

    /** Ищет сущности и возвращает нормализованную страницу `items/totalCount/totalPages`. */
    fun search(
        entityType: String,
        input: String?,
        filter: String?,
        orderBy: String?,
        orderAsc: Boolean?,
        rootOnly: Boolean?,
        fields: String?,
        perPage: Int?,
        page: Int?,
    ): JsonNode

    /** Возвращает нормализованную страницу событий сущности. */
    fun listEvents(
        entityType: String,
        entityId: String,
        perPage: Int?,
        from: String?,
        selected: String?,
        newEventsOnTop: Boolean?,
        direction: String?,
    ): JsonNode

    /** Возвращает состояние пакетной операции Tracker. */
    fun getBulkOperation(operationId: String): JsonNode

    /** Возвращает элементы пакетной операции, завершившиеся ошибкой. */
    fun listBulkOperationErrors(operationId: String): JsonNode

    /** Возвращает нормализованную страницу комментариев сущности. */
    fun listComments(
        entityType: String,
        entityId: String,
        expand: String?,
        perPage: Int?,
        from: String?,
        selected: String?,
        newCommentsOnTop: Boolean?,
        direction: String?,
    ): JsonNode

    /** Возвращает один комментарий сущности. */
    fun getComment(entityType: String, entityId: String, commentId: String, expand: String?): JsonNode

    /** Возвращает массив пунктов чек-листа проекта или портфеля. */
    fun listChecklist(entityType: String, entityId: String): JsonNode

    /** Возвращает метаданные всех вложений сущности. */
    fun listAttachments(entityType: String, entityId: String): JsonNode

    /** Возвращает метаданные одного вложения сущности. */
    fun getAttachment(entityType: String, entityId: String, fileId: String): JsonNode

    /** Возвращает связи сущности. */
    fun listLinks(entityType: String, entityId: String, fields: String?): JsonNode

    /** Возвращает настройки доступа сущности. */
    fun getAccess(entityType: String, entityId: String, extended: Boolean): JsonNode

    /** Возвращает ключевые результаты цели. */
    fun listGoalKeyResults(goalId: String): JsonNode

    /** Возвращает метрики сущности. */
    fun listMetrics(entityType: String, entityId: String): JsonNode
}
