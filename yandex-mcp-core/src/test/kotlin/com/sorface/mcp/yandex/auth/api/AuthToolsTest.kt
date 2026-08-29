package com.sorface.mcp.yandex.auth.api

import com.sorface.mcp.yandex.auth.application.AuthService
import com.sorface.mcp.yandex.auth.application.AuthSessionService
import com.sorface.mcp.yandex.auth.application.BrowserLauncher
import com.sorface.mcp.yandex.auth.domain.AuthSessionState
import com.sorface.mcp.yandex.auth.domain.AuthSessionView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("MCP-инструменты авторизации (AuthTools)")
class AuthToolsTest {

    @Test
    @DisplayName("Запуск авторизации открывает браузер и возвращает идентификатор сессии")
    fun `start opens browser and returns session`() {
        val authService = mockk<AuthService>()
        val sessions = mockk<AuthSessionService>()
        val browser = mockk<BrowserLauncher>()
        val session = AuthSessionView(
            sessionId = "session-1",
            state = AuthSessionState.PENDING,
            verificationUrl = "https://oauth.yandex.ru/device",
            userCode = "ABCD-1234",
            expiresAt = Instant.parse("2026-01-01T12:05:00Z"),
            nextPollAt = Instant.parse("2026-01-01T12:00:05Z"),
            message = "Ожидается подтверждение",
        )
        every { sessions.start() } returns session
        every { browser.open(session.verificationUrl!!) } returns true

        val result = AuthTools(authService, sessions, browser).startAuthorization()

        assertThat(result.session.sessionId).isEqualTo("session-1")
        assertThat(result.browserOpened).isTrue()
    }

    @Test
    @DisplayName("Выход удаляет токены через сервис авторизации")
    fun `logout clears tokens`() {
        val authService = mockk<AuthService>(relaxed = true)
        val tools = AuthTools(authService, mockk(), mockk())

        val result = tools.logout()

        verify { authService.logout() }
        assertThat(result).contains("токены удалены")
    }
}
