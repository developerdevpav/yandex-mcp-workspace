package com.sorface.mcp.yandex.tracker

import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.tracker.config.TrackerApiProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Точка входа MCP-сервера для Yandex Tracker.
 *
 * @author Sorface Developer
 */
@SpringBootApplication(scanBasePackages = ["com.sorface.mcp.yandex"])
@EnableConfigurationProperties(YandexProperties::class, TrackerApiProperties::class)
class YandexTrackerMcpApplication

fun main(args: Array<String>) {
    val builder = SpringApplicationBuilder(YandexTrackerMcpApplication::class.java)
    if (args.firstOrNull() == "auth") {
        builder.profiles("auth")
    }
    builder.run(*args)
}
