package com.sorface.mcp.yandex.tracker.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sorface.mcp.yandex.common.ApiException

/**
 * Тип сущности унифицированного Entities API Tracker.
 *
 * @property apiValue значение сегмента URI API
 * @author Sorface Developer
 */
enum class TrackerEntityType(val apiValue: String) {
    PROJECT("project"),
    PORTFOLIO("portfolio"),
    GOAL("goal"),
    ;

    /** Возвращает `true`, если сущность поддерживает чек-лист. */
    fun supportsChecklist(): Boolean = this != GOAL

    companion object {
        /**
         * Разбирает строковое значение типа сущности.
         *
         * @throws ApiException если тип не поддерживается Entities API
         */
        fun parse(value: String): TrackerEntityType = entries.firstOrNull { it.apiValue == value.trim().lowercase() }
            ?: throw ApiException(400, "Параметр entityType должен иметь значение project, portfolio или goal")
    }
}

/**
 * Централизованные правила транспортного контракта Entities API.
 *
 * Правила используются read- и write-сервисами до HTTP-запроса, чтобы одинаково
 * обрабатывать типы сущностей, JSON-параметры и несовместимые поля.
 *
 * @author Sorface Developer
 */
object TrackerEntityContract {

    private val readOnlyFields = setOf(
        "progressPercentage",
        "issueQueues",
        "lastCommentUpdatedAt",
        "linkedGoalsCount",
        "linkedProjectsCount",
    )

    private val projectAndPortfolioStatuses = setOf(
        "draft",
        "draft2",
        "in_progress",
        "according_to_plan",
        "postponed",
        "at_risk",
        "blocked",
        "launched",
        "cancelled",
    )

    private val goalStatuses = setOf(
        "draft",
        "according_to_plan",
        "at_risk",
        "blocked",
        "achieved",
        "partially_achieved",
        "not_achieved",
        "exceeded",
        "cancelled",
    )

    private val projectRelationships = setOf("depends on", "is dependent by", "works towards")
    private val portfolioRelationships = setOf("depends on", "is dependent by")
    private val goalRelationships = setOf(
        "parent entity",
        "child entity",
        "depends on",
        "is dependent by",
        "is supported by",
    )

