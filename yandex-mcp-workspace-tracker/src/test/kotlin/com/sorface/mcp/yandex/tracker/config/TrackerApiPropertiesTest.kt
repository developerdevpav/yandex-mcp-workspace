package com.sorface.mcp.yandex.tracker.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Настройки API Tracker (TrackerApiProperties)")
class TrackerApiPropertiesTest {

    @Test
    @DisplayName("Адрес API Tracker по умолчанию указывает на публичный endpoint")
    fun `default base url points to yandex public api`() {
        assertThat(TrackerApiProperties().baseUrl).isEqualTo("https://api.tracker.yandex.net")
    }
}
