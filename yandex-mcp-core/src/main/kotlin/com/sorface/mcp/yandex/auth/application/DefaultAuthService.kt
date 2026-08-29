package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.AuthStatus
import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.auth.domain.DeviceAuthorization
import com.sorface.mcp.yandex.auth.domain.TokenSet
import com.sorface.mcp.yandex.config.YandexProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Базовая реализация сервиса авторизации по сценарию OAuth 2.0 Device Flow.
 *
 * Обновление токена выполняется лениво: при запросе токена доступа проверяется срок действия,
 * и если токен истекает в пределах запаса [EXPIRY_SKEW_SECONDS], он обновляется по токену обновления.
 * Доступ к токенам сериализуется блокировкой, чтобы параллельные вызовы инструментов не вызвали
 * одновременное обновление.
 *
 * @author Sorface Developer
 */
@Service
class DefaultAuthService(
    private val oauthClient: YandexOAuthClient,
    private val tokenStore: TokenStore,
    private val properties: YandexProperties,
    private val clock: Clock,
) : AuthService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val lock = ReentrantLock()

    override fun beginDeviceAuthorization(): DeviceAuthorization {
        requireOAuthConfigured()
        return oauthClient.requestDeviceCode(properties.oauth.scopes)
    }

    override fun completeDeviceAuthorization(authorization: DeviceAuthorization): TokenSet {
        requireOAuthConfigured()
        val deadline = clock.instant().plusSeconds(authorization.expiresInSeconds.toLong())
        var intervalMillis = authorization.intervalSeconds.coerceAtLeast(1) * 1000L

        while (clock.instant().isBefore(deadline)) {
            Thread.sleep(intervalMillis)
            when (val result = pollDeviceAuthorization(authorization)) {
                is DeviceAuthorizationProgress.Authorized -> return result.tokenSet
                is DeviceAuthorizationProgress.Pending -> {
                    if (result.slowDown) {
                        intervalMillis += 5000L
                    }
                }

                is DeviceAuthorizationProgress.Failed -> throw AuthorizationException(
                    "Авторизация не завершена: ${result.error}" +
                        (result.description?.let { " ($it)" } ?: ""),
                )
            }
        }
        throw AuthorizationException("Истёк срок ожидания подтверждения авторизации")
    }

    override fun pollDeviceAuthorization(authorization: DeviceAuthorization): DeviceAuthorizationProgress {
        requireOAuthConfigured()
        return when (val result = oauthClient.pollToken(authorization.deviceCode)) {
            is TokenPollResult.Success -> {
                tokenStore.save(result.tokenSet)
                logger.info("Авторизация подтверждена, токены сохранены")
                DeviceAuthorizationProgress.Authorized(result.tokenSet)
            }

            is TokenPollResult.Pending -> DeviceAuthorizationProgress.Pending(result.slowDown)
            is TokenPollResult.Failure -> DeviceAuthorizationProgress.Failed(result.error, result.description)
        }
    }

    override fun currentAccessToken(): String = lock.withLock {
        tokenStore.update { current ->
            current ?: throw AuthorizationException(
                "AUTH_REQUIRED: токен не найден. Вызовите MCP-инструмент yandex_auth_start.",
            )

            if (!current.isExpiring(clock.instant(), EXPIRY_SKEW_SECONDS)) {
                return@update current
            }

            val refreshToken = current.refreshToken
                ?: throw AuthorizationException(
                    "AUTH_REQUIRED: токен истёк и не может быть обновлён. Вызовите yandex_auth_start.",
                )

            logger.info("Токен доступа истекает, выполняется обновление")
            val refreshed = oauthClient.refresh(refreshToken)
            refreshed.copy(refreshToken = refreshed.refreshToken ?: current.refreshToken)
        }?.accessToken ?: throw AuthorizationException("AUTH_REQUIRED: токен не найден.")
    }

    override fun status(): AuthStatus {
        val token = tokenStore.load()
        return AuthStatus(
            configured = isConfigured(),
            authorized = token != null,
            expiresAt = token?.expiresAt,
            orgHeader = properties.orgHeaderName(),
            readOnly = properties.readOnly,
        )
    }

    override fun logout() {
        lock.withLock { tokenStore.clear() }
        logger.info("Локальные токены авторизации удалены")
    }

    private fun isConfigured(): Boolean =
        properties.clientId.isNotBlank() &&
            properties.clientSecret.isNotBlank() &&
            properties.orgId.isNotBlank()

    private fun requireOAuthConfigured() {
        if (properties.clientId.isBlank() || properties.clientSecret.isBlank()) {
            throw AuthorizationException(
                "Не заданы обязательные настройки OAuth: client_id и client_secret.",
            )
        }
    }

    private companion object {
        /** Запас времени до истечения токена, при котором инициируется обновление. */
        const val EXPIRY_SKEW_SECONDS = 60L
    }
}
