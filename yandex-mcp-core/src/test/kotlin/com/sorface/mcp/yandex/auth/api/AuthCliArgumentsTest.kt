package com.sorface.mcp.yandex.auth.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Короткие параметры CLI авторизации")
class AuthCliArgumentsTest {

    @Test
    @DisplayName("Короткие имена преобразуются в свойства Spring Boot")
    fun `normalizes short options`() {
        val result = normalizeYandexCliArguments(
            arrayOf("setup", "--client-id=id", "--client-secret=secret", "--org-id=42"),
        )

        assertThat(result).containsExactly(
            "setup",
            "--yandex.client-id=id",
            "--yandex.client-secret=secret",
            "--yandex.org-id=42",
        )
    }

    @Test
    @DisplayName("Неизвестные и полные параметры не изменяются")
    fun `preserves other options`() {
        val result = normalizeYandexCliArguments(arrayOf("doctor", "--yandex.read-only=true", "--debug"))

        assertThat(result).containsExactly("doctor", "--yandex.read-only=true", "--debug")
    }
}
