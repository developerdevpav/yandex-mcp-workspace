package com.sorface.mcp.yandex.common

import com.sorface.mcp.yandex.config.YandexProperties
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Страж изменяющих операций (WriteGuard)")
class WriteGuardTest {

    @Test
    @DisplayName("В обычном режиме изменяющая операция разрешена")
    fun `allows write when not read-only`() {
        val guard = WriteGuard(YandexProperties(readOnly = false))

        assertThatCode { guard.ensureWritable("создание задачи") }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("В режиме только для чтения операция отклоняется с понятным сообщением")
    fun `blocks write in read-only`() {
        val guard = WriteGuard(YandexProperties(readOnly = true))

        assertThatThrownBy { guard.ensureWritable("создание задачи") }
            .isInstanceOf(ReadOnlyModeException::class.java)
            .hasMessageContaining("создание задачи")
            .hasMessageContaining("только для чтения")
    }
}
