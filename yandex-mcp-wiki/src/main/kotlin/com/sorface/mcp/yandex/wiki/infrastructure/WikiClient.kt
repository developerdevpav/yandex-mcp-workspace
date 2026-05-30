package com.sorface.mcp.yandex.wiki.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.sorface.mcp.yandex.common.ApiErrorTranslator
import com.sorface.mcp.yandex.common.ApiException
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.util.function.Function

/**
 * Низкоуровневый HTTP-клиент к REST API Yandex Wiki.
 *
 * Инкапсулирует обращение к API через [RestClient], построение строки запроса, разбор тела
 * ответа в [JsonNode] и единый маппинг ошибок. Поддерживает методы `GET`, `POST`, `DELETE`,
 * а также бинарный `PUT` для загрузки частей файла. Ошибки `4xx`/`5xx` переводятся в
 * [ApiException] через [ApiErrorTranslator]. Заголовки авторизации и организации добавляет
 * интерсептор, настроенный на самом [RestClient].
 *
 * @author Sorface Developer
 */
@Component
class WikiClient(
    private val wikiRestClient: RestClient,
) {

    /**
     * Выполняет `GET`-запрос и возвращает тело ответа.
     *
     * @param path путь относительно базового адреса API, например `/v1/pages/{id}`
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun get(path: String, query: Map<String, String?> = emptyMap()): JsonNode =
        wikiRestClient.get()
            .uri(uriFunction(path, query))
            .exchange { _, response -> readBody(response) }!!

    /**
     * Выполняет `POST`-запрос с телом в формате JSON и возвращает тело ответа.
     *
     * @param path путь относительно базового адреса API
     * @param body тело запроса; сериализуется в JSON
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun post(path: String, body: Any, query: Map<String, String?> = emptyMap()): JsonNode =
        wikiRestClient.post()
            .uri(uriFunction(path, query))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange { _, response -> readBody(response) }!!

    /**
     * Выполняет `POST`-запрос без тела и возвращает тело ответа.
     *
     * @param path путь относительно базового адреса API
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun postEmpty(path: String, query: Map<String, String?> = emptyMap()): JsonNode =
        wikiRestClient.post()
            .uri(uriFunction(path, query))
            .exchange { _, response -> readBody(response) }!!

    /**
     * Выполняет `DELETE`-запрос и возвращает тело ответа.
     *
     * Часть методов Wiki (например, удаление страницы) возвращает в теле токен восстановления.
     * Ответ без тела (`204 No Content`) возвращается как `null`-узел.
     *
     * @param path путь относительно базового адреса API
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun delete(path: String, query: Map<String, String?> = emptyMap()): JsonNode =
        wikiRestClient.delete()
            .uri(uriFunction(path, query))
            .exchange { _, response -> readBody(response) }!!

    /**
     * Выполняет `DELETE`-запрос с телом в формате JSON и возвращает тело ответа.
     *
     * Используется для пакетного удаления (например, строк или столбцов таблицы), когда список
     * удаляемых элементов передаётся в теле запроса.
     *
     * @param path путь относительно базового адреса API
     * @param body тело запроса; сериализуется в JSON
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun deleteWithBody(path: String, body: Any, query: Map<String, String?> = emptyMap()): JsonNode =
        wikiRestClient.method(HttpMethod.DELETE)
            .uri(uriFunction(path, query))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange { _, response -> readBody(response) }!!

    /**
     * Выполняет `PUT`-запрос с бинарным телом (`application/octet-stream`).
     *
     * Используется для загрузки части файла в сессию загрузки.
     *
     * @param path путь относительно базового адреса API
     * @param bytes содержимое части файла
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun putBinary(path: String, bytes: ByteArray, query: Map<String, String?> = emptyMap()): JsonNode =
        wikiRestClient.put()
            .uri(uriFunction(path, query))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
            .exchange { _, response -> readBody(response) }!!

    /**
     * Строит функцию формирования URI: добавляет путь и непустые параметры строки запроса.
     */
    private fun uriFunction(path: String, query: Map<String, String?>): Function<UriBuilder, URI> =
        Function { builder ->
            builder.path(path)
            query.forEach { (key, value) -> if (value != null) builder.queryParam(key, value) }
            builder.build()
        }

    /**
     * Разбирает ответ API: при ошибочном статусе бросает [ApiException], иначе возвращает тело.
     */
    private fun readBody(response: RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse): JsonNode {
        val body = runCatching { response.bodyTo(JsonNode::class.java) }.getOrNull()
        if (response.statusCode.isError) {
            throw ApiErrorTranslator.translate("Wiki", response.statusCode, extractMessages(body))
        }
        return body ?: NullNode.instance
    }

    /**
     * Извлекает текст ошибки из тела ответа Wiki, перебирая известные поля сообщений.
     */
    private fun extractMessages(body: JsonNode?): String {
        if (body == null || body.isNull) return ""
        val parts = mutableListOf<String>()
        listOf("human_message", "message", "debug_message", "detail", "error_description")
            .forEach { field -> body.path(field).takeIf { it.isTextual }?.let { parts += it.asText() } }
        body.path("errorMessages").takeIf { it.isArray }?.forEach { parts += it.asText() }
        body.path("errors").takeIf { it.isObject }?.fields()?.forEach { (field, value) ->
            parts += "$field: ${value.asText()}"
        }
        return parts.filter { it.isNotBlank() }.distinct().joinToString("; ")
    }
}
