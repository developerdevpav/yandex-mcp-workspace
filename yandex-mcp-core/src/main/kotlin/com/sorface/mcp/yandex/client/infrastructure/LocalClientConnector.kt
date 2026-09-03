package com.sorface.mcp.yandex.client.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sorface.mcp.yandex.client.application.ClientAvailability
import com.sorface.mcp.yandex.client.application.ClientConnectionResult
import com.sorface.mcp.yandex.client.application.ClientConnector
import com.sorface.mcp.yandex.client.application.ClientScope
import com.sorface.mcp.yandex.client.application.ClientTarget
import com.sorface.mcp.yandex.client.application.McpServerCommand
import org.springframework.stereotype.Component
import org.tomlj.Toml
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Выполняет локальные процессы, используемые штатными CLI клиентов. */
fun interface LocalProcessExecutor {
    /** Запускает команду в указанном каталоге и возвращает код и объединённый вывод. */
    fun execute(command: List<String>, directory: Path): ProcessResult
}

/** Результат локального процесса. */
data class ProcessResult(val exitCode: Int, val output: String)

/** Системная реализация запуска локального процесса. */
@Component
class SystemLocalProcessExecutor : LocalProcessExecutor {
    override fun execute(command: List<String>, directory: Path): ProcessResult {
        val process = ProcessBuilder(command)
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        return ProcessResult(process.waitFor(), output)
    }
}

/**
 * Локальный адаптер конфигураций Codex, Claude и Cursor.
 *
 * JSON-файлы меняются через Jackson, TOML — заменой только точных секций
 * `mcp_servers.yandex-*`. Перед прямой записью существующего файла создаётся резервная копия.
 *
 * @author Sorface Developer
 */
