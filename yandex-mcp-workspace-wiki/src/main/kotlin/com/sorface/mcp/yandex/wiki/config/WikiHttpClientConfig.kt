package com.sorface.mcp.yandex.wiki.config

import com.sorface.mcp.yandex.common.RetryingHttpRequestInterceptor
import com.sorface.mcp.yandex.common.YandexApiAuthInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.http.client.ClientHttpRequestFactory

/**
 * HTTP-клиент для API Yandex Wiki.
 *
 * @author Sorface Developer
 */
@Configuration
class WikiHttpClientConfig {

    /**
     * HTTP-клиент для обращения к API Yandex Wiki.
     *
     * Базовый адрес берётся из [WikiApiProperties], заголовки авторизации и организации
     * добавляет [YandexApiAuthInterceptor] на каждый запрос.
     */
    @Bean
    fun wikiRestClient(
        properties: WikiApiProperties,
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
