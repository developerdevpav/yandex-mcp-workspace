package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.common.ApiException
import com.sorface.mcp.yandex.common.JsonFields
import com.sorface.mcp.yandex.common.WriteGuard
import com.sorface.mcp.yandex.wiki.infrastructure.WikiClient
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/**
 * Реализация сервиса изменения Wiki поверх [WikiClient].
 *
 * Отвечает за сборку тел запросов и оркестрацию многошаговой загрузки вложений. Перед каждой
 * операцией обращается к [WriteGuard]. Загрузка файла выполняется через сессию загрузки по
 * частям: создание сессии, отправка частей размером до [UPLOAD_PART_SIZE_BYTES], завершение
 * сессии и прикрепление к странице.
 *
 * @author Sorface Developer
 */
@Service
class DefaultWikiWriteService(
    private val wikiClient: WikiClient,
    private val objectMapper: ObjectMapper,
    private val writeGuard: WriteGuard,
) : WikiWriteService {

    override fun createPage(
        title: String,
        slug: String?,
        parentId: String?,
        content: String?,
        fields: String?,
    ): JsonNode {
        writeGuard.ensureWritable("создание страницы")
        val body = JsonFields.merge(
            objectMapper,
            explicit = mapOf(
                "title" to title,
                "slug" to slug,
                "parent_id" to parentId,
                "content" to content,
            ),
            json = fields,
        )
        return wikiClient.post("/v1/pages", body)
    }

    override fun updatePage(id: String, title: String?, content: String?, fields: String?): JsonNode {
        writeGuard.ensureWritable("изменение страницы")
        val body = JsonFields.merge(
            objectMapper,
            explicit = mapOf("title" to title, "content" to content),
            json = fields,
        )
        return wikiClient.post("/v1/pages/$id", body)
    }

    override fun deletePage(id: String): JsonNode {
        writeGuard.ensureWritable("удаление страницы")
        return wikiClient.delete("/v1/pages/$id")
    }

    override fun recoverPage(recoveryToken: String): JsonNode {
        writeGuard.ensureWritable("восстановление страницы")
        return wikiClient.postEmpty("/v1/recovery_tokens/$recoveryToken/recover")
    }

    override fun clonePage(
        id: String,
        slug: String?,
        parentId: String?,
        title: String?,
        fields: String?,
    ): JsonNode {
        writeGuard.ensureWritable("клонирование страницы")
        val body = JsonFields.merge(
            objectMapper,
            explicit = mapOf("slug" to slug, "parent_id" to parentId, "title" to title),
            json = fields,
        )
        return wikiClient.post("/v1/pages/$id/clone", body)
    }

    override fun appendContent(id: String, content: String, location: String?, anchor: String?): JsonNode {
        writeGuard.ensureWritable("дополнение содержимого страницы")
        val body = objectMapper.createObjectNode().put("content", content)
        location?.takeIf { it.isNotBlank() }?.let { body.put("location", it) }
        anchor?.takeIf { it.isNotBlank() }?.let { body.put("anchor", it) }
        return wikiClient.post("/v1/pages/$id/append-content", body)
    }

    override fun addComment(id: String, content: String, parentId: String?): JsonNode {
        writeGuard.ensureWritable("добавление комментария к странице")
        val body = objectMapper.createObjectNode().put("content", content)
        parentId?.takeIf { it.isNotBlank() }?.let { body.put("parent_id", it) }
        return wikiClient.post("/v1/pages/$id/comments", body)
    }

    override fun uploadAttachment(pageId: String, filePath: String, name: String?): JsonNode {
        writeGuard.ensureWritable("загрузка вложения")
        val file = resolveReadableFile(filePath)
        val bytes = Files.readAllBytes(file)
        val attachmentName = name?.takeIf { it.isNotBlank() } ?: file.fileName.toString()

        val sessionId = createUploadSession(attachmentName, bytes.size.toLong())
        uploadParts(sessionId, bytes)
        finishUploadSession(sessionId)
        return attachUploadSessions(pageId, sessionId)
    }

    override fun attachUploadSessions(pageId: String, sessionIds: String): JsonNode {
        writeGuard.ensureWritable("прикрепление вложения")
        val ids = sessionIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (ids.isEmpty()) {
            throw ApiException(400, "Не указан ни один идентификатор сессии загрузки")
        }
        val body = objectMapper.createObjectNode()
        val array = body.putArray("upload_sessions")
        ids.forEach { array.add(it) }
        return wikiClient.post("/v1/pages/$pageId/attachments", body)
    }

    /**
     * Создаёт сессию загрузки и возвращает её идентификатор.
     */
    private fun createUploadSession(fileName: String, size: Long): String {
        val body = objectMapper.createObjectNode()
            .put("name", fileName)
            .put("size", size)
        val response = wikiClient.post("/v1/upload_sessions", body)
        return extractSessionId(response)
    }

    /**
     * Разбивает содержимое на части и загружает их последовательно, начиная с номера 1.
     */
    private fun uploadParts(sessionId: String, bytes: ByteArray) {
        var partNumber = 1
        var offset = 0
        while (offset < bytes.size) {
            val end = minOf(offset + UPLOAD_PART_SIZE_BYTES, bytes.size)
            val chunk = bytes.copyOfRange(offset, end)
            wikiClient.putBinary(
                "/v1/upload_sessions/$sessionId/upload_part",
                chunk,
                mapOf("part_number" to partNumber.toString()),
            )
            offset = end
            partNumber++
        }
    }

    /**
     * Завершает сессию загрузки.
     */
    private fun finishUploadSession(sessionId: String) {
        wikiClient.postEmpty("/v1/upload_sessions/$sessionId/finish")
    }

    /**
     * Извлекает идентификатор сессии из ответа создания сессии.
     */
    private fun extractSessionId(response: JsonNode): String {
        val id = response.path("id").takeIf { !it.isMissingNode && !it.isNull }
            ?: response.path("session_id")
        if (id.isMissingNode || id.isNull || id.asText().isBlank()) {
            throw ApiException(502, "Ответ Wiki не содержит идентификатор сессии загрузки")
        }
        return id.asText()
    }

    /**
     * Проверяет, что путь указывает на существующий обычный файл, и возвращает его.
     *
     * @throws ApiException если файл не найден или не является обычным файлом
     */
    private fun resolveReadableFile(filePath: String): Path {
        val file = runCatching { Path.of(filePath) }.getOrElse {
            throw ApiException(400, "Некорректный путь к файлу: $filePath")
        }
        if (!file.exists() || !file.isRegularFile()) {
            throw ApiException(400, "Файл не найден или не является обычным файлом: $filePath")
        }
        return file
    }

    private companion object {
        /** Максимальный размер части файла при загрузке — 5 МБ. */
        const val UPLOAD_PART_SIZE_BYTES = 5 * 1024 * 1024
    }
}
