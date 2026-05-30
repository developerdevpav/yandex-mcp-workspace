package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.auth.domain.TokenSet
import com.sorface.mcp.yandex.config.YandexProperties
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

@DisplayName("Сервис авторизации Device Flow (DefaultAuthService)")
class DefaultAuthServiceTest {

    private val now: Instant = Instant.parse("2026-01-01T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val properties = YandexProperties(
        clientId = "client",
        clientSecret = "secret",
        orgId = "org",
    )

    @Test
    @DisplayName("Действующий токен возвращается без обновления")
    fun `returns access token when not expiring`() {
        val store = mockk<TokenStore>()
        val client = mockk<YandexOAuthClient>()
        every { store.load() } returns TokenSet("valid", "refresh", "OAuth", now.plusSeconds(3600))

        val service = DefaultAuthService(client, store, properties, clock)

        assertThat(service.currentAccessToken()).isEqualTo("valid")
        verify(exactly = 0) { client.refresh(any()) }
    }

    @Test
    @DisplayName("Истекающий токен обновляется и сохраняется")
    fun `refreshes token when expiring`() {
        val store = mockk<TokenStore>(relaxed = true)
        val client = mockk<YandexOAuthClient>()
        every { store.load() } returns TokenSet("old", "refresh", "OAuth", now.plusSeconds(10))
        every { client.refresh("refresh") } returns TokenSet("new", "refresh-2", "OAuth", now.plusSeconds(3600))

        val service = DefaultAuthService(client, store, properties, clock)

        assertThat(service.currentAccessToken()).isEqualTo("new")
        verify { store.save(match { it.accessToken == "new" }) }
    }

    @Test
    @DisplayName("Без сохранённого токена выбрасывается ошибка авторизации")
    fun `throws when no token stored`() {
        val store = mockk<TokenStore>()
        val client = mockk<YandexOAuthClient>()
        every { store.load() } returns null

        val service = DefaultAuthService(client, store, properties, clock)

        assertThatThrownBy { service.currentAccessToken() }
            .isInstanceOf(AuthorizationException::class.java)
    }
}
