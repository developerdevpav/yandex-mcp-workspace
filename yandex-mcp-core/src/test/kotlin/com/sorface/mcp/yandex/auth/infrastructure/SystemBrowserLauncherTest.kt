package com.sorface.mcp.yandex.auth.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Открытие системного браузера (SystemBrowserLauncher)")
class SystemBrowserLauncherTest {

    @Test
    @DisplayName("Схемы кроме HTTP и HTTPS не запускаются")
    fun `rejects unsafe url scheme`() {
        assertThat(SystemBrowserLauncher().open("file:///tmp/token")).isFalse()
    }
}
