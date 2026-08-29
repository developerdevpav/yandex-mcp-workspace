package com.sorface.mcp.yandex.auth.infrastructure

import com.sorface.mcp.yandex.config.OrgType
import com.sorface.mcp.yandex.config.YandexProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

@DisplayName("Локальные настройки авторизации (PropertiesAuthSettingsStore)")
class PropertiesAuthSettingsStoreTest {

    @Test
    @DisplayName("Настройки сохраняются без OAuth-токенов")
    fun `saves settings without tokens`(@TempDir tempDir: Path) {
        val previous = System.getProperty("user.home")
        try {
            System.setProperty("user.home", tempDir.toString())
            val store = PropertiesAuthSettingsStore()

            store.save(
                YandexProperties(
                    clientId = "client",
                    clientSecret = "secret",
                    orgId = "org",
                    orgType = OrgType.YANDEX_CLOUD,
                    oauth = YandexProperties.OAuthProperties(scopes = "tracker:read wiki:read"),
                ),
            )

            val values = Properties().apply {
                Files.newInputStream(store.path).use(::load)
            }
            assertThat(values.getProperty("yandex.client-id")).isEqualTo("client")
            assertThat(values.getProperty("yandex.client-secret")).isEqualTo("secret")
            assertThat(values.getProperty("yandex.org-type")).isEqualTo("YANDEX_CLOUD")
            assertThat(Files.readString(store.path)).doesNotContain("access_token", "refresh_token")
        } finally {
            if (previous == null) System.clearProperty("user.home") else System.setProperty("user.home", previous)
        }
    }
}
