package com.sorface.mcp.yandex.config

import com.sorface.mcp.yandex.common.RetryingHttpRequestInterceptor
import com.sorface.mcp.yandex.common.YandexApiAuthInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Clock
import java.net.http.HttpClient
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory

/**
 * Конфигурация общих компонентов для обращения к внешним сервисам.
 *
 * @author Sorface Developer
 */
@Configuration
class HttpClientConfig {

    /**
     * Источник текущего времени. Вынесен в bean для подмены в тестах.
     */
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    /**
     * Интерсептор повторных запросов при временных сбоях API (сеть, `429`, `5xx`).
     *
     * Регистрируется последним в цепочке интерсепторов клиентов Tracker и Wiki, поэтому повтор
     * формирует новый фактический запрос с уже добавленными заголовками авторизации.
     *
     * @param properties настройки, содержащие параметры повторных запросов
     */
    @Bean
    fun retryingHttpRequestInterceptor(
        properties: YandexProperties,
        clock: Clock,
    ): RetryingHttpRequestInterceptor = RetryingHttpRequestInterceptor(properties, clock = clock)

    /** Фабрика запросов с конечными тайм-аутами подключения и чтения ответа. */
    @Bean
    fun yandexClientHttpRequestFactory(properties: YandexProperties): ClientHttpRequestFactory {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.http.connectTimeout)
            .build()
        return JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(properties.http.readTimeout)
        }
    }

    /**
     * HTTP-клиент для обращения к сервису Яндекс OAuth.
     *
     * @param properties настройки подключения, содержащие базовый адрес OAuth-сервиса
     */
    @Bean
    fun oauthRestClient(
        properties: YandexProperties,
        yandexClientHttpRequestFactory: ClientHttpRequestFactory,
    ): RestClient =
        RestClient.builder()
            .baseUrl(properties.oauth.baseUrl)
            .requestFactory(yandexClientHttpRequestFactory)
            .build()

}
