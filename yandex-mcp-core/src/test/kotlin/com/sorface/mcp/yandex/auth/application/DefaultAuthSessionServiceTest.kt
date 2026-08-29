package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.AuthSessionState
import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.auth.domain.DeviceAuthorization
import com.sorface.mcp.yandex.auth.domain.TokenSet
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DisplayName("Интерактивные сессии авторизации (DefaultAuthSessionService)")
class DefaultAuthSessionServiceTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val authorization = DeviceAuthorization(
        deviceCode = "internal-device-code",
        userCode = "USER-CODE",
        verificationUrl = "https://oauth.yandex.ru/device",
        intervalSeconds = 5,
        expiresInSeconds = 300,
    )

    @Test
    @DisplayName("Новая сессия раскрывает только пользовательский код")
    fun `start hides internal device code`() {
        val authService = mockk<AuthService>()
        every { authService.beginDeviceAuthorization() } returns authorization

        val session = DefaultAuthSessionService(authService, clock).start()

        assertThat(session.state).isEqualTo(AuthSessionState.PENDING)
        assertThat(session.userCode).isEqualTo("USER-CODE")
        assertThat(session.toString()).doesNotContain("internal-device-code")
    }

    @Test
    @DisplayName("Успешный опрос завершает сессию и скрывает пользовательский код")
    fun `poll completes authorization`() {
        val authService = mockk<AuthService>()
        every { authService.beginDeviceAuthorization() } returns authorization
        every { authService.pollDeviceAuthorization(authorization) } returns
            DeviceAuthorizationProgress.Authorized(
                TokenSet("access", "refresh", "OAuth", now.plusSeconds(3600)),
            )
        val service = DefaultAuthSessionService(authService, clock)
        val started = service.start()

        val completed = service.poll(started.sessionId)

        assertThat(completed.state).isEqualTo(AuthSessionState.AUTHORIZED)
        assertThat(completed.userCode).isNull()
        assertThat(completed.verificationUrl).isNull()
    }

    @Test
    @DisplayName("Неизвестная сессия требует начать авторизацию заново")
    fun `unknown session is rejected`() {
        val service = DefaultAuthSessionService(mockk(), clock)

        assertThatThrownBy { service.poll("missing") }
            .isInstanceOf(AuthorizationException::class.java)
            .hasMessageContaining("yandex_auth_start")
    }

    @Test
    @DisplayName("Повторный опрос раньше nextPollAt не обращается к OAuth")
    fun `poll respects oauth interval`() {
        val authService = mockk<AuthService>()
        every { authService.beginDeviceAuthorization() } returns authorization
        every { authService.pollDeviceAuthorization(authorization) } returns DeviceAuthorizationProgress.Pending(false)
        val service = DefaultAuthSessionService(authService, clock)
        val started = service.start()

        service.poll(started.sessionId)
        service.poll(started.sessionId)

        verify(exactly = 1) { authService.pollDeviceAuthorization(authorization) }
    }
}
