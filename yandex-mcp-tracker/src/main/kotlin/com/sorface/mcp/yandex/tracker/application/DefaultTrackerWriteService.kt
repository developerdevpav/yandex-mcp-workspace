package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sorface.mcp.yandex.common.JsonFields
import com.sorface.mcp.yandex.common.WriteGuard
import com.sorface.mcp.yandex.tracker.infrastructure.TrackerClient
import org.springframework.stereotype.Service

/**
 * Реализация сервиса изменения Tracker поверх [TrackerClient].
 *
 * Отвечает за сборку тела запросов из удобных параметров и слияние их с произвольными полями
 * (параметр `fields`). Перед каждой операцией обращается к [WriteGuard], чтобы отклонить запрос
 * в режиме только для чтения. Структуру ответов API не изменяет.
 *
 * @author Sorface Developer
 */
@Service
class DefaultTrackerWriteService(
    private val trackerClient: TrackerClient,
    private val objectMapper: ObjectMapper,
    private val writeGuard: WriteGuard,
) : TrackerWriteService {

    override fun createIssue(
        queue: String,
        summary: String,
        description: String?,
        type: String?,
        priority: String?,
        assignee: String?,
        parent: String?,
        fields: String?,
    ): JsonNode {
        writeGuard.ensureWritable("создание задачи")
        val body = buildFields(
            explicit = mapOf(
                "queue" to queue,
                "summary" to summary,
                "description" to description,
                "type" to type,
                "priority" to priority,
                "assignee" to assignee,
                "parent" to parent,
            ),
            fields = fields,
        )
        return trackerClient.post("/v3/issues", body)
    }

    override fun updateIssue(
        key: String,
        summary: String?,
        description: String?,
        priority: String?,
        assignee: String?,
        fields: String?,
        version: Int?,
    ): JsonNode {
        writeGuard.ensureWritable("изменение задачи")
        val body = buildFields(
            explicit = mapOf(
                "summary" to summary,
                "description" to description,
                "priority" to priority,
                "assignee" to assignee,
            ),
            fields = fields,
        )
        return trackerClient.patch("/v3/issues/$key", body, mapOf("version" to version?.toString()))
    }

    override fun moveIssue(key: String, targetQueue: String, fields: String?): JsonNode {
        writeGuard.ensureWritable("перенос задачи")
        val body = buildFields(explicit = emptyMap(), fields = fields)
        return trackerClient.post("/v3/issues/$key/_move", body, mapOf("queue" to targetQueue))
    }

    override fun executeTransition(key: String, transitionId: String, comment: String?, fields: String?): JsonNode {
        writeGuard.ensureWritable("переход по статусу")
        val body = buildFields(explicit = mapOf("comment" to comment), fields = fields)
        return trackerClient.post("/v3/issues/$key/transitions/$transitionId/_execute", body)
    }

    override fun addComment(key: String, text: String, summonees: String?): JsonNode {
        writeGuard.ensureWritable("добавление комментария")
        val body = objectMapper.createObjectNode()
        body.put("text", text)
        addStringArray(body, "summonees", summonees)
        return trackerClient.post("/v3/issues/$key/comments", body)
    }

    override fun updateComment(key: String, commentId: String, text: String): JsonNode {
        writeGuard.ensureWritable("изменение комментария")
        val body = objectMapper.createObjectNode().put("text", text)
        return trackerClient.patch("/v3/issues/$key/comments/$commentId", body)
    }

    override fun deleteComment(key: String, commentId: String) {
        writeGuard.ensureWritable("удаление комментария")
        trackerClient.delete("/v3/issues/$key/comments/$commentId")
    }

    override fun createLink(key: String, relationship: String, issue: String): JsonNode {
        writeGuard.ensureWritable("создание связи")
        val body = objectMapper.createObjectNode()
            .put("relationship", relationship)
            .put("issue", issue)
        return trackerClient.post("/v3/issues/$key/links", body)
    }

    override fun deleteLink(key: String, linkId: String) {
        writeGuard.ensureWritable("удаление связи")
        trackerClient.delete("/v3/issues/$key/links/$linkId")
    }

    /**
     * Собирает тело запроса из явных полей и произвольного JSON-объекта `fields`.
     *
     * Сначала добавляются непустые явные поля, затем поверх накладывается разобранный объект
     * `fields`: он может переопределить явные значения и добавить любые поля, в том числе
     * пользовательские.
     */
    private fun buildFields(explicit: Map<String, String?>, fields: String?): ObjectNode =
        JsonFields.merge(objectMapper, explicit, fields)

    /**
     * Добавляет в тело массив строк из значения, перечисленного через запятую, если оно не пустое.
     */
    private fun addStringArray(body: ObjectNode, name: String, value: String?) {
        val items = value?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        if (!items.isNullOrEmpty()) {
            val array = body.putArray(name)
            items.forEach { array.add(it) }
        }
    }
}
