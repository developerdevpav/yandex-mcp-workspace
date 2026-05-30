package com.sorface.mcp.yandex.wiki.config

import com.sorface.mcp.yandex.common.RetryingHttpRequestInterceptor
import com.sorface.mcp.yandex.common.YandexApiAuthInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

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
    ): RestClient =
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestInterceptor(authInterceptor)
            .requestInterceptor(retryInterceptor)
            .build()

}
