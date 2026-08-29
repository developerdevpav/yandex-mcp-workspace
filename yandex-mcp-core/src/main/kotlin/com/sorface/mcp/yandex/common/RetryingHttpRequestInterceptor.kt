package com.sorface.mcp.yandex.common

import com.sorface.mcp.yandex.config.YandexProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.pow

/**
 * Интерсептор повторных запросов к API Яндекса при временных сбоях.
 *
 * Повтор выполняется при сетевых ошибках ([IOException]), превышении лимита запросов (`429`)
 * и временных ошибках сервиса. Сетевые ошибки и `5xx` повторяются только для идемпотентных
 * методов, чтобы неоднозначный сбой после `POST` не создал дубликат объекта. Задержка растёт
 * экспоненциально; `Retry-After` поддерживается и в секундах, и в формате HTTP-date.
 *
 * Интерсептор должен быть последним в цепочке: повторный вызов [ClientHttpRequestExecution.execute]
 * формирует новый фактический HTTP-запрос, поэтому добавленные ранее заголовки авторизации
 * сохраняются между попытками.
 *
 * @author Sorface Developer
 */
class RetryingHttpRequestInterceptor(
    private val properties: YandexProperties,
    private val clock: Clock = Clock.systemUTC(),
    private val sleeper: (Long) -> Unit = { millis -> if (millis > 0) Thread.sleep(millis) },
) : ClientHttpRequestInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val retry = properties.retry
        var attempt = 1
        while (true) {
            val outcome = runCatching { execution.execute(request, body) }

            outcome.exceptionOrNull()?.let { failure ->
                if (
                    failure is IOException && retry.enabled && attempt < retry.maxAttempts &&
                    request.method in IDEMPOTENT_METHODS
                ) {
                    waitBeforeRetry(attempt, delayForAttempt(attempt), request, failure.toString())
                    attempt++
                    return@let
                }
                throwFailure(failure)
            }

            val response = outcome.getOrNull() ?: continue
            val status = response.statusCode.value()
            if (retry.enabled && attempt < retry.maxAttempts && isRetryable(request.method, status)) {
                val delay = retryAfterMillis(response) ?: delayForAttempt(attempt)
                response.close()
                waitBeforeRetry(attempt, delay, request, "HTTP $status")
                attempt++
                continue
            }
            return response
        }
    }

    /**
     * Признак того, что статус ответа допускает повтор: превышение лимита или временная ошибка сервиса.
     */
    private fun isRetryable(method: HttpMethod, status: Int): Boolean =
        status == STATUS_TOO_MANY_REQUESTS ||
            method in IDEMPOTENT_METHODS && (status == STATUS_REQUEST_TIMEOUT || status in RETRYABLE_SERVER_ERRORS)

    /**
     * Вычисляет задержку для попытки по экспоненциальной формуле с верхней границей.
     */
    private fun delayForAttempt(attempt: Int): Long {
        val initial = properties.retry.initialDelay.toMillis().toDouble()
        val maxDelay = properties.retry.maxDelay.toMillis().toDouble()
        val computed = initial * properties.retry.multiplier.pow((attempt - 1).toDouble())
        return min(computed, maxDelay).toLong()
    }

    /**
     * Извлекает задержку из `Retry-After`. Поддерживаются оба формата RFC: число секунд и
     * HTTP-date. Значение ограничивается настройкой `maxDelay`.
     */
    private fun retryAfterMillis(response: ClientHttpResponse): Long? {
        val header = response.headers.getFirst(HttpHeaders.RETRY_AFTER)?.trim() ?: return null
        val maxDelayMillis = properties.retry.maxDelay.toMillis()
        header.toLongOrNull()?.let { seconds ->
            if (seconds < 0) return null
            return min(seconds.coerceAtMost(Long.MAX_VALUE / 1000) * 1000, maxDelayMillis)
        }
        val retryAt = runCatching {
            ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
        }.getOrNull() ?: return null
        return Duration.between(clock.instant(), retryAt).toMillis().coerceIn(0, maxDelayMillis)
    }

    /**
     * Делает паузу перед повтором и пишет диагностику в журнал (stderr).
     */
    private fun waitBeforeRetry(attempt: Int, delayMillis: Long, request: HttpRequest, reason: String) {
        log.warn(
            "Повтор запроса {} {} после ошибки [{}]: попытка {} из {}, задержка {} мс",
            request.method,
            request.uri,
            reason,
            attempt + 1,
            properties.retry.maxAttempts,
            delayMillis,
        )
        sleeper(delayMillis)
    }

    /**
     * Пробрасывает исходную ошибку, сохраняя её тип для совместимости с обработкой выше.
     */
    private fun throwFailure(failure: Throwable): Nothing = when (failure) {
        is IOException -> throw failure
        is RuntimeException -> throw failure
        is Error -> throw failure
        else -> throw IllegalStateException(failure)
    }

    private companion object {
        const val STATUS_TOO_MANY_REQUESTS = 429
        const val STATUS_REQUEST_TIMEOUT = 408
        val RETRYABLE_SERVER_ERRORS = 500..504
        val IDEMPOTENT_METHODS = setOf(
            HttpMethod.GET,
            HttpMethod.HEAD,
            HttpMethod.OPTIONS,
            HttpMethod.PUT,
            HttpMethod.DELETE,
        )
    }
}
