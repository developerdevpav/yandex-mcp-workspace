package com.sorface.mcp.yandex.tracker.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Настройки API Yandex Tracker.
 *
 * Префикс свойств — `yandex.tracker`. Используется только модулем `yandex-mcp-workspace-tracker`.
 *
 * @property baseUrl базовый адрес API Tracker
 *
 * @author Sorface Developer
 */
@ConfigurationProperties(prefix = "yandex.tracker")
data class TrackerApiProperties(
    val baseUrl: String = "https://api.tracker.yandex.net",
)
