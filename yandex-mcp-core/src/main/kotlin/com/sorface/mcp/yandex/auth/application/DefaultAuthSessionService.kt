package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.AuthSessionState
import com.sorface.mcp.yandex.auth.domain.AuthSessionView
import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.auth.domain.DeviceAuthorization
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Потокобезопасная реализация интерактивных сессий Device Flow.
 *
 * Сервис хранит только короткоживущий `device_code` в памяти процесса. MCP получает непрозрачный
 * идентификатор сессии и не может извлечь OAuth-токены или внутренний код устройства.
 *
 * @author Sorface Developer
 */
@Service
class DefaultAuthSessionService(
    private val authService: AuthService,
    private val clock: Clock,
) : AuthSessionService {

    private val sessions = ConcurrentHashMap<String, Session>()

    override fun start(): AuthSessionView {
        sessions.clear()
        val authorization = authService.beginDeviceAuthorization()
        val now = clock.instant()
        val session = Session(
            id = UUID.randomUUID().toString(),
            authorization = authorization,
            expiresAt = now.plusSeconds(authorization.expiresInSeconds.toLong()),
            intervalSeconds = authorization.intervalSeconds.coerceAtLeast(1),
            nextPollAt = now,
        )
        sessions[session.id] = session
        return session.view()
    }

    override fun poll(sessionId: String): AuthSessionView {
        val session = sessions[sessionId]
            ?: throw AuthorizationException("Сессия авторизации не найдена или была заменена. Запустите yandex_auth_start.")

        return synchronized(session) {
            if (session.state != AuthSessionState.PENDING) {
                return@synchronized session.view()
            }

            val now = clock.instant()
            if (!now.isBefore(session.expiresAt)) {
                session.state = AuthSessionState.EXPIRED
                session.message = "Срок действия кода истёк. Запустите новую авторизацию."
                return@synchronized session.view()
            }
            if (now.isBefore(session.nextPollAt)) {
                return@synchronized session.view()
            }

            when (val progress = authService.pollDeviceAuthorization(session.authorization)) {
                is DeviceAuthorizationProgress.Pending -> {
                    if (progress.slowDown) session.intervalSeconds += SLOW_DOWN_SECONDS
                    session.nextPollAt = now.plusSeconds(session.intervalSeconds.toLong())
                    session.message = "Ожидается подтверждение в браузере."
                }

                is DeviceAuthorizationProgress.Authorized -> {
                    session.state = AuthSessionState.AUTHORIZED
                    session.nextPollAt = now
                    session.message = "Авторизация завершена, токены сохранены."
                }

                is DeviceAuthorizationProgress.Failed -> {
                    session.state = AuthSessionState.FAILED
                    session.nextPollAt = now
                    session.message = buildString {
                        append("Yandex OAuth отклонил авторизацию: ${progress.error}")
                        progress.description?.takeIf { it.isNotBlank() }?.let { append(" ($it)") }
                    }
                }
            }
            session.view()
        }
    }

    /** Внутренняя запись, содержащая секретный `device_code` только в памяти процесса. */
    private data class Session(
        val id: String,
        val authorization: DeviceAuthorization,
        val expiresAt: Instant,
        var intervalSeconds: Int,
        var nextPollAt: Instant,
        var state: AuthSessionState = AuthSessionState.PENDING,
        var message: String = "Откройте страницу Yandex OAuth и подтвердите доступ.",
    ) {
        fun view(): AuthSessionView = AuthSessionView(
            sessionId = id,
            state = state,
            verificationUrl = authorization.verificationUrl.takeIf { state == AuthSessionState.PENDING },
            userCode = authorization.userCode.takeIf { state == AuthSessionState.PENDING },
            expiresAt = expiresAt,
            nextPollAt = nextPollAt.takeIf { state == AuthSessionState.PENDING },
            message = message,
        )
    }

    private companion object {
        const val SLOW_DOWN_SECONDS = 5
    }
}
