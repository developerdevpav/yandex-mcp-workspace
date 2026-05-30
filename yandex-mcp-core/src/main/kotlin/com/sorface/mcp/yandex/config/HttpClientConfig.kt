package com.sorface.mcp.yandex.config

import com.sorface.mcp.yandex.common.RetryingHttpRequestInterceptor
import com.sorface.mcp.yandex.common.YandexApiAuthInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Clock

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
    fun retryingHttpRequestInterceptor(properties: YandexProperties): RetryingHttpRequestInterceptor =
        RetryingHttpRequestInterceptor(properties)

    /**
     * HTTP-клиент для обращения к сервису Яндекс OAuth.
     *
     * @param properties настройки подключения, содержащие базовый адрес OAuth-сервиса
     */
    @Bean
    fun oauthRestClient(properties: YandexProperties): RestClient =
        RestClient.builder()
            .baseUrl(properties.oauth.baseUrl)
            .build()

}
