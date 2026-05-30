package com.sorface.mcp.yandex.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("Конфигурация подключения к Яндексу (YandexProperties)")
class YandexPropertiesTest {

    @Test
    @DisplayName("Для организации Яндекс 360 используется заголовок X-Org-ID")
    fun `org header for yandex 360`() {
        val properties = YandexProperties(orgType = OrgType.YANDEX_360)

        assertThat(properties.orgHeaderName()).isEqualTo("X-Org-ID")
    }

    @Test
    @DisplayName("Для организации Yandex Cloud используется заголовок X-Cloud-Org-ID")
    fun `org header for yandex cloud`() {
        val properties = YandexProperties(orgType = OrgType.YANDEX_CLOUD)

        assertThat(properties.orgHeaderName()).isEqualTo("X-Cloud-Org-ID")
    }

    @Test
    @DisplayName("Адрес OAuth по умолчанию указывает на публичный сервис Яндекса")
    fun `default oauth base url points to yandex public api`() {
        assertThat(YandexProperties().oauth.baseUrl).isEqualTo("https://oauth.yandex.com")
    }

    @Test
    @DisplayName("Повторные запросы включены по умолчанию с разумными параметрами")
    fun `retry defaults are sensible`() {
        val retry = YandexProperties().retry

        assertThat(retry.enabled).isTrue()
        assertThat(retry.maxAttempts).isEqualTo(3)
        assertThat(retry.initialDelay).isEqualTo(Duration.ofMillis(500))
        assertThat(retry.multiplier).isEqualTo(2.0)
        assertThat(retry.maxDelay).isEqualTo(Duration.ofSeconds(10))
    }
}
