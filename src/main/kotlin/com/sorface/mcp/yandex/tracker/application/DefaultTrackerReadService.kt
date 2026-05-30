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
        perPage: Int?,
        page: Int?,
    ): PagedResult {
        val body = buildSearchBody(query, filter, queue, keys, order)
        val pageQuery = mapOf(
            "expand" to expand,
            "perPage" to perPage?.toString(),
            "page" to page?.toString(),
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

    override fun getChangelog(key: String, field: String?, type: String?, perPage: Int?): PagedResult =
        trackerClient.getPaged(
            "/v3/issues/$key/changelog",
            mapOf(
                "field" to field,
                "type" to type,
                "perPage" to perPage?.toString(),
            ),
        )

    override fun listComments(key: String, expand: String?): JsonNode =
        trackerClient.get("/v3/issues/$key/comments", mapOf("expand" to expand))

    override fun listLinks(key: String): JsonNode =
        trackerClient.get("/v3/issues/$key/links")

    /**
     * Собирает тело запроса для `_search` и `_count`.
     *
     * Язык запросов (`query`) в Tracker используется самостоятельно и несовместим со
     * структурным фильтром, поэтому при заданном `query` остальные параметры фильтрации
     * игнорируются. Иначе формируется объект `filter` с учётом очереди и ключей.
     */
    private fun buildSearchBody(
        query: String?,
        filter: String?,
        queue: String?,
        keys: String?,
        order: String?,
    ): ObjectNode {
        val body = objectMapper.createObjectNode()

        if (!query.isNullOrBlank()) {
            body.put("query", query)
            order?.takeIf { it.isNotBlank() }?.let { body.put("order", it) }
            return body
        }

        val filterNode: ObjectNode = parseFilter(filter)
        queue?.takeIf { it.isNotBlank() }?.let { filterNode.put("queue", it) }
        if (!filterNode.isEmpty) {
            body.set<ObjectNode>("filter", filterNode)
        }

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
