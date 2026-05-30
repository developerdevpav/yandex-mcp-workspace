package com.sorface.mcp.yandex.auth.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.auth.domain.TokenSet
import com.sorface.mcp.yandex.config.YandexProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("Файловое хранилище токенов (FileTokenStore)")
class FileTokenStoreTest {

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    private fun store(tempDir: Path): FileTokenStore {
        val properties = YandexProperties(tokenStorePath = tempDir.resolve("tokens.json").toString())
        return FileTokenStore(properties, objectMapper)
    }

    @Test
    @DisplayName("До сохранения токенов загрузка возвращает null")
    fun `load returns null when file is absent`(@TempDir tempDir: Path) {
        assertThat(store(tempDir).load()).isNull()
    }

    @Test
    @DisplayName("Сохранённый набор токенов читается без потери данных")
    fun `save then load round trips token set`(@TempDir tempDir: Path) {
        val tokenStore = store(tempDir)
        val expected = TokenSet(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            tokenType = "OAuth",
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS),
        )

        tokenStore.save(expected)

        assertThat(tokenStore.load()).isEqualTo(expected)
    }

    @Test
    @DisplayName("После очистки токены недоступны")
    fun `clear removes stored tokens`(@TempDir tempDir: Path) {
        val tokenStore = store(tempDir)
        tokenStore.save(
            TokenSet("access", "refresh", "OAuth", Instant.now().plusSeconds(3600)),
        )

        tokenStore.clear()

        assertThat(tokenStore.load()).isNull()
    }
}
