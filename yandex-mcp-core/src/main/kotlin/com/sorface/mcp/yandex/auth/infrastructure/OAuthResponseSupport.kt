package com.sorface.mcp.yandex.auth.infrastructure

import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.auth.infrastructure.dto.TokenResponse
import org.springframework.http.HttpStatusCode

/**
 * Формирование сообщений об ошибках ответов сервиса Яндекс OAuth.
 */
internal object OAuthResponseSupport {

    fun requireSuccess(status: HttpStatusCode, errorBody: TokenResponse?) {
        if (status.is2xxSuccessful) return
        throw AuthorizationException(oauthErrorMessage(status, errorBody))
    }

    fun oauthErrorMessage(status: HttpStatusCode, body: TokenResponse?): String {
        val code = body?.error
        val description = body?.errorDescription
        return when {
            code != null && description != null -> "OAuth: $code — $description (HTTP $status)"
            code != null -> "OAuth: $code (HTTP $status)"
            description != null -> "OAuth: $description (HTTP $status)"
            else -> "OAuth: запрос отклонён (HTTP $status)"
        }
    }
}
