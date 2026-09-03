package com.sorface.mcp.yandex.client.api

import com.sorface.mcp.yandex.auth.api.AuthSetupPrompter
import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.client.application.ClientConnector
import com.sorface.mcp.yandex.client.application.ClientScope
import com.sorface.mcp.yandex.client.application.ClientTarget
import com.sorface.mcp.yandex.client.application.McpServerCommand
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

/**
 * Интерактивная команда подключения установленных MCP-серверов к локальным агентам.
 *
 * @author Sorface Developer
 */
@Component
@Profile("auth")
class ClientConnectCommandRunner(
    private val connector: ClientConnector,
    private val prompter: AuthSetupPrompter,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (args.nonOptionArgs.firstOrNull()?.lowercase() != "connect") return

        val servers = installedServers(args)
        if (servers.isEmpty()) {
            throw AuthorizationException(
                "Не найдены установленные Tracker или Wiki. Передайте --tracker-command или --wiki-command.",
            )
        }

        val availability = connector.detect()
        System.err.println("Доступные клиенты MCP:")
        availability.forEachIndexed { index, client ->
            val marker = if (client.available) "доступен" else client.reason
            System.err.println("  ${index + 1}) ${client.target.title} — $marker")
        }
        val available = availability.filter { it.available }
        if (available.isEmpty()) {
            System.err.println("Поддерживаемые клиенты не обнаружены. Их можно подключить позже командой connect.")
            return
        }

        val selected = selectTargets(availability.map { it.target }, available.map { it.target })
        if (selected.isEmpty()) {
            System.err.println("Подключение агентов пропущено.")
            return
        }

        val restart = mutableListOf<String>()
        selected.forEach { target ->
            val scope = selectScope(target)
            val result = runCatching {
                connector.connect(target, scope, servers, ::confirmReplacement)
            }.getOrElse { error ->
                System.err.println("${target.title}: ${error.message}")
                return@forEach
            }
            System.err.println("${target.title}: ${result.message}")
            if (result.restartRequired) restart += target.title
        }

        if (restart.isNotEmpty()) {
            System.err.println("Перезапустите: ${restart.joinToString()}.")
        }
    }

    private fun installedServers(args: ApplicationArguments): List<McpServerCommand> = buildList {
        commandOption(args, "tracker-command")?.let { add(McpServerCommand("yandex-tracker", it)) }
        commandOption(args, "wiki-command")?.let { add(McpServerCommand("yandex-wiki", it)) }
        if (isEmpty()) {
            val bin = Path.of(System.getProperty("user.home", "."), ".local", "bin")
            listOf("tracker", "wiki").forEach { component ->
                val command = bin.resolve("yandex-mcp-$component")
                if (Files.isExecutable(command)) add(McpServerCommand("yandex-$component", command))
            }
        }
    }

    private fun commandOption(args: ApplicationArguments, name: String): Path? =
        args.getOptionValues(name)?.lastOrNull()?.takeIf { it.isNotBlank() }?.let(Path::of)

    private fun selectTargets(all: List<ClientTarget>, available: List<ClientTarget>): List<ClientTarget> {
        System.err.println("Укажите номера через запятую; Enter подключит все найденные клиенты, 0 — пропустить.")
        val answer = prompter.read("Клиенты: ").orEmpty().trim()
        if (answer.isBlank()) return available
        if (answer == "0") return emptyList()
        val indexes = answer.split(',').map { token ->
            token.trim().toIntOrNull()?.takeIf { it in 1..all.size }
                ?: throw AuthorizationException("Некорректный номер клиента: $token")
        }
        val selected = indexes.distinct().map { all[it - 1] }
        val unavailable = selected.filterNot { it in available }
        if (unavailable.isNotEmpty()) {
            throw AuthorizationException("Клиенты не обнаружены: ${unavailable.joinToString { it.title }}")
        }
        return selected
    }

    private fun selectScope(target: ClientTarget): ClientScope {
        if (!target.supportsProjectScope) return ClientScope.USER
        val answer = prompter.read(
            "${target.title}: уровень [1 — пользователь, 2 — текущий проект; Enter = 1]: ",
        ).orEmpty().trim()
        return when (answer) {
            "", "1" -> ClientScope.USER
            "2" -> ClientScope.PROJECT
            else -> throw AuthorizationException("Неизвестный уровень конфигурации: $answer")
        }
    }

    private fun confirmReplacement(serverName: String, currentValue: String): Boolean {
        System.err.println("Найдена существующая запись $serverName:")
        System.err.println(currentValue.prependIndent("  "))
        val answer = prompter.read("Заменить только эту запись? [y/N]: ").orEmpty().trim()
        return answer.equals("y", ignoreCase = true) || answer.equals("yes", ignoreCase = true) ||
            answer.equals("д", ignoreCase = true) || answer.equals("да", ignoreCase = true)
    }
}
