package com.sorface.mcp.yandex.tracker.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.sorface.mcp.yandex.common.ApiErrorTranslator
import com.sorface.mcp.yandex.common.ApiException
import com.sorface.mcp.yandex.tracker.domain.PagedResult
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.file.Path
import java.util.function.Function

/**
 * Низкоуровневый HTTP-клиент к REST API Yandex Tracker.
 *
 * Инкапсулирует обращение к API через [RestClient], построение строки запроса, разбор тела
 * ответа в [JsonNode] и единый маппинг ошибок. Ответы об ошибках Tracker (`4xx`/`5xx`)
 * переводятся в [ApiException] с понятным сообщением, включающим сведения из тела ответа.
 * Заголовки авторизации и организации добавляет интерсептор, настроенный на самом [RestClient],
 * поэтому здесь они не задаются.
 *
 * @author Sorface Developer
 */
@Component
class TrackerClient(
    private val trackerRestClient: RestClient,
) {

    /**
     * Выполняет `GET`-запрос и возвращает тело ответа.
     *
     * @param path путь относительно базового адреса API, например `/v3/myself`
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun get(path: String, query: Map<String, String?> = emptyMap()): JsonNode =
        getPaged(path, query).items

    /**
     * Выполняет `GET`-запрос и возвращает тело ответа вместе со сведениями о пагинации.
     *
     * @param path путь относительно базового адреса API
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return результат с телом ответа и метаданными пагинации
     */
    fun getPaged(path: String, query: Map<String, String?> = emptyMap()): PagedResult =
        trackerRestClient.get()
            .uri(uriFunction(path, query))
            .exchange { _, response -> toPagedResult(response) }!!

    /**
     * Выполняет `POST`-запрос с телом в формате JSON и возвращает ответ вместе с пагинацией.
     *
     * @param path путь относительно базового адреса API, например `/v3/issues/_search`
     * @param body тело запроса; сериализуется в JSON
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return результат с телом ответа и метаданными пагинации
     */
    fun postPaged(path: String, body: Any, query: Map<String, String?> = emptyMap()): PagedResult =
        trackerRestClient.post()
            .uri(uriFunction(path, query))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange { _, response -> toPagedResult(response) }!!

    /**
     * Выполняет `POST`-запрос с телом в формате JSON и возвращает только тело ответа.
     *
     * @param path путь относительно базового адреса API
     * @param body тело запроса; сериализуется в JSON
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun post(path: String, body: Any, query: Map<String, String?> = emptyMap()): JsonNode =
        postPaged(path, body, query).items

    /**
     * Выполняет `POST`-запрос без тела.
     *
     * @param path путь относительно базового адреса API
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun postEmpty(path: String, query: Map<String, String?> = emptyMap()): JsonNode =
        trackerRestClient.post()
            .uri(uriFunction(path, query))
            .exchange { _, response -> toPagedResult(response) }!!
            .items

    /**
     * Загружает файл multipart-запросом, не считывая его целиком в память.
     *
     * @param path путь endpoint загрузки
     * @param file локальный обычный файл
     * @param fileName необязательное имя файла в Tracker
     * @return метаданные временного файла
     */
    fun postMultipart(path: String, file: Path, fileName: String?): JsonNode {
        val parts = LinkedMultiValueMap<String, Any>()
        parts.add("file", FileSystemResource(file))
        return trackerRestClient.post()
            .uri(uriFunction(path, mapOf("filename" to fileName)))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(parts)
            .exchange { _, response -> toPagedResult(response) }!!
            .items
    }

    /**
     * Выполняет `PATCH`-запрос с телом в формате JSON и возвращает тело ответа.
     *
     * @param path путь относительно базового адреса API, например `/v3/issues/{key}`
     * @param body тело запроса; сериализуется в JSON
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode]
     */
    fun patch(path: String, body: Any, query: Map<String, String?> = emptyMap()): JsonNode =
        trackerRestClient.patch()
            .uri(uriFunction(path, query))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange { _, response -> toPagedResult(response) }!!
            .items

    /**
     * Выполняет `DELETE`-запрос. Ответ без тела (`204 No Content`) возвращается как `null`-узел.
     *
     * @param path путь относительно базового адреса API
     * @param query параметры строки запроса; значения `null` пропускаются
     * @return тело ответа в виде [JsonNode] (как правило, `null`-узел)
     */
    fun delete(path: String, query: Map<String, String?> = emptyMap()): JsonNode =
        trackerRestClient.delete()
            .uri(uriFunction(path, query))
            .exchange { _, response -> toPagedResult(response) }!!
            .items

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
     * Разбирает ответ API: при ошибочном статусе бросает [ApiException], иначе извлекает
     * тело и сведения о пагинации из заголовков `X-Total-Count` и `X-Total-Pages`.
     */
    private fun toPagedResult(response: RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse): PagedResult {
        val body = runCatching { response.bodyTo(JsonNode::class.java) }.getOrNull()
        if (response.statusCode.isError) {
            throw mapError(response.statusCode, body)
        }
        return PagedResult(
            items = body ?: NullNode.instance,
            totalCount = response.headers.getFirst(HEADER_TOTAL_COUNT)?.toLongOrNull(),
            totalPages = response.headers.getFirst(HEADER_TOTAL_PAGES)?.toLongOrNull(),
            nextPageId = nextPageUrl(response.headers[HEADER_LINK]).let(::extractPageId),
            nextPageUrl = nextPageUrl(response.headers[HEADER_LINK]),
            scrollId = response.headers.getFirst(HEADER_SCROLL_ID),
        )
    }

    /**
     * Извлекает ссылку `rel="next"` из одного или нескольких заголовков `Link`.
     */
    private fun nextPageUrl(linkHeaders: List<String>?): String? =
        linkHeaders.orEmpty()
            .asSequence()
            .flatMap { it.split(",").asSequence() }
            .mapNotNull { NEXT_LINK_REGEX.find(it)?.groupValues?.get(1) }
            .firstOrNull()

    /**
     * Извлекает курсор `id` из ссылки следующей страницы относительной пагинации.
     */
    private fun extractPageId(nextPageUrl: String?): String? = nextPageUrl
        ?.let { runCatching { UriComponentsBuilder.fromUriString(it).build().queryParams.getFirst("id") }.getOrNull() }

    /**
     * Переводит ошибочный HTTP-статус и тело ответа Tracker в [ApiException] с понятным сообщением.
     */
    private fun mapError(status: HttpStatusCode, body: JsonNode?): ApiException =
        ApiErrorTranslator.translate("Tracker", status, extractMessages(body))

    /**
     * Извлекает текст ошибки из стандартного тела ответа Tracker: массива `errorMessages`
     * и объекта `errors` с пояснениями по полям.
     */
    private fun extractMessages(body: JsonNode?): String {
        if (body == null || body.isNull) return ""
        val parts = mutableListOf<String>()
        body.path("errorMessages").takeIf { it.isArray }?.forEach { parts += it.asText() }
        listOf("message", "error", "error_description")
            .forEach { field -> body.path(field).takeIf { it.isTextual }?.let { parts += it.asText() } }
        body.path("errors").takeIf { it.isObject }?.fields()?.forEach { (field, value) ->
            parts += "$field: ${value.asText()}"
        }
        return parts.filter { it.isNotBlank() }.joinToString("; ")
    }

    private companion object {
        const val HEADER_TOTAL_COUNT = "X-Total-Count"
        const val HEADER_TOTAL_PAGES = "X-Total-Pages"
        const val HEADER_LINK = "Link"
        const val HEADER_SCROLL_ID = "X-Scroll-Id"
        val NEXT_LINK_REGEX = Regex("""<([^>]+)>\s*;\s*rel=\"?next\"?""", RegexOption.IGNORE_CASE)
    }
}
