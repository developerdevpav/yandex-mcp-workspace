package com.sorface.mcp.yandex.common

import com.sorface.mcp.yandex.auth.application.AuthService
import com.sorface.mcp.yandex.config.YandexProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

/**
 * Интерсептор, добавляющий в каждый запрос к API Яндекса заголовки авторизации и организации.
 *
 * Заголовок `Authorization: OAuth <токен>` формируется из действующего токена доступа,
 * который [AuthService] при необходимости обновляет. Имя заголовка организации
 * (`X-Org-ID` для Яндекс 360 или `X-Cloud-Org-ID` для Yandex Cloud) выбирается по типу
 * организации из настроек. Токен запрашивается на каждый запрос, чтобы не использовать
 * устаревшее значение после фонового обновления.
 *
 * @author Sorface Developer
 */
@Component
class YandexApiAuthInterceptor(
    private val authService: AuthService,
    private val properties: YandexProperties,
) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        request.headers.set(HttpHeaders.AUTHORIZATION, "OAuth ${authService.currentAccessToken()}")
        request.headers.set(properties.orgHeaderName(), properties.orgId)
        return execution.execute(request, body)
    }
}
