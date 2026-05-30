package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.JsonNode
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
) : WikiReadService {

    override fun getPageBySlug(slug: String, fields: String?): JsonNode =
        wikiClient.get("/v1/pages", mapOf("slug" to slug, "fields" to fields))

    override fun getPageById(id: String, fields: String?): JsonNode =
        wikiClient.get("/v1/pages/$id", mapOf("fields" to fields))

    override fun getDescendants(slug: String): JsonNode =
        wikiClient.get("/v1/pages/descendants", mapOf("slug" to slug))

    override fun getResources(id: String): JsonNode =
        wikiClient.get("/v1/pages/$id/resources")

    override fun listComments(id: String): JsonNode =
        wikiClient.get("/v1/pages/$id/comments")

    override fun listAttachments(id: String): JsonNode =
        wikiClient.get("/v1/pages/$id/attachments")
}
