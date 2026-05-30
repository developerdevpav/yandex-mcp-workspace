package com.sorface.mcp.yandex.common

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Помощник сборки тела запроса из явных полей и произвольного JSON-объекта.
 *
 * Изменяющие инструменты принимают частые поля отдельными параметрами, а всё остальное —
 * строкой JSON-объекта. Этот помощник объединяет их в одно тело и единообразно сообщает об
 * ошибке разбора, чтобы логика не дублировалась между сервисами Tracker и Wiki.
 *
 * @author Sorface Developer
 */
object JsonFields {

    /**
     * Собирает тело запроса: сначала непустые явные поля, затем поверх — разобранный JSON-объект.
     *
     * Объект из JSON может как добавить новые поля, так и переопределить явные значения.
     *
     * @param objectMapper сериализатор для разбора и создания узлов
     * @param explicit явные поля; значения `null` пропускаются
     * @param json строка JSON-объекта с дополнительными полями
     * @param parameterName имя параметра для сообщения об ошибке
     * @return собранное тело запроса
     * @throws ApiException если строка не является корректным JSON-объектом
     */
    fun merge(
        objectMapper: ObjectMapper,
        explicit: Map<String, String?>,
        json: String?,
        parameterName: String = "fields",
    ): ObjectNode {
        val body = objectMapper.createObjectNode()
        explicit.forEach { (name, value) -> if (value != null) body.put(name, value) }
        parseObject(objectMapper, json, parameterName).fields().forEach { (name, value) ->
            body.set<JsonNode>(name, value)
        }
        return body
    }

    /**
     * Разбирает строку в JSON-объект.
     *
     * @param objectMapper сериализатор для разбора
     * @param json строка JSON-объекта; пустая строка даёт пустой объект
     * @param parameterName имя параметра для сообщения об ошибке
     * @return разобранный объект
     * @throws ApiException если строка не является корректным JSON-объектом
     */
    fun parseObject(objectMapper: ObjectMapper, json: String?, parameterName: String): ObjectNode {
        if (json.isNullOrBlank()) return objectMapper.createObjectNode()
        val parsed = runCatching { objectMapper.readTree(json) }.getOrElse {
            throw ApiException(400, "Параметр $parameterName должен быть корректным JSON-объектом")
        }
        if (parsed !is ObjectNode) {
            throw ApiException(400, "Параметр $parameterName должен быть JSON-объектом")
        }
        return parsed
    }
}
