package com.sorface.mcp.yandex.wiki.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Настройки API Yandex Wiki.
 *
 * Префикс свойств — `yandex.wiki`. Используется только модулем `yandex-mcp-workspace-wiki`.
 *
 * @property baseUrl базовый адрес API Wiki
 *
 * @author Sorface Developer
 */
@ConfigurationProperties(prefix = "yandex.wiki")
data class WikiApiProperties(
    val baseUrl: String = "https://api.wiki.yandex.net",
)
