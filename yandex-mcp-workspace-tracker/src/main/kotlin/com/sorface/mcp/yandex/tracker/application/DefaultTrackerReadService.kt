package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sorface.mcp.yandex.common.ApiException
import com.sorface.mcp.yandex.tracker.domain.PagedResult
import com.sorface.mcp.yandex.tracker.infrastructure.TrackerClient
import org.springframework.stereotype.Service

/**
 * Реализация сервиса чтения Tracker поверх [TrackerClient].
 *
 * Отвечает за сборку тела запросов поиска и подсчёта из удобных для агента параметров
 * (язык запросов, структурный фильтр, очередь, ключи) и за пробрасывание параметров
 * пагинации. Структуру ответов API не изменяет.
 *
 * @author Sorface Developer
 */
@Service
class DefaultTrackerReadService(
    private val trackerClient: TrackerClient,
    private val objectMapper: ObjectMapper,
) : TrackerReadService {

    override fun myself(): JsonNode = trackerClient.get("/v3/myself")

    override fun getIssue(key: String, expand: String?): JsonNode =
        trackerClient.get("/v3/issues/$key", mapOf("expand" to expand))

    override fun searchIssues(
        query: String?,
        filter: String?,
        queue: String?,
        keys: String?,
        order: String?,
        expand: String?,
        fields: String?,
        perPage: Int?,
        page: Int?,
        id: String?,
        scrollType: String?,
        perScroll: Int?,
        scrollTTLMillis: Int?,
        scrollId: String?,
    ): PagedResult {
        val body = buildSearchBody(query, filter, queue, keys, order)
        if (!queue.isNullOrBlank() && page != null) {
            throw ApiException(400, "Для поиска по queue используйте курсор id, параметр page не поддерживается")
        }
        val pageQuery = mapOf(
            "expand" to expand,
            "fields" to fields,
            "perPage" to perPage?.toString(),
            "page" to page?.toString(),
            "id" to id,
            "scrollType" to scrollType,
            "perScroll" to perScroll?.toString(),
            "scrollTTLMillis" to scrollTTLMillis?.toString(),
            "scrollId" to scrollId,
        )
        return trackerClient.postPaged("/v3/issues/_search", body, pageQuery)
    }

    override fun countIssues(query: String?, filter: String?, queue: String?, keys: String?): Long {
        val body = buildSearchBody(query, filter, queue, keys, order = null)
        return trackerClient.post("/v3/issues/_count", body).asLong()
    }

    override fun listQueues(expand: String?, perPage: Int?, page: Int?): PagedResult =
        trackerClient.getPaged(
            "/v3/queues",
            mapOf(
                "expand" to expand,
                "perPage" to perPage?.toString(),
                "page" to page?.toString(),
            ),
        )

    override fun getQueue(id: String, expand: String?): JsonNode =
        trackerClient.get("/v3/queues/$id", mapOf("expand" to expand))

    override fun listIssueTypes(): JsonNode = trackerClient.get("/v3/issuetypes")

    override fun listPriorities(): JsonNode = trackerClient.get("/v3/priorities")

    override fun listStatuses(): JsonNode = trackerClient.get("/v3/statuses")

    override fun listResolutions(): JsonNode = trackerClient.get("/v3/resolutions")

    override fun listTransitions(key: String): JsonNode =
        trackerClient.get("/v3/issues/$key/transitions")

    override fun getChangelog(key: String, field: String?, type: String?, perPage: Int?, id: String?): PagedResult =
        trackerClient.getPaged(
            "/v3/issues/$key/changelog",
            mapOf(
                "field" to field,
                "type" to type,
                "perPage" to perPage?.toString(),
                "id" to id,
            ),
        )

    override fun listComments(key: String, expand: String?, perPage: Int?, id: String?): PagedResult =
        trackerClient.getPaged(
            "/v3/issues/$key/comments",
            mapOf(
                "expand" to expand,
                "perPage" to perPage?.toString(),
                "id" to id,
            ),
        )

    override fun listLinks(key: String): JsonNode =
        trackerClient.get("/v3/issues/$key/links")

    override fun listChecklistItems(key: String): JsonNode =
        trackerClient.get("/v3/issues/$key/checklistItems")

    override fun listWorklogs(key: String): JsonNode =
        trackerClient.get("/v3/issues/$key/worklog")

    override fun listUsers(perPage: Int?, page: Int?): PagedResult =
        trackerClient.getPaged(
            "/v3/users",
            mapOf(
                "perPage" to perPage?.toString(),
                "page" to page?.toString(),
            ),
        )

    override fun getUser(id: String): JsonNode = trackerClient.get("/v3/users/$id")

    override fun listFields(): JsonNode = trackerClient.get("/v3/fields")

    override fun getField(id: String): JsonNode = trackerClient.get("/v3/fields/$id")

    override fun listQueueFields(queueId: String): JsonNode =
        trackerClient.get("/v3/queues/$queueId/fields")

    /**
     * Собирает тело запроса для `_search` и `_count`.
     *
     * API Tracker принимает ровно один критерий поиска: `query`, `filter`, `queue` или `keys`.
     * Смешивание критериев не допускается, потому что Tracker молча выбирает один из них по
     * внутреннему приоритету и возвращает неожиданный для вызывающей стороны результат.
     */
    private fun buildSearchBody(
        query: String?,
        filter: String?,
        queue: String?,
        keys: String?,
        order: String?,
    ): ObjectNode {
        val criteriaCount = listOf(query, filter, queue, keys).count { !it.isNullOrBlank() }
        if (criteriaCount != 1) {
            throw ApiException(400, "Укажите ровно один критерий поиска: query, filter, queue или keys")
        }

        val body = objectMapper.createObjectNode()
        query?.takeIf { it.isNotBlank() }?.let { body.put("query", it) }
        filter?.takeIf { it.isNotBlank() }?.let { body.set<ObjectNode>("filter", parseFilter(it)) }
        queue?.takeIf { it.isNotBlank() }?.let { body.put("queue", it) }
        keys?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { keyList ->
                val array = body.putArray("keys")
                keyList.forEach { array.add(it) }
            }

        order?.takeIf { it.isNotBlank() }?.let { body.put("order", it) }
        return body
    }

    /**
     * Разбирает строку структурного фильтра в JSON-объект.
     *
     * @throws ApiException если строка не является корректным JSON-объектом
     */
    private fun parseFilter(filter: String?): ObjectNode {
        if (filter.isNullOrBlank()) return objectMapper.createObjectNode()
        val parsed = runCatching { objectMapper.readTree(filter) }.getOrElse {
            throw ApiException(400, "Параметр filter должен быть корректным JSON-объектом")
        }
        if (parsed !is ObjectNode) {
            throw ApiException(400, "Параметр filter должен быть JSON-объектом, например {\"queue\":\"TREK\"}")
        }
        return parsed
    }
}
