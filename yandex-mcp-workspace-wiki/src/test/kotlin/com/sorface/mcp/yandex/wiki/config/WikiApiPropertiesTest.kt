package com.sorface.mcp.yandex.wiki.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Настройки API Wiki (WikiApiProperties)")
class WikiApiPropertiesTest {

    @Test
    @DisplayName("Адрес API Wiki по умолчанию указывает на публичный endpoint")
    fun `default base url points to yandex public api`() {
        assertThat(WikiApiProperties().baseUrl).isEqualTo("https://api.wiki.yandex.net")
    }
}