    /** Проверяет и возвращает непустой идентификатор, безопасный для сегмента URI. */
    fun requireIdentifier(value: String, parameterName: String): String {
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            throw ApiException(400, "Параметр $parameterName обязателен")
        }
        if (normalized.any { it == '/' || it == '?' || it == '#' }) {
            throw ApiException(400, "Параметр $parameterName содержит недопустимые символы URI")
        }
        return normalized
    }

    /** Нормализует список полей: удаляет пробелы, пустые элементы и повторы. */
    fun normalizeCsv(value: String?): String? = value
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(",")

    /** Разбирает непустой JSON-объект или возвращает пустой объект. */
    fun parseObject(objectMapper: ObjectMapper, value: String?, parameterName: String): ObjectNode {
        if (value.isNullOrBlank()) return objectMapper.createObjectNode()
        val parsed = parseJson(objectMapper, value, parameterName)
        return parsed as? ObjectNode
            ?: throw ApiException(400, "Параметр $parameterName должен быть JSON-объектом")
    }

    /** Разбирает непустой JSON-массив. */
    fun parseArray(objectMapper: ObjectMapper, value: String?, parameterName: String): ArrayNode {
        if (value.isNullOrBlank()) {
            throw ApiException(400, "Параметр $parameterName обязателен и должен быть JSON-массивом")
        }
        val parsed = parseJson(objectMapper, value, parameterName)
        return parsed as? ArrayNode
            ?: throw ApiException(400, "Параметр $parameterName должен быть JSON-массивом")
    }

    /** Проверяет отсутствие явно неизменяемых полей в PATCH/POST. */
    fun validateWritableFields(fields: ObjectNode) {
        val forbidden = fields.fieldNames().asSequence().filter { it in readOnlyFields }.toList()
        if (forbidden.isNotEmpty()) {
            throw ApiException(400, "Поля доступны только для чтения: ${forbidden.joinToString(", ")}")
        }
    }

    /** Проверяет совместимость полей с типом сущности. */
    fun validateFieldCompatibility(type: TrackerEntityType, fields: ObjectNode) {
        if (type == TrackerEntityType.GOAL && fields.has("start")) {
            throw ApiException(400, "Поле start недоступно для entityType=goal")
        }
        if (type != TrackerEntityType.GOAL && fields.has("keyResultItems")) {
            throw ApiException(400, "Поле keyResultItems доступно только для entityType=goal")
        }
        if (type == TrackerEntityType.GOAL && fields.has("checklistItems")) {
            throw ApiException(400, "Поле checklistItems недоступно для entityType=goal")
        }
        fields.path("parentEntity").takeUnless { it.isMissingNode || it.isNull }?.let { parent ->
            if (!parent.isObject) {
                throw ApiException(400, "Поле parentEntity должно быть JSON-объектом")
            }
            if (type == TrackerEntityType.GOAL && parent.has("secondary")) {
                throw ApiException(400, "Поле parentEntity.secondary недоступно для entityType=goal")
            }
        }
        fields.path("entityStatus").takeUnless { it.isMissingNode }?.let { statusNode ->
            if (!statusNode.isTextual) {
                throw ApiException(400, "Поле entityStatus должно быть строкой")
            }
            val status = statusNode.asText()
            val allowed = if (type == TrackerEntityType.GOAL) goalStatuses else projectAndPortfolioStatuses
            if (status !in allowed) {
                throw ApiException(400, "Статус '$status' недоступен для entityType=${type.apiValue}")
            }
        }
    }

    /** Проверяет параметры обычной пагинации поиска. */
    fun validatePage(perPage: Int?, page: Int?) {
        if (perPage != null && perPage <= 0) throw ApiException(400, "Параметр perPage должен быть больше 0")
        if (page != null && page <= 0) throw ApiException(400, "Параметр page должен быть больше 0")
    }

    /** Проверяет параметры относительной пагинации. */
    fun validateRelativePage(perPage: Int?, from: String?, selected: String?, direction: String?) {
        if (perPage != null && perPage <= 0) throw ApiException(400, "Параметр perPage должен быть больше 0")
        if (!from.isNullOrBlank() && !selected.isNullOrBlank()) {
            throw ApiException(400, "Параметры from и selected взаимоисключающие")
        }
        if (!direction.isNullOrBlank() && direction !in setOf("forward", "backward")) {
            throw ApiException(400, "Параметр direction должен иметь значение forward или backward")
        }
    }

    /** Проверяет, что чек-лист поддерживается выбранным типом сущности. */
    fun requireChecklist(type: TrackerEntityType) {
        if (!type.supportsChecklist()) {
            throw ApiException(400, "Чек-лист недоступен для entityType=goal")
        }
    }

    /** Нормализует и проверяет тип связи для исходной сущности. */
    fun relationship(type: TrackerEntityType, value: String): String {
        val normalized = value.trim().lowercase()
        val allowed = when (type) {
            TrackerEntityType.PROJECT -> projectRelationships
            TrackerEntityType.PORTFOLIO -> portfolioRelationships
            TrackerEntityType.GOAL -> goalRelationships
        }
        if (normalized !in allowed) {
            throw ApiException(
                400,
                "Связь '$value' недоступна для entityType=${type.apiValue}; допустимы: ${allowed.joinToString(", ")}",
            )
        }
        return normalized
    }

    /** Преобразует строку идентификаторов через запятую в уникальный список. */
    fun uniqueIdentifiers(value: String, parameterName: String): List<String> {
        val items = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty()) throw ApiException(400, "Параметр $parameterName должен содержать идентификаторы")
        if (items.size != items.distinct().size) {
            throw ApiException(400, "Параметр $parameterName не должен содержать повторяющиеся идентификаторы")
        }
        return items.map { requireIdentifier(it, parameterName) }
    }

    private fun parseJson(objectMapper: ObjectMapper, value: String, parameterName: String): JsonNode =
        runCatching { objectMapper.readTree(value) }.getOrElse {
            throw ApiException(400, "Параметр $parameterName должен содержать корректный JSON")
        }
}
