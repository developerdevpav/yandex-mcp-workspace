package com.sorface.mcp.yandex.tracker.config

import com.sorface.mcp.yandex.common.RetryingHttpRequestInterceptor
import com.sorface.mcp.yandex.common.YandexApiAuthInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.http.client.ClientHttpRequestFactory

/**
 * HTTP-клиент для API Yandex Tracker.
 *
 * @author Sorface Developer
 */
@Configuration
class TrackerHttpClientConfig {

    /**
     * HTTP-клиент для обращения к API Yandex Tracker.
     *
     * Базовый адрес берётся из [TrackerApiProperties], заголовки авторизации и организации
     * добавляет [YandexApiAuthInterceptor] на каждый запрос.
     */
    @Bean
    fun trackerRestClient(
        properties: TrackerApiProperties,
        authInterceptor: YandexApiAuthInterceptor,
        retryInterceptor: RetryingHttpRequestInterceptor,
        yandexClientHttpRequestFactory: ClientHttpRequestFactory,
    ): RestClient =
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(yandexClientHttpRequestFactory)
            .requestInterceptor(authInterceptor)
            .requestInterceptor(retryInterceptor)
            .build()
}
