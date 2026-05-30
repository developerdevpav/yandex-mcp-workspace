package com.sorface.mcp.yandex.auth.infrastructure

import com.sorface.mcp.yandex.auth.application.TokenPollResult
import com.sorface.mcp.yandex.auth.application.YandexOAuthClient
import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.auth.domain.DeviceAuthorization
import com.sorface.mcp.yandex.auth.domain.TokenSet
import com.sorface.mcp.yandex.auth.infrastructure.dto.DeviceCodeResponse
import com.sorface.mcp.yandex.auth.infrastructure.dto.TokenResponse
import com.sorface.mcp.yandex.config.YandexProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.time.Clock

/**
 * Реализация клиента Яндекс OAuth на основе [RestClient].
 *
 * Все запросы к токен-сервису отправляются в формате `application/x-www-form-urlencoded`.
 * Идентификатор и секрет приложения передаются в теле запроса. При опросе статуса
 * сервис OAuth отвечает кодом `400` с телом ошибки (`authorization_pending`, `slow_down`),
 * поэтому ответ обрабатывается без выброса исключения по статусу.
 *
 * @author Sorface Developer
 */
@Component
class RestClientYandexOAuthClient(
    private val oauthRestClient: RestClient,
    private val properties: YandexProperties,
    private val clock: Clock,
) : YandexOAuthClient {

    override fun requestDeviceCode(scopes: String): DeviceAuthorization {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("client_id", properties.clientId)
            if (scopes.isNotBlank()) add("scope", scopes)
        }

        return oauthRestClient.post()
            .uri("/device/code")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .exchange<DeviceAuthorization> { _, response ->
                val body = response.bodyTo(DeviceCodeResponse::class.java)
                if (!response.statusCode.is2xxSuccessful || body == null) {
                    throw AuthorizationException(
                        "Не удалось получить код устройства: HTTP ${response.statusCode}",
                    )
                }
                DeviceAuthorization(
                    deviceCode = body.deviceCode,
                    userCode = body.userCode,
                    verificationUrl = body.verificationUrl,
                    intervalSeconds = body.interval,
                    expiresInSeconds = body.expiresIn,
                )
            }!!
    }

    override fun pollToken(deviceCode: String): TokenPollResult {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "device_code")
            add("code", deviceCode)
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
        }

        return oauthRestClient.post()
            .uri("/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .exchange<TokenPollResult> { _, response ->
                val body = response.bodyTo(TokenResponse::class.java)
                when {
                    response.statusCode.is2xxSuccessful && body?.accessToken != null ->
                        TokenPollResult.Success(body.toTokenSet())

                    body?.error == "authorization_pending" -> TokenPollResult.Pending(slowDown = false)
                    body?.error == "slow_down" -> TokenPollResult.Pending(slowDown = true)
                    else -> TokenPollResult.Failure(
                        error = body?.error ?: "unknown_error",
                        description = body?.errorDescription,
                    )
                }
            }!!
    }

    override fun refresh(refreshToken: String): TokenSet {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
        }

        return oauthRestClient.post()
            .uri("/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .exchange<TokenSet> { _, response ->
                val body = response.bodyTo(TokenResponse::class.java)
                if (!response.statusCode.is2xxSuccessful || body?.accessToken == null) {
                    throw AuthorizationException(
                        "Не удалось обновить токен: ${body?.error ?: response.statusCode}",
                    )
                }
                body.toTokenSet()
            }!!
    }

    /**
     * Преобразует ответ токен-сервиса в доменный набор токенов, вычисляя момент истечения.
     */
    private fun TokenResponse.toTokenSet(): TokenSet = TokenSet(
        accessToken = requireNotNull(accessToken) { "Ответ не содержит access_token" },
        refreshToken = refreshToken,
        tokenType = tokenType ?: "OAuth",
        expiresAt = clock.instant().plusSeconds(expiresIn ?: 0),
    )
}
