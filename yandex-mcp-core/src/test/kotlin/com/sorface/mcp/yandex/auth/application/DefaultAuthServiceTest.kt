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
        val store = InMemoryTokenStore(TokenSet("valid", "refresh", "OAuth", now.plusSeconds(3600)))
        val client = mockk<YandexOAuthClient>()

        val service = DefaultAuthService(client, store, properties, clock)

        assertThat(service.currentAccessToken()).isEqualTo("valid")
        verify(exactly = 0) { client.refresh(any()) }
    }

    @Test
    @DisplayName("Истекающий токен обновляется и сохраняется")
    fun `refreshes token when expiring`() {
        val store = InMemoryTokenStore(TokenSet("old", "refresh", "OAuth", now.plusSeconds(10)))
        val client = mockk<YandexOAuthClient>()
        every { client.refresh("refresh") } returns TokenSet("new", "refresh-2", "OAuth", now.plusSeconds(3600))

        val service = DefaultAuthService(client, store, properties, clock)

        assertThat(service.currentAccessToken()).isEqualTo("new")
        assertThat(store.load()?.accessToken).isEqualTo("new")
    }

    @Test
    @DisplayName("Без сохранённого токена выбрасывается ошибка авторизации")
    fun `throws when no token stored`() {
        val store = InMemoryTokenStore(null)
        val client = mockk<YandexOAuthClient>()

        val service = DefaultAuthService(client, store, properties, clock)

        assertThatThrownBy { service.currentAccessToken() }
            .isInstanceOf(AuthorizationException::class.java)
    }

    @Test
    @DisplayName("Успешный одиночный опрос сохраняет токены")
    fun `single poll stores tokens`() {
        val store = mockk<TokenStore>(relaxed = true)
        val client = mockk<YandexOAuthClient>()
        val authorization = com.sorface.mcp.yandex.auth.domain.DeviceAuthorization(
            "device", "user", "https://oauth.yandex.ru/device", 5, 300,
        )
        val tokens = TokenSet("access", "refresh", "OAuth", now.plusSeconds(3600))
        every { client.pollToken("device") } returns TokenPollResult.Success(tokens)

        val result = DefaultAuthService(client, store, properties, clock)
            .pollDeviceAuthorization(authorization)

        assertThat(result).isEqualTo(DeviceAuthorizationProgress.Authorized(tokens))
        verify { store.save(tokens) }
    }

    @Test
    @DisplayName("Выход очищает локальное хранилище")
    fun `logout clears token store`() {
        val store = mockk<TokenStore>(relaxed = true)
        val service = DefaultAuthService(mockk(), store, properties, clock)

        service.logout()

        verify { store.clear() }
    }

    /** Минимальное согласованное хранилище для проверки refresh-транзакции. */
    private class InMemoryTokenStore(initial: TokenSet?) : TokenStore {
        private var token: TokenSet? = initial

        override fun load(): TokenSet? = token

        override fun save(tokenSet: TokenSet) {
            token = tokenSet
        }

        override fun clear() {
            token = null
        }

        override fun update(transform: (TokenSet?) -> TokenSet?): TokenSet? = synchronized(this) {
            transform(token).also { token = it }
        }
    }
}
