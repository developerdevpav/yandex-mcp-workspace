package com.sorface.mcp.yandex.auth.api

import com.sorface.mcp.yandex.auth.application.AuthService
import com.sorface.mcp.yandex.auth.application.AuthSettingsStore
import com.sorface.mcp.yandex.auth.application.BrowserLauncher
import com.sorface.mcp.yandex.auth.domain.DeviceAuthorization
import com.sorface.mcp.yandex.auth.domain.TokenSet
import com.sorface.mcp.yandex.config.OrgType
import com.sorface.mcp.yandex.config.YandexProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.ApplicationArguments
import java.nio.file.Path
import java.time.Instant

@DisplayName("Интерактивная команда настройки авторизации")
class AuthCommandRunnerTest {

    @Test
    @DisplayName("Setup запрашивает отсутствующие значения, авторизует и сохраняет настройки")
    fun `setup prompts logs in and saves settings`() {
        val properties = YandexProperties()
        val authService = mockk<AuthService>()
        val settingsStore = mockk<AuthSettingsStore>(relaxed = true)
        val browser = mockk<BrowserLauncher>()
        val arguments = mockk<ApplicationArguments>()
        val authorization = DeviceAuthorization(
            deviceCode = "private-code",
            userCode = "ABCD-1234",
            verificationUrl = "https://oauth.yandex.ru/device",
            intervalSeconds = 5,
            expiresInSeconds = 300,
        )
        val answers = ArrayDeque(listOf("client", "secret", "org", "yandex_cloud"))
        val prompter = object : AuthSetupPrompter {
            override fun read(prompt: String, secret: Boolean): String? = answers.removeFirst()
        }

        every { arguments.nonOptionArgs } returns listOf("setup")
        every { authService.beginDeviceAuthorization() } returns authorization
        every { authService.completeDeviceAuthorization(authorization) } returns TokenSet(
            accessToken = "access",
            refreshToken = "refresh",
            tokenType = "OAuth",
            expiresAt = Instant.parse("2026-08-30T12:00:00Z"),
        )
        every { browser.open(authorization.verificationUrl) } returns true
        every { settingsStore.path } returns Path.of("config.properties")

        AuthCommandRunner(authService, browser, settingsStore, properties, prompter).run(arguments)

        assertThat(properties.clientId).isEqualTo("client")
        assertThat(properties.clientSecret).isEqualTo("secret")
        assertThat(properties.orgId).isEqualTo("org")
        assertThat(properties.orgType).isEqualTo(OrgType.YANDEX_CLOUD)
        verify { settingsStore.save(properties) }
    }
}
