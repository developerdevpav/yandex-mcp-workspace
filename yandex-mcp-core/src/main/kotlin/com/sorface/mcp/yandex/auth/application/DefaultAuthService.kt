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
        requireConfigured()
        return oauthClient.requestDeviceCode(properties.oauth.scopes)
    }

    override fun completeDeviceAuthorization(authorization: DeviceAuthorization): TokenSet {
        requireConfigured()
        val deadline = clock.instant().plusSeconds(authorization.expiresInSeconds.toLong())
        var intervalMillis = authorization.intervalSeconds.coerceAtLeast(1) * 1000L

        while (clock.instant().isBefore(deadline)) {
            Thread.sleep(intervalMillis)
            when (val result = oauthClient.pollToken(authorization.deviceCode)) {
                is TokenPollResult.Success -> {
                    tokenStore.save(result.tokenSet)
                    logger.info("Авторизация подтверждена, токены сохранены")
                    return result.tokenSet
                }

                is TokenPollResult.Pending -> {
                    if (result.slowDown) {
                        intervalMillis += 5000L
                    }
                }

                is TokenPollResult.Failure -> throw AuthorizationException(
                    "Авторизация не завершена: ${result.error}" +
                        (result.description?.let { " ($it)" } ?: ""),
                )
            }
        }
        throw AuthorizationException("Истёк срок ожидания подтверждения авторизации")
    }

    override fun currentAccessToken(): String = lock.withLock {
        val current = tokenStore.load()
            ?: throw AuthorizationException("Токен не найден. Выполните авторизацию командой 'auth'.")

        if (!current.isExpiring(clock.instant(), EXPIRY_SKEW_SECONDS)) {
            return current.accessToken
        }

        val refreshToken = current.refreshToken
            ?: throw AuthorizationException("Токен истёк, токен обновления отсутствует. Требуется повторная авторизация.")

        logger.info("Токен доступа истекает, выполняется обновление")
        val refreshed = oauthClient.refresh(refreshToken)
        val stored = refreshed.copy(
            refreshToken = refreshed.refreshToken ?: current.refreshToken,
        )
        tokenStore.save(stored)
        stored.accessToken
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

    private fun isConfigured(): Boolean =
        properties.clientId.isNotBlank() &&
            properties.clientSecret.isNotBlank() &&
            properties.orgId.isNotBlank()

    private fun requireConfigured() {
        if (!isConfigured()) {
            throw AuthorizationException(
                "Не заданы обязательные настройки: client_id, client_secret и идентификатор организации.",
            )
        }
    }

    private companion object {
        /** Запас времени до истечения токена, при котором инициируется обновление. */
        const val EXPIRY_SKEW_SECONDS = 60L
    }
}
