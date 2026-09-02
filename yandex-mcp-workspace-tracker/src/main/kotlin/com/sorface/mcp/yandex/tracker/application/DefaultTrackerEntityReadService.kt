package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sorface.mcp.yandex.tracker.domain.TrackerEntityContract
import com.sorface.mcp.yandex.tracker.domain.TrackerEntityType
import com.sorface.mcp.yandex.tracker.infrastructure.TrackerClient
import org.springframework.stereotype.Service

/**
 * Реализация чтения Entities API поверх [TrackerClient].
 *
 * @author Sorface Developer
 */
@Service
class DefaultTrackerEntityReadService(
    private val trackerClient: TrackerClient,
    private val objectMapper: ObjectMapper,
) : TrackerEntityReadService {

    override fun get(entityType: String, entityId: String, fields: String?, expand: String?): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        val id = TrackerEntityContract.requireIdentifier(entityId, "entityId")
        return trackerClient.get(
            entityPath(type, id),
            mapOf(
                "fields" to TrackerEntityContract.normalizeCsv(fields),
                "expand" to TrackerEntityContract.normalizeCsv(expand),
            ),
        )
    }

    override fun search(
        entityType: String,
        input: String?,
        filter: String?,
        orderBy: String?,
        orderAsc: Boolean?,
        rootOnly: Boolean?,
        fields: String?,
        perPage: Int?,
        page: Int?,
    ): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        TrackerEntityContract.validatePage(perPage, page)
        val body = objectMapper.createObjectNode()
        input?.takeIf { it.isNotBlank() }?.let { body.put("input", it) }
        filter?.takeIf { it.isNotBlank() }?.let {
            body.set<JsonNode>("filter", TrackerEntityContract.parseObject(objectMapper, it, "filter"))
        }
        orderBy?.takeIf { it.isNotBlank() }?.let { body.put("orderBy", it) }
        orderAsc?.let { body.put("orderAsc", it) }
        rootOnly?.let { body.put("rootOnly", it) }

        val response = trackerClient.post(
            "/v3/entities/${type.apiValue}/_search",
            body,
            mapOf(
                "fields" to TrackerEntityContract.normalizeCsv(fields),
                "perPage" to (perPage ?: DEFAULT_PAGE_SIZE).toString(),
                "page" to (page ?: DEFAULT_PAGE).toString(),
            ),
        )
        val normalized = (response as? ObjectNode)?.deepCopy() ?: objectMapper.createObjectNode()
        normalized.remove(listOf("values", "hits", "pages"))
        return normalized.apply {
            set<JsonNode>("items", response.path("values").takeUnless { it.isMissingNode } ?: emptyArray())
            put("totalCount", response.path("hits").asLong(0))
            put("totalPages", response.path("pages").asLong(0))
            put("page", page ?: DEFAULT_PAGE)
            put("perPage", perPage ?: DEFAULT_PAGE_SIZE)
        }
    }

    override fun listEvents(
        entityType: String,
        entityId: String,
        perPage: Int?,
        from: String?,
        selected: String?,
        newEventsOnTop: Boolean?,
        direction: String?,
    ): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        val id = TrackerEntityContract.requireIdentifier(entityId, "entityId")
        TrackerEntityContract.validateRelativePage(perPage, from, selected, direction)
        val response = trackerClient.get(
            "${entityPath(type, id)}/events/_relative",
            relativeQuery(perPage, from, selected, direction) +
                ("newEventsOnTop" to newEventsOnTop?.toString()),
        )
        return normalizeRelative(response, "events")
    }

    override fun getBulkOperation(operationId: String): JsonNode =
        trackerClient.get("/v3/bulkchange/${TrackerEntityContract.requireIdentifier(operationId, "operationId")}")

    override fun listBulkOperationErrors(operationId: String): JsonNode =
        trackerClient.get(
            "/v3/bulkchange/${TrackerEntityContract.requireIdentifier(operationId, "operationId")}/issues",
        )

    override fun listComments(
        entityType: String,
        entityId: String,
        expand: String?,
        perPage: Int?,
        from: String?,
        selected: String?,
        newCommentsOnTop: Boolean?,
        direction: String?,
    ): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        val id = TrackerEntityContract.requireIdentifier(entityId, "entityId")
        TrackerEntityContract.validateRelativePage(perPage, from, selected, direction)
        val response = trackerClient.get(
            "${entityPath(type, id)}/comments/_relative",
            relativeQuery(perPage, from, selected, direction) + mapOf(
                "expand" to TrackerEntityContract.normalizeCsv(expand),
                "newCommentsOnTop" to newCommentsOnTop?.toString(),
            ),
        )
        return normalizeRelative(response, "comments")
    }

    override fun getComment(entityType: String, entityId: String, commentId: String, expand: String?): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        val id = TrackerEntityContract.requireIdentifier(entityId, "entityId")
        val comment = TrackerEntityContract.requireIdentifier(commentId, "commentId")
        return trackerClient.get(
            "${entityPath(type, id)}/comments/$comment",
            mapOf("expand" to TrackerEntityContract.normalizeCsv(expand)),
        )
    }

    override fun listChecklist(entityType: String, entityId: String): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        TrackerEntityContract.requireChecklist(type)
        val entity = get(type.apiValue, entityId, "checklistItems", null)
        return entity.path("fields").path("checklistItems").takeUnless { it.isMissingNode } ?: emptyArray()
    }

    override fun listAttachments(entityType: String, entityId: String): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        val id = TrackerEntityContract.requireIdentifier(entityId, "entityId")
        return trackerClient.get("${entityPath(type, id)}/attachments")
    }

    override fun getAttachment(entityType: String, entityId: String, fileId: String): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        val id = TrackerEntityContract.requireIdentifier(entityId, "entityId")
        val file = TrackerEntityContract.requireIdentifier(fileId, "fileId")
        return trackerClient.get("${entityPath(type, id)}/attachments/$file")
    }

    override fun listLinks(entityType: String, entityId: String, fields: String?): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        val id = TrackerEntityContract.requireIdentifier(entityId, "entityId")
        return trackerClient.get(
            "${entityPath(type, id)}/links",
            mapOf("fields" to TrackerEntityContract.normalizeCsv(fields)),
        )
    }

    override fun getAccess(entityType: String, entityId: String, extended: Boolean): JsonNode {
        val type = TrackerEntityType.parse(entityType)
        val id = TrackerEntityContract.requireIdentifier(entityId, "entityId")
        val resource = if (extended) "extendedPermissions" else "permissions"
        return trackerClient.get("${entityPath(type, id)}/$resource")
    }

    override fun listGoalKeyResults(goalId: String): JsonNode {
        val entity = get(TrackerEntityType.GOAL.apiValue, goalId, "keyResultItems", null)
        return entity.path("fields").path("keyResultItems").takeUnless { it.isMissingNode } ?: emptyArray()
    }

    override fun listMetrics(entityType: String, entityId: String): JsonNode {
        val entity = get(entityType, entityId, "metricItems", null)
        return entity.path("fields").path("metricItems").takeUnless { it.isMissingNode } ?: emptyArray()
    }

    private fun normalizeRelative(response: JsonNode, itemsField: String): ObjectNode {
        val normalized = (response as? ObjectNode)?.deepCopy() ?: objectMapper.createObjectNode()
        normalized.remove(itemsField)
        return normalized.apply {
            set<JsonNode>(
                "items",
                when {
                    response.isArray -> response
                    response.path(itemsField).isArray -> response.path(itemsField)
                    else -> emptyArray()
                },
            )
            put("hasNext", response.path("hasNext").asBoolean(false))
            put("hasPrev", response.path("hasPrev").asBoolean(false))
        }
    }

    private fun relativeQuery(
        perPage: Int?,
        from: String?,
        selected: String?,
        direction: String?,
    ): Map<String, String?> = mapOf(
        "perPage" to (perPage ?: DEFAULT_PAGE_SIZE).toString(),
        "from" to from?.takeIf { it.isNotBlank() },
        "selected" to selected?.takeIf { it.isNotBlank() },
        "direction" to direction?.takeIf { it.isNotBlank() },
    )

    private fun entityPath(type: TrackerEntityType, entityId: String): String =
        "/v3/entities/${type.apiValue}/$entityId"

    private fun emptyArray(): ArrayNode = objectMapper.createArrayNode()

    private companion object {
        const val DEFAULT_PAGE_SIZE = 50
        const val DEFAULT_PAGE = 1
    }
}
