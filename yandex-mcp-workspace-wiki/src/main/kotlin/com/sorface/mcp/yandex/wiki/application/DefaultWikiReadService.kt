package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.common.JsonFields
import com.sorface.mcp.yandex.wiki.infrastructure.WikiClient
import org.springframework.stereotype.Service

/**
 * Реализация сервиса чтения Wiki поверх [WikiClient].
 *
 * Пробрасывает параметры адреса, идентификатора и дополнительных полей в запросы к API,
 * не изменяя структуру ответов.
 *
 * @author Sorface Developer
 */
@Service
class DefaultWikiReadService(
    private val wikiClient: WikiClient,
    private val objectMapper: ObjectMapper,
) : WikiReadService {

    override fun getPageBySlug(slug: String, fields: String?): JsonNode =
        wikiClient.get("/v1/pages", mapOf("slug" to slug, "fields" to fields))

    override fun getPageById(id: String, fields: String?): JsonNode =
        wikiClient.get("/v1/pages/$id", mapOf("fields" to fields))

    override fun getDescendantsBySlug(
        slug: String,
        cursor: String?,
        pageSize: Int?,
        actuality: String?,
        includeSelf: Boolean?,
        showAll: Boolean?,
    ): JsonNode = wikiClient.get(
        "/v1/pages/descendants",
        descendantsQuery(cursor, pageSize, actuality, includeSelf, showAll) + ("slug" to slug),
    )

    override fun getDescendantsById(
        id: String,
        cursor: String?,
        pageSize: Int?,
        actuality: String?,
        includeSelf: Boolean?,
        showAll: Boolean?,
    ): JsonNode = wikiClient.get(
        "/v1/pages/$id/descendants",
        descendantsQuery(cursor, pageSize, actuality, includeSelf, showAll),
    )

    override fun getResources(id: String, cursor: String?, type: String?): JsonNode =
        wikiClient.get("/v1/pages/$id/resources", mapOf("cursor" to cursor, "type" to type))

    override fun listComments(
        id: String,
        cursor: String?,
        pageSize: Int?,
        orderBy: String?,
        orderDirection: String?,
        statusFilter: String?,
    ): JsonNode = wikiClient.get(
        "/v1/pages/$id/comments",
        mapOf(
            "cursor" to cursor,
            "page_size" to pageSize?.toString(),
            "order_by" to orderBy,
            "order_direction" to orderDirection,
            "status_filter" to statusFilter,
        ),
    )

    override fun getCommentThread(id: String, commentId: String, cursor: String?, pageSize: Int?): JsonNode =
        wikiClient.get(
            "/v1/pages/$id/comments/$commentId/thread",
            mapOf("cursor" to cursor, "page_size" to pageSize?.toString()),
        )

    override fun listAttachments(
        id: String,
        cursor: String?,
        pageSize: Int?,
        orderBy: String?,
        orderDirection: String?,
    ): JsonNode = wikiClient.get(
        "/v1/pages/$id/attachments",
        mapOf(
            "cursor" to cursor,
            "page_size" to pageSize?.toString(),
            "order_by" to orderBy,
            "order_direction" to orderDirection,
        ),
    )

    override fun search(
        query: String,
        filters: String?,
        cursor: Int?,
        limit: Int?,
        orderBy: String?,
        highlight: Boolean?,
    ): JsonNode {
        val body = objectMapper.createObjectNode().put("query", query)
        filters?.takeIf { it.isNotBlank() }?.let {
            body.set<JsonNode>("filters", JsonFields.parseObject(objectMapper, it, "filters"))
        }
        cursor?.let { body.put("cursor", it) }
        limit?.let { body.put("limit", it) }
        orderBy?.takeIf { it.isNotBlank() }?.let { body.put("order_by", it) }
        highlight?.let { body.put("highlight", it) }
        return wikiClient.post("/v1/search", body)
    }

    override fun getCloneOperationStatus(taskId: String): JsonNode =
        wikiClient.get("/v1/operations/clone/$taskId")

    private fun descendantsQuery(
        cursor: String?,
        pageSize: Int?,
        actuality: String?,
        includeSelf: Boolean?,
        showAll: Boolean?,
    ): Map<String, String?> = mapOf(
        "cursor" to cursor,
        "page_size" to pageSize?.toString(),
        "actuality" to actuality,
        "include_self" to includeSelf?.toString(),
        "show_all" to showAll?.toString(),
    )
}
