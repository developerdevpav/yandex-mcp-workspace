package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sorface.mcp.yandex.common.ApiException
import com.sorface.mcp.yandex.common.WriteGuard
import com.sorface.mcp.yandex.tracker.domain.TrackerEntityContract
import com.sorface.mcp.yandex.tracker.domain.TrackerEntityType
import com.sorface.mcp.yandex.tracker.infrastructure.TrackerClient
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/**
 * Реализация изменяющих операций Entities API поверх [TrackerClient].
 *
 * @author Sorface Developer
 */
@Service
class DefaultTrackerEntityWriteService(
    private val trackerClient: TrackerClient,
    private val objectMapper: ObjectMapper,
    private val writeGuard: WriteGuard,
) : TrackerEntityWriteService {

    override fun create(
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
    ): JsonNode {
        writeGuard.ensureWritable("создание сущности")
        val type = TrackerEntityType.parse(entityType)
        val title = summary.trim().takeIf { it.isNotEmpty() }
            ?: throw ApiException(400, "Параметр summary обязателен")
        val entityFields = TrackerEntityContract.parseObject(objectMapper, fields, "fields")
        entityFields.path("summary").takeIf { it.isTextual && it.asText() != title }?.let {
            throw ApiException(400, "Параметр fields не может переопределять summary другим значением")
        }
        entityFields.put("summary", title)
        description?.let { entityFields.put("description", it) }
        lead?.takeIf { it.isNotBlank() }?.let { entityFields.put("lead", it) }
        start?.takeIf { it.isNotBlank() }?.let { entityFields.put("start", it) }
        end?.takeIf { it.isNotBlank() }?.let { entityFields.put("end", it) }
        entityStatus?.takeIf { it.isNotBlank() }?.let { entityFields.put("entityStatus", it) }
        parentEntity?.takeIf { it.isNotBlank() }?.let {
            entityFields.set<JsonNode>(
                "parentEntity",
                TrackerEntityContract.parseObject(objectMapper, it, "parentEntity"),
            )
        }
        validateFields(type, entityFields)

        val body = objectMapper.createObjectNode().set<ObjectNode>("fields", entityFields)
        links?.takeIf { it.isNotBlank() }?.let {
            body.set<JsonNode>("links", TrackerEntityContract.parseArray(objectMapper, it, "links"))
        }
        return trackerClient.post(
            "/v3/entities/${type.apiValue}",
            body,
            mapOf("fields" to TrackerEntityContract.normalizeCsv(responseFields)),
        )
    }

    override fun update(
        entityType: String,
        entityId: String,
        fields: String?,
        comment: String?,
        links: String?,
        responseFields: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("изменение сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val body = objectMapper.createObjectNode()
        fields?.takeIf { it.isNotBlank() }?.let {
            val entityFields = TrackerEntityContract.parseObject(objectMapper, it, "fields")
            if (entityFields.isEmpty) throw ApiException(400, "Параметр fields не должен быть пустым")
            validateFields(type, entityFields)
            body.set<JsonNode>("fields", entityFields)
        }
        comment?.takeIf { it.isNotBlank() }?.let { body.put("comment", it) }
        links?.takeIf { it.isNotBlank() }?.let {
            val linkItems = TrackerEntityContract.parseArray(objectMapper, it, "links")
            if (linkItems.isEmpty) throw ApiException(400, "Параметр links не должен быть пустым")
            body.set<JsonNode>("links", linkItems)
        }
        requireChanges(body)
        return trackerClient.patch(entityPath(type, id), body, responseQuery(responseFields, expand))
    }

    override fun delete(entityType: String, entityId: String, withBoard: Boolean): JsonNode {
        writeGuard.ensureWritable("удаление сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        if (withBoard && type != TrackerEntityType.PROJECT) {
            throw ApiException(400, "Параметр withBoard=true доступен только для entityType=project")
        }
        trackerClient.delete(entityPath(type, id), mapOf("withBoard" to withBoard.toString()))
        return confirmation("deleted", type, id).put("withBoard", withBoard)
    }

    override fun bulkUpdate(
        entityType: String,
        entityIds: String,
        fields: String?,
        comment: String?,
        links: String?,
    ): JsonNode {
        writeGuard.ensureWritable("пакетное изменение сущностей")
        val type = TrackerEntityType.parse(entityType)
        val ids = TrackerEntityContract.uniqueIdentifiers(entityIds, "entityIds")
        val values = objectMapper.createObjectNode()
        fields?.takeIf { it.isNotBlank() }?.let {
            val entityFields = TrackerEntityContract.parseObject(objectMapper, it, "fields")
            if (entityFields.isEmpty) throw ApiException(400, "Параметр fields не должен быть пустым")
            validateFields(type, entityFields)
            values.set<JsonNode>("fields", entityFields)
        }
        comment?.takeIf { it.isNotBlank() }?.let { values.put("comment", it) }
        links?.takeIf { it.isNotBlank() }?.let {
            val linkItems = TrackerEntityContract.parseArray(objectMapper, it, "links")
            if (linkItems.isEmpty) throw ApiException(400, "Параметр links не должен быть пустым")
            values.set<JsonNode>("links", linkItems)
        }
        requireChanges(values)
        val body = objectMapper.createObjectNode()
        body.putArray("metaEntities").addAll(ids.map { objectMapper.nodeFactory.textNode(it) })
        body.set<ObjectNode>("values", values)
        return trackerClient.post("/v3/entities/${type.apiValue}/bulkchange/_update", body)
    }

    override fun addComment(
        entityType: String,
        entityId: String,
        text: String,
        attachmentIds: String?,
        summonees: String?,
        maillistSummonees: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("добавление комментария к сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val body = commentBody(text, attachmentIds, summonees, maillistSummonees, requireText = true)
        return trackerClient.post(
            "${entityPath(type, id)}/comments",
            body,
            mapOf("expand" to TrackerEntityContract.normalizeCsv(expand)),
        )
    }

    override fun updateComment(
        entityType: String,
        entityId: String,
        commentId: String,
        text: String?,
        attachmentIds: String?,
        summonees: String?,
        maillistSummonees: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("изменение комментария сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val comment = identifier(commentId, "commentId")
        val body = commentBody(text, attachmentIds, summonees, maillistSummonees, requireText = false)
        requireChanges(body)
        return trackerClient.patch(
            "${entityPath(type, id)}/comments/$comment",
            body,
            mapOf("expand" to TrackerEntityContract.normalizeCsv(expand)),
        )
    }

    override fun deleteComment(entityType: String, entityId: String, commentId: String): JsonNode {
        writeGuard.ensureWritable("удаление комментария сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val comment = identifier(commentId, "commentId")
        trackerClient.delete("${entityPath(type, id)}/comments/$comment")
        return confirmation("deleted", type, id).put("commentId", comment)
    }

    override fun addChecklistItem(
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
    ): JsonNode {
        writeGuard.ensureWritable("добавление пункта чек-листа сущности")
        val type = checklistType(entityType)
        val id = entityId(entityId)
        val item = objectMapper.createObjectNode()
        item.put("text", text.trim().takeIf { it.isNotEmpty() } ?: throw ApiException(400, "Параметр text обязателен"))
        checked?.let { item.put("checked", it) }
        assignee?.takeIf { it.isNotBlank() }?.let { item.put("assignee", it) }
        deadline?.takeIf { it.isNotBlank() }?.let {
            item.set<JsonNode>("deadline", TrackerEntityContract.parseObject(objectMapper, it, "deadline"))
        }
        return trackerClient.post(
            "${entityPath(type, id)}/checklistItems",
            objectMapper.createArrayNode().add(item),
            mutationQuery(notify, notifyAuthor, responseFields, expand),
        )
    }

    override fun replaceChecklist(
        entityType: String,
        entityId: String,
        items: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("полная замена чек-листа сущности")
        val type = checklistType(entityType)
        val id = entityId(entityId)
        val requested = TrackerEntityContract.parseArray(objectMapper, items, "items")
        val current = currentFieldItems(type, id, "checklistItems")
        val currentById = current.associateBy { itemIdentifier(it, "пункт текущего чек-листа") }
        if (requested.size() != current.size()) {
            throw ApiException(409, "Полная замена не может менять число пунктов; используйте add/delete/clear")
        }
        val merged = objectMapper.createArrayNode()
        requested.forEach { requestedItem ->
            val requestedObject = requestedItem as? ObjectNode
                ?: throw ApiException(400, "Каждый элемент items должен быть JSON-объектом")
            val itemId = itemIdentifier(requestedObject, "пункт items")
            if (!requestedObject.path("text").isTextual || requestedObject.path("text").asText().isBlank()) {
                throw ApiException(400, "Каждый пункт items должен содержать непустые id и text")
            }
            val currentItem = currentById[itemId] as? ObjectNode
                ?: throw ApiException(409, "Пункт $itemId отсутствует в актуальном чек-листе")
            merged.add(currentItem.deepCopy().setAll<ObjectNode>(requestedObject))
        }
        if (merged.map { it.path("id").asText() }.toSet() != currentById.keys) {
            throw ApiException(409, "Набор id должен совпадать с актуальным чек-листом")
        }
        return trackerClient.patch(
            "${entityPath(type, id)}/checklistItems",
            merged,
            mutationQuery(notify, notifyAuthor, responseFields, expand),
        )
    }

    override fun updateChecklistItem(
        entityType: String,
        entityId: String,
        itemId: String,
        fields: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("изменение пункта чек-листа сущности")
        val type = checklistType(entityType)
        val id = entityId(entityId)
        val item = identifier(itemId, "itemId")
        val body = TrackerEntityContract.parseObject(objectMapper, fields, "fields")
        requireChanges(body)
        return trackerClient.patch(
            "${entityPath(type, id)}/checklistItems/$item",
            body,
            mutationQuery(notify, notifyAuthor, responseFields, expand),
        )
    }

    override fun moveChecklistItem(
        entityType: String,
        entityId: String,
        itemId: String,
        beforeItemId: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("перемещение пункта чек-листа сущности")
        val type = checklistType(entityType)
        val id = entityId(entityId)
        val item = identifier(itemId, "itemId")
        val before = identifier(beforeItemId, "beforeItemId")
        val body = objectMapper.createObjectNode().put("before", before)
        return trackerClient.post(
            "${entityPath(type, id)}/checklistItems/$item/_move",
            body,
            mutationQuery(notify, notifyAuthor, responseFields, expand),
        )
    }

    override fun deleteChecklistItem(
        entityType: String,
        entityId: String,
        itemId: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("удаление пункта чек-листа сущности")
        val type = checklistType(entityType)
        val id = entityId(entityId)
        val item = identifier(itemId, "itemId")
        return trackerClient.delete(
            "${entityPath(type, id)}/checklistItems/$item",
            mutationQuery(notify, notifyAuthor, responseFields, expand),
        )
    }

    override fun clearChecklist(
        entityType: String,
        entityId: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("полное удаление чек-листа сущности")
        val type = checklistType(entityType)
        val id = entityId(entityId)
        return trackerClient.delete(
            "${entityPath(type, id)}/checklistItems",
            mutationQuery(notify, notifyAuthor, responseFields, expand),
        )
    }

    override fun uploadTemporaryAttachment(filePath: String, fileName: String?): JsonNode {
        writeGuard.ensureWritable("загрузка временного файла Tracker")
        val file = resolveReadableFile(filePath)
        val size = Files.size(file)
        if (size > MAX_ATTACHMENT_SIZE_BYTES) {
            throw ApiException(400, "Размер файла превышает ограничение Tracker 1024 МБ")
        }
        return trackerClient.postMultipart(
            "/v3/attachments/",
            file,
            fileName?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    override fun attachTemporaryFile(
        entityType: String,
        entityId: String,
        temporaryFileId: String,
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): JsonNode {
        writeGuard.ensureWritable("прикрепление временного файла к сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val file = identifier(temporaryFileId, "temporaryFileId")
        trackerClient.postEmpty(
            "${entityPath(type, id)}/attachments/$file",
            mutationQuery(notify, notifyAuthor, responseFields, expand),
        )
        val attachments = trackerClient.get("${entityPath(type, id)}/attachments")
        val confirmed = attachments.firstOrNull { it.path("id").asText() == file }
            ?: throw ApiException(502, "Tracker не подтвердил прикрепление временного файла $file")
        return confirmation("attached", type, id).apply {
            put("temporaryFileId", file)
            set<JsonNode>("attachment", confirmed)
        }
    }

    override fun deleteAttachment(entityType: String, entityId: String, fileId: String): JsonNode {
        writeGuard.ensureWritable("удаление вложения сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val file = identifier(fileId, "fileId")
        trackerClient.delete("${entityPath(type, id)}/attachments/$file")
        return confirmation("deleted", type, id).put("fileId", file)
    }

    override fun createLink(
        entityType: String,
        entityId: String,
        relationship: String,
        rightEntityId: String,
    ): JsonNode {
        writeGuard.ensureWritable("создание связи сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val relation = TrackerEntityContract.relationship(type, relationship)
        val right = identifier(rightEntityId, "rightEntityId")
        val current = trackerClient.get("${entityPath(type, id)}/links")
        if (current.any { linkMatches(it, relation, right) }) {
            throw ApiException(409, "Связь '$relation' с сущностью $right уже существует")
        }
        val body = objectMapper.createArrayNode().add(
            objectMapper.createObjectNode().put("relationship", relation).put("entity", right),
        )
        trackerClient.post("${entityPath(type, id)}/links", body)
        val links = trackerClient.get("${entityPath(type, id)}/links")
        val confirmed = links.firstOrNull { linkMatches(it, relation, right) }
            ?: throw ApiException(502, "Tracker не подтвердил создание связи с сущностью $right")
        return confirmation("created", type, id).set<JsonNode>("link", confirmed)
    }

    override fun deleteLink(entityType: String, entityId: String, rightEntityId: String): JsonNode {
        writeGuard.ensureWritable("удаление связи сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val right = identifier(rightEntityId, "rightEntityId")
        trackerClient.delete("${entityPath(type, id)}/links", mapOf("right" to right))
        val links = trackerClient.get("${entityPath(type, id)}/links")
        if (links.any { extractRightEntityId(it) == right }) {
            throw ApiException(502, "Tracker не подтвердил удаление связи с сущностью $right")
        }
        return confirmation("deleted", type, id).put("rightEntityId", right)
    }

    override fun updateAccess(
        entityType: String,
        entityId: String,
        permissionSources: String?,
        grant: String?,
        revoke: String?,
        extended: Boolean,
    ): JsonNode {
        writeGuard.ensureWritable("изменение доступа к сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        if (!extended && !permissionSources.isNullOrBlank()) {
            throw ApiException(400, "permissionSources доступен только при extended=true")
        }
        val sources = permissionSources?.takeIf { it.isNotBlank() }
            ?.let { TrackerEntityContract.parseArray(objectMapper, it, "permissionSources") }
        val grantNode = grant?.takeIf { it.isNotBlank() }
            ?.let { TrackerEntityContract.parseObject(objectMapper, it, "grant") }
        val revokeNode = revoke?.takeIf { it.isNotBlank() }
            ?.let { TrackerEntityContract.parseObject(objectMapper, it, "revoke") }
        val changesAcl = grantNode?.isEmpty == false || revokeNode?.isEmpty == false
        if (sources?.isEmpty == false && changesAcl) {
            throw ApiException(400, "Нельзя изменять ACL при непустом permissionSources")
        }
        if (changesAcl && sources == null) {
            val current = trackerClient.get("${entityPath(type, id)}/extendedPermissions")
            if (current.path("permissionSources").isArray && !current.path("permissionSources").isEmpty) {
                throw ApiException(409, "Сущность наследует ACL; явно передайте permissionSources=[]")
            }
        }
        val acl = objectMapper.createObjectNode()
        grantNode?.takeUnless { it.isEmpty }?.let { acl.set<JsonNode>("grant", it) }
        revokeNode?.takeUnless { it.isEmpty }?.let { acl.set<JsonNode>("revoke", it) }
        val body = objectMapper.createObjectNode()
        if (extended) {
            sources?.let { body.set<JsonNode>("permissionSources", it) }
            if (!acl.isEmpty) body.set<ObjectNode>("acl", acl)
        } else {
            body.setAll<ObjectNode>(acl)
        }
        requireChanges(body)
        val resource = if (extended) "extendedPermissions" else "permissions"
        trackerClient.patch("${entityPath(type, id)}/$resource", body)
        return trackerClient.get("${entityPath(type, id)}/$resource")
    }

    override fun addGoalKeyResult(goalId: String, keyResult: String): JsonNode {
        writeGuard.ensureWritable("добавление ключевого результата цели")
        val id = entityId(goalId)
        val item = TrackerEntityContract.parseObject(objectMapper, keyResult, "keyResult")
        validateKeyResult(item)
        val operator = objectMapper.createObjectNode().set<ObjectNode>("add", item)
        return patchField(TrackerEntityType.GOAL, id, "keyResultItems", operator)
    }

    override fun updateGoalKeyResult(goalId: String, keyResultId: String, keyResult: String): JsonNode {
        writeGuard.ensureWritable("изменение ключевого результата цели")
        val id = entityId(goalId)
        val itemId = identifier(keyResultId, "keyResultId")
        val replacement = TrackerEntityContract.parseObject(objectMapper, keyResult, "keyResult")
        val current = currentFieldItems(TrackerEntityType.GOAL, id, "keyResultItems")
        val index = current.indexOfFirst { it.path("id").asText() == itemId }
        if (index < 0) throw ApiException(404, "Ключевой результат $itemId не найден")
        val merged = (current[index] as? ObjectNode)?.deepCopy()?.setAll<ObjectNode>(replacement)
            ?: throw ApiException(502, "Tracker вернул ключевой результат в неожиданном формате")
        merged.put("id", itemId)
        validateKeyResult(merged)
        current.set(index, merged)
        return patchField(TrackerEntityType.GOAL, id, "keyResultItems", current)
    }

    override fun deleteGoalKeyResult(goalId: String, keyResultId: String): JsonNode {
        writeGuard.ensureWritable("удаление ключевого результата цели")
        val id = entityId(goalId)
        val itemId = identifier(keyResultId, "keyResultId")
        val item = currentFieldItems(TrackerEntityType.GOAL, id, "keyResultItems")
            .firstOrNull { it.path("id").asText() == itemId }
            ?: throw ApiException(404, "Ключевой результат $itemId не найден")
        val operator = objectMapper.createObjectNode().set<JsonNode>("remove", item)
        return patchField(TrackerEntityType.GOAL, id, "keyResultItems", operator)
    }

    override fun replaceMetrics(entityType: String, entityId: String, metrics: String): JsonNode {
        writeGuard.ensureWritable("замена метрик сущности")
        val type = TrackerEntityType.parse(entityType)
        val id = entityId(entityId)
        val items = TrackerEntityContract.parseArray(objectMapper, metrics, "metrics")
        items.forEach { item ->
            if (!item.isObject || !item.path("text").isTextual || item.path("text").asText().isBlank()) {
                throw ApiException(400, "Каждая метрика должна быть JSON-объектом с непустым полем text")
            }
        }
        return patchField(type, id, "metricItems", items)
    }

    override fun clearMetrics(entityType: String, entityId: String): JsonNode {
        writeGuard.ensureWritable("очистка метрик сущности")
        val type = TrackerEntityType.parse(entityType)
        return patchField(type, entityId(entityId), "metricItems", objectMapper.nullNode())
    }

    private fun validateFields(type: TrackerEntityType, fields: ObjectNode) {
        TrackerEntityContract.validateWritableFields(fields)
        TrackerEntityContract.validateFieldCompatibility(type, fields)
    }

    private fun commentBody(
        text: String?,
        attachmentIds: String?,
        summonees: String?,
        maillistSummonees: String?,
        requireText: Boolean,
    ): ObjectNode = objectMapper.createObjectNode().apply {
        when {
            text != null && text.isNotBlank() -> put("text", text)
            text != null -> throw ApiException(400, "Параметр text не должен быть пустым")
            requireText -> throw ApiException(400, "Параметр text обязателен")
        }
        addCsvArray(this, "attachmentIds", attachmentIds)
        addCsvArray(this, "summonees", summonees)
        addCsvArray(this, "maillistSummonees", maillistSummonees)
    }

    private fun addCsvArray(body: ObjectNode, field: String, value: String?) {
        TrackerEntityContract.normalizeCsv(value)?.split(',')?.let { items ->
            body.putArray(field).addAll(items.map { objectMapper.nodeFactory.textNode(it) })
        }
    }

    private fun mutationQuery(
        notify: Boolean?,
        notifyAuthor: Boolean?,
        responseFields: String?,
        expand: String?,
    ): Map<String, String?> = mapOf(
        "notify" to (notify ?: true).toString(),
        "notifyAuthor" to (notifyAuthor ?: false).toString(),
        "fields" to TrackerEntityContract.normalizeCsv(responseFields),
        "expand" to TrackerEntityContract.normalizeCsv(expand),
    )

    private fun responseQuery(responseFields: String?, expand: String?): Map<String, String?> = mapOf(
        "fields" to TrackerEntityContract.normalizeCsv(responseFields),
        "expand" to TrackerEntityContract.normalizeCsv(expand),
    )

    private fun currentFieldItems(type: TrackerEntityType, id: String, field: String): ArrayNode {
        val entity = trackerClient.get(entityPath(type, id), mapOf("fields" to field))
        val items = entity.path("fields").path(field)
        return when {
            items.isArray -> items.deepCopy<ArrayNode>()
            items.isMissingNode || items.isNull -> objectMapper.createArrayNode()
            else -> throw ApiException(502, "Tracker вернул поле $field в неожиданном формате")
        }
    }

    private fun patchField(type: TrackerEntityType, id: String, field: String, value: JsonNode): JsonNode {
        val fields = objectMapper.createObjectNode().set<JsonNode>(field, value)
        val body = objectMapper.createObjectNode().set<ObjectNode>("fields", fields)
        return trackerClient.patch(entityPath(type, id), body, mapOf("fields" to field))
    }

    private fun validateKeyResult(item: ObjectNode) {
        val type = item.path("type").takeIf { it.isTextual }?.asText()
        if (type !in setOf("value", "binary")) {
            throw ApiException(400, "Поле keyResult.type должно иметь значение value или binary")
        }
        if (!item.path("text").isTextual || item.path("text").asText().isBlank()) {
            throw ApiException(400, "Поле keyResult.text обязательно")
        }
        if (type == "value" && !item.path("progress").isObject) {
            throw ApiException(400, "Для keyResult.type=value обязателен объект progress")
        }
    }

    private fun linkMatches(link: JsonNode, relationship: String, rightEntityId: String): Boolean =
        link.path("relationship").asText().equals(relationship, ignoreCase = true) &&
            extractRightEntityId(link) == rightEntityId

    private fun extractRightEntityId(link: JsonNode): String? = sequenceOf(
        link.path("entity"),
        link.path("right"),
        link.path("rightEntity"),
    ).mapNotNull { node ->
        when {
            node.isTextual -> node.asText()
            node.isObject -> node.path("id").takeIf { it.isValueNode }?.asText()
            else -> null
        }
    }.firstOrNull()

    private fun itemIdentifier(item: JsonNode, context: String): String = item.path("id")
        .takeIf { it.isValueNode && it.asText().isNotBlank() }
        ?.asText()
        ?: throw ApiException(400, "$context должен содержать непустое поле id")

    private fun confirmation(action: String, type: TrackerEntityType, id: String): ObjectNode =
        objectMapper.createObjectNode().apply {
            put(action, true)
            put("entityType", type.apiValue)
            put("entityId", id)
        }

    private fun requireChanges(body: ObjectNode) {
        if (body.isEmpty) throw ApiException(400, "Не указано ни одного изменения")
    }

    private fun resolveReadableFile(filePath: String): Path {
        val file = runCatching { Path.of(filePath) }.getOrElse {
            throw ApiException(400, "Некорректный путь к файлу")
        }
        if (!file.exists() || !file.isRegularFile()) {
            throw ApiException(400, "Файл не найден или не является обычным файлом")
        }
        return file
    }

    private fun checklistType(value: String): TrackerEntityType = TrackerEntityType.parse(value).also {
        TrackerEntityContract.requireChecklist(it)
    }

    private fun entityId(value: String): String = identifier(value, "entityId")

    private fun identifier(value: String, name: String): String = TrackerEntityContract.requireIdentifier(value, name)

    private fun entityPath(type: TrackerEntityType, id: String): String = "/v3/entities/${type.apiValue}/$id"

    private companion object {
        const val MAX_ATTACHMENT_SIZE_BYTES = 1024L * 1024L * 1024L
    }
}