@Component
class LocalClientConnector(
    private val objectMapper: ObjectMapper,
    private val processExecutor: LocalProcessExecutor,
    private val clock: Clock = Clock.systemUTC(),
    private val home: Path = Path.of(System.getProperty("user.home", ".")),
    private val workingDirectory: Path = Path.of(System.getProperty("user.dir", ".")),
    private val environment: Map<String, String> = System.getenv(),
) : ClientConnector {

    override fun detect(): List<ClientAvailability> = listOf(
        availability(
            ClientTarget.CODEX,
            commandExists("codex") || Files.exists(home.resolve(".codex")) || macApplicationExists("Codex.app"),
            "не найдены команда codex, каталог ~/.codex или приложение Codex",
        ),
        availability(
            ClientTarget.CLAUDE_CODE,
            commandExists("claude"),
            "команда claude не найдена в PATH",
        ),
        availability(
            ClientTarget.CLAUDE_DESKTOP,
            Files.exists(claudeDesktopConfig().parent) || macApplicationExists("Claude.app"),
            "не найдено приложение или каталог конфигурации Claude Desktop",
        ),
        availability(
            ClientTarget.CURSOR,
            commandExists("cursor") || Files.exists(home.resolve(".cursor")) || macApplicationExists("Cursor.app"),
            "не найдены команда cursor, каталог ~/.cursor или приложение Cursor",
        ),
    )

    override fun connect(
        target: ClientTarget,
        scope: ClientScope,
        servers: List<McpServerCommand>,
        confirmReplace: (serverName: String, currentValue: String) -> Boolean,
    ): ClientConnectionResult {
        require(scope != ClientScope.PROJECT || target.supportsProjectScope) {
            "${target.title} не поддерживает конфигурацию на уровне проекта"
        }
        require(servers.isNotEmpty()) { "Не передано ни одного MCP-сервера" }

        return when (target) {
            ClientTarget.CODEX -> connectCodex(scope, servers, confirmReplace)
            ClientTarget.CLAUDE_CODE -> connectClaudeCode(scope, servers, confirmReplace)
            ClientTarget.CLAUDE_DESKTOP -> updateJson(
                target,
                claudeDesktopConfig(),
                servers,
                confirmReplace,
                restartRequired = true,
            )
            ClientTarget.CURSOR -> updateJson(
                target,
                if (scope == ClientScope.USER) home.resolve(".cursor/mcp.json")
                else workingDirectory.resolve(".cursor/mcp.json"),
                servers,
                confirmReplace,
                restartRequired = true,
            )
        }
    }

    private fun connectCodex(
        scope: ClientScope,
        servers: List<McpServerCommand>,
        confirmReplace: (String, String) -> Boolean,
    ): ClientConnectionResult {
        if (scope == ClientScope.USER && commandExists("codex")) {
            return connectUsingCli(
                target = ClientTarget.CODEX,
                servers = servers,
                inspect = { listOf("codex", "mcp", "get", it) },
                remove = { listOf("codex", "mcp", "remove", it) },
                add = { listOf("codex", "mcp", "add", it.name, "--", it.command.toString()) },
                confirmReplace = confirmReplace,
            )
        }
        val path = if (scope == ClientScope.USER) home.resolve(".codex/config.toml")
        else workingDirectory.resolve(".codex/config.toml")
        return updateCodexToml(path, servers, confirmReplace)
    }

    private fun connectClaudeCode(
        scope: ClientScope,
        servers: List<McpServerCommand>,
        confirmReplace: (String, String) -> Boolean,
    ): ClientConnectionResult = connectUsingCli(
        target = ClientTarget.CLAUDE_CODE,
        servers = servers,
        inspect = { listOf("claude", "mcp", "get", it) },
        remove = { listOf("claude", "mcp", "remove", it, "--scope", scope.name.lowercase()) },
        add = {
            listOf(
                "claude", "mcp", "add", "--scope", scope.name.lowercase(),
                "--transport", "stdio", it.name, "--", it.command.toString(),
            )
        },
        confirmReplace = confirmReplace,
    )

    private fun connectUsingCli(
        target: ClientTarget,
        servers: List<McpServerCommand>,
        inspect: (String) -> List<String>,
        remove: (String) -> List<String>,
        add: (McpServerCommand) -> List<String>,
        confirmReplace: (String, String) -> Boolean,
    ): ClientConnectionResult {
        var changed = false
        servers.forEach { server ->
            val current = processExecutor.execute(inspect(server.name), workingDirectory)
            if (current.exitCode == 0) {
                if (!confirmReplace(server.name, current.output.ifBlank { "существующая запись" })) return@forEach
                val removed = processExecutor.execute(remove(server.name), workingDirectory)
                check(removed.exitCode == 0) {
                    "Не удалось удалить прежнюю запись ${server.name}: ${removed.output}"
                }
            }
            val added = processExecutor.execute(add(server), workingDirectory)
            check(added.exitCode == 0) { "Не удалось добавить ${server.name}: ${added.output}" }
            val verification = processExecutor.execute(inspect(server.name), workingDirectory)
            check(verification.exitCode == 0) {
                "${server.name} добавлен, но клиент не подтвердил конфигурацию: ${verification.output}"
            }
            changed = true
        }
        return ClientConnectionResult(
            target,
            changed,
            if (changed) "MCP-серверы подключены штатной командой клиента" else "Изменения пропущены",
        )
    }

    private fun updateJson(
        target: ClientTarget,
        path: Path,
        servers: List<McpServerCommand>,
        confirmReplace: (String, String) -> Boolean,
        restartRequired: Boolean,
    ): ClientConnectionResult {
        val root = if (Files.exists(path)) {
            val parsed = runCatching { objectMapper.readTree(path.toFile()) }
                .getOrElse { throw IllegalStateException("Некорректный JSON в $path: ${it.message}") }
            parsed as? ObjectNode ?: throw IllegalStateException("Корень JSON в $path должен быть объектом")
        } else {
            objectMapper.createObjectNode()
        }
        val existingServers = root.get("mcpServers")
        if (existingServers != null && existingServers !is ObjectNode) {
            throw IllegalStateException("Поле mcpServers в $path должно быть объектом")
        }
        val mcpServers = existingServers as? ObjectNode ?: root.putObject("mcpServers")
        var changed = false
        servers.forEach { server ->
            val existing = mcpServers.get(server.name)
            if (existing != null && !confirmReplace(server.name, existing.toString())) return@forEach
            mcpServers.putObject(server.name).put("command", server.command.toString())
            changed = true
        }
        if (changed) writeJsonAtomically(path, root)
        return ClientConnectionResult(
            target,
            changed,
            if (changed) "Конфигурация обновлена: $path" else "Изменения пропущены",
            restartRequired && changed,
        )
    }

    private fun updateCodexToml(
        path: Path,
        servers: List<McpServerCommand>,
        confirmReplace: (String, String) -> Boolean,
    ): ClientConnectionResult {
        var content = if (Files.exists(path)) Files.readString(path) else ""
        validateToml(path, content)
        var changed = false
        servers.forEach { server ->
            val header = "[mcp_servers.${server.name}]"
            val ranges = tomlSectionRanges(content, header)
            check(ranges.size <= 1) { "В $path найдено несколько секций $header; файл не изменён" }
            if (ranges.isNotEmpty()) {
                val current = content.substring(ranges.single().first, ranges.single().last + 1).trim()
                if (!confirmReplace(server.name, current)) return@forEach
                val range = ranges.single()
                content = content.removeRange(range.first, range.last + 1).trimEnd()
            }
            val section = buildString {
                appendLine(header)
                appendLine("command = \"${escapeToml(server.command.toString())}\"")
                appendLine("startup_timeout_sec = 30")
                append("tool_timeout_sec = 120")
            }
            content = listOf(content.trimEnd(), section).filter { it.isNotBlank() }.joinToString("\n\n") + "\n"
            changed = true
        }
        validateToml(path, content)
        if (changed) writeTextAtomically(path, content)
        return ClientConnectionResult(
            ClientTarget.CODEX,
            changed,
            if (changed) "Конфигурация обновлена: $path" else "Изменения пропущены",
            restartRequired = changed,
        )
    }

    private fun validateToml(path: Path, content: String) {
        if (content.isBlank()) return
        val parsed = Toml.parse(content)
        check(!parsed.hasErrors()) {
            "Некорректный TOML в $path: ${parsed.errors().joinToString { it.toString() }}"
        }
    }

    private fun tomlSectionRanges(content: String, header: String): List<IntRange> {
        val lines = content.splitToSequence('\n').toList()
        val starts = mutableListOf<Int>()
        var offset = 0
        lines.forEach { line ->
            if (line.trim() == header) starts += offset
            offset += line.length + 1
        }
        return starts.map { start ->
            val nextHeader = Regex("(?m)^\\s*\\[").find(content, start + header.length)?.range?.first
            start until (nextHeader ?: content.length)
        }
    }

    private fun writeJsonAtomically(path: Path, root: ObjectNode) {
        val text = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n"
        writeTextAtomically(path, text)
    }

    private fun writeTextAtomically(path: Path, content: String) {
        Files.createDirectories(requireNotNull(path.toAbsolutePath().parent))
        if (Files.exists(path)) Files.copy(path, backupPath(path), StandardCopyOption.COPY_ATTRIBUTES)
        val temporary = Files.createTempFile(path.toAbsolutePath().parent, ".${path.fileName}-", ".tmp")
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8)
            runCatching {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }.getOrElse {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun backupPath(path: Path): Path {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC)
            .format(clock.instant())
        return path.resolveSibling("${path.fileName}.backup-$timestamp")
    }

    private fun availability(target: ClientTarget, available: Boolean, unavailableReason: String) =
        ClientAvailability(target, available, if (available) "обнаружен" else unavailableReason)

    private fun commandExists(command: String): Boolean {
        val path = environment["PATH"].orEmpty()
        return path.split(System.getProperty("path.separator"))
            .filter { it.isNotBlank() }
            .any { Files.isExecutable(Path.of(it).resolve(command)) }
    }

    private fun macApplicationExists(application: String): Boolean {
        if (!System.getProperty("os.name", "").lowercase(Locale.ROOT).contains("mac")) return false
        return Files.exists(Path.of("/Applications").resolve(application)) ||
            Files.exists(home.resolve("Applications").resolve(application))
    }

    private fun claudeDesktopConfig(): Path =
        if (System.getProperty("os.name", "").lowercase(Locale.ROOT).contains("mac")) {
            home.resolve("Library/Application Support/Claude/claude_desktop_config.json")
        } else {
            home.resolve(".config/Claude/claude_desktop_config.json")
        }

    private fun escapeToml(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}
