package com.sorface.mcp.yandex.system.api

import com.sorface.mcp.yandex.config.YandexProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Служебные инструменты MCP-сервера (SystemTools)")
class SystemToolsTest {

    @Test
    @DisplayName("Проверка доступности возвращает pong")
    fun `ping returns pong`() {
        val tools = SystemTools(YandexProperties())

        assertThat(tools.ping()).isEqualTo("pong")
    }

    @Test
    @DisplayName("Сведения о сервере сообщают режим чтения и записи по умолчанию")
    fun `server info reports read-write by default`() {
        val tools = SystemTools(YandexProperties())

        assertThat(tools.serverInfo()).contains("чтение и запись")
    }

    @Test
    @DisplayName("Сведения о сервере сообщают режим только для чтения при read-only")
    fun `server info reports read-only`() {
        val tools = SystemTools(YandexProperties(readOnly = true))

        assertThat(tools.serverInfo()).contains("только чтение")
    }
}
