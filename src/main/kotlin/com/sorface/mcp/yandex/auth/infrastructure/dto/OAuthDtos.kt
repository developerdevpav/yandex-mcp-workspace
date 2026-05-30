package com.sorface.mcp.yandex.auth.infrastructure.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Ответ сервиса OAuth на запрос кодов устройства.
 *
 * @author Sorface Developer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DeviceCodeResponse(
    @param:JsonProperty("device_code") val deviceCode: String,
    @param:JsonProperty("user_code") val userCode: String,
    @param:JsonProperty("verification_url") val verificationUrl: String,
    @param:JsonProperty("interval") val interval: Int = 5,
    @param:JsonProperty("expires_in") val expiresIn: Int,
)

/**
 * Ответ сервиса OAuth на запрос токена. Содержит либо токены, либо описание ошибки.
 *
 * @author Sorface Developer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenResponse(
    @param:JsonProperty("access_token") val accessToken: String? = null,
    @param:JsonProperty("refresh_token") val refreshToken: String? = null,
    @param:JsonProperty("token_type") val tokenType: String? = null,
    @param:JsonProperty("expires_in") val expiresIn: Long? = null,
    @param:JsonProperty("error") val error: String? = null,
    @param:JsonProperty("error_description") val errorDescription: String? = null,
)
