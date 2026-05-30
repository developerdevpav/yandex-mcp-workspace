package com.sorface.mcp.yandex.wiki

import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.wiki.config.WikiApiProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Точка входа MCP-сервера для Yandex Wiki.
 *
 * @author Sorface Developer
 */
@SpringBootApplication(scanBasePackages = ["com.sorface.mcp.yandex"])
@EnableConfigurationProperties(YandexProperties::class, WikiApiProperties::class)
class YandexWikiMcpApplication

fun main(args: Array<String>) {
    val builder = SpringApplicationBuilder(YandexWikiMcpApplication::class.java)
    if (args.firstOrNull() == "auth") {
        builder.profiles("auth")
    }
    builder.run(*args)
}
