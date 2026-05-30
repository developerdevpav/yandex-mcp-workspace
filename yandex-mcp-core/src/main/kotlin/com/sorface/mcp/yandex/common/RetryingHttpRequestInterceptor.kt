package com.sorface.mcp.yandex.common

import com.sorface.mcp.yandex.config.YandexProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow

/**
 * Интерсептор повторных запросов к API Яндекса при временных сбоях.
 *
 * Повтор выполняется при сетевых ошибках ([IOException]), превышении лимита запросов (`429`)
 * и временных ошибках сервиса (`5xx`). Задержка между попытками растёт экспоненциально согласно
 * настройкам [YandexProperties.RetryProperties]; для ответа `429` с заголовком `Retry-After`
 * используется указанное в нём время. Ответы с прочими статусами (включая `4xx`, кроме `429`)
 * и неретраебельные ошибки пробрасываются без повтора.
 *
 * Интерсептор должен быть последним в цепочке: повторный вызов [ClientHttpRequestExecution.execute]
 * формирует новый фактический HTTP-запрос, поэтому добавленные ранее заголовки авторизации
 * сохраняются между попытками.
 *
 * @author Sorface Developer
 */
class RetryingHttpRequestInterceptor(
    private val properties: YandexProperties,
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
                if (failure is IOException && retry.enabled && attempt < retry.maxAttempts) {
                    waitBeforeRetry(attempt, delayForAttempt(attempt), request, failure.toString())
                    attempt++
                    return@let
                }
                throwFailure(failure)
            }

            val response = outcome.getOrNull() ?: continue
            val status = response.statusCode.value()
            if (retry.enabled && attempt < retry.maxAttempts && isRetryable(status)) {
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
    private fun isRetryable(status: Int): Boolean = status == STATUS_TOO_MANY_REQUESTS || status in 500..599

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
     * Извлекает задержку из заголовка `Retry-After` (целое число секунд), если он присутствует.
     */
    private fun retryAfterMillis(response: ClientHttpResponse): Long? {
        val header = response.headers.getFirst(HttpHeaders.RETRY_AFTER)?.trim() ?: return null
        val seconds = header.toLongOrNull() ?: return null
        if (seconds < 0) return null
        return min(seconds * 1000, properties.retry.maxDelay.toMillis())
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
    }
}
