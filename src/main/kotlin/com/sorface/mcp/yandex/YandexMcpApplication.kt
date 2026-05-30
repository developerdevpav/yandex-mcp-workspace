package com.sorface.mcp.yandex

import com.sorface.mcp.yandex.config.YandexProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Точка входа MCP-сервера для Yandex Tracker и Yandex Wiki.
 *
 * Поддерживаются два режима, выбираемые первым аргументом командной строки:
 * - `serve` (по умолчанию) — запуск MCP-сервера по транспорту stdio. Обмен сообщениями MCP
 *   идёт через стандартный ввод-вывод, поэтому в stdout нельзя писать ничего, кроме
 *   протокольных сообщений (вся диагностика направляется в stderr, см. logback-spring.xml).
 * - `auth` — интерактивное получение токена по сценарию OAuth 2.0 Device Flow. В этом режиме
 *   активируется профиль `auth`, MCP-сервер не стартует, выполняется только команда авторизации.
 *
 * @author Sorface Developer
 */
@SpringBootApplication
@EnableConfigurationProperties(YandexProperties::class)
class YandexMcpApplication

fun main(args: Array<String>) {
    val builder = SpringApplicationBuilder(YandexMcpApplication::class.java)
    if (args.firstOrNull() == "auth") {
        builder.profiles("auth")
    }
    builder.run(*args)
}
