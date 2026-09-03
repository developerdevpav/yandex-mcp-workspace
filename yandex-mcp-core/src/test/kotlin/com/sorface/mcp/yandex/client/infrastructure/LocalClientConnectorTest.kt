package com.sorface.mcp.yandex.client.infrastructure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sorface.mcp.yandex.client.application.ClientScope
import com.sorface.mcp.yandex.client.application.ClientTarget
import com.sorface.mcp.yandex.client.application.McpServerCommand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList

@DisplayName("Подключение локальных MCP-клиентов")
class LocalClientConnectorTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    @DisplayName("Cursor сохраняет посторонние серверы и создаёт резервную копию")
    fun `cursor preserves unrelated servers and creates backup`() {
        val config = temporaryDirectory.resolve(".cursor/mcp.json")
        Files.createDirectories(config.parent)
        Files.writeString(
            config,
            """{"mcpServers":{"other":{"command":"other"},"yandex-tracker":{"command":"old"}}}""",
        )
        val connector = connector()

        val result = connector.connect(
            ClientTarget.CURSOR,
            ClientScope.USER,
            listOf(McpServerCommand("yandex-tracker", Path.of("/stable/yandex-mcp-tracker"))),
        ) { _, current ->
            assertThat(current).contains("old")
            true
        }

        val root = jacksonObjectMapper().readTree(config.toFile())
        assertThat(root.at("/mcpServers/other/command").asText()).isEqualTo("other")
        assertThat(root.at("/mcpServers/yandex-tracker/command").asText())
            .isEqualTo("/stable/yandex-mcp-tracker")
        assertThat(result.restartRequired).isTrue()
        assertThat(Files.list(config.parent).use { files ->
            files.anyMatch { it.fileName.toString().startsWith("mcp.json.backup-") }
        }).isTrue()
    }

    @Test
    @DisplayName("Codex заменяет только выбранную TOML-секцию")
    fun `codex replaces only selected toml section`() {
        val config = temporaryDirectory.resolve("project/.codex/config.toml")
        Files.createDirectories(config.parent)
        Files.writeString(
            config,
            """
            model = "gpt-test"

            [mcp_servers.yandex-tracker]
            command = "/old"

            [mcp_servers.other]
            command = "/other"
            """.trimIndent() + "\n",
        )
        val connector = connector(workingDirectory = temporaryDirectory.resolve("project"))

        connector.connect(
            ClientTarget.CODEX,
            ClientScope.PROJECT,
            listOf(McpServerCommand("yandex-tracker", Path.of("/stable/tracker"))),
        ) { _, _ -> true }

        val result = Files.readString(config)
        assertThat(result).contains("model = \"gpt-test\"")
        assertThat(result).contains("[mcp_servers.other]\ncommand = \"/other\"")
        assertThat(result).contains("[mcp_servers.yandex-tracker]\ncommand = \"/stable/tracker\"")
        assertThat(result).doesNotContain("command = \"/old\"")
    }

    @Test
    @DisplayName("Некорректный JSON не изменяется")
    fun `invalid json is not modified`() {
        val config = temporaryDirectory.resolve(".cursor/mcp.json")
        Files.createDirectories(config.parent)
        Files.writeString(config, "{not-json")
        val connector = connector()

        assertThatThrownBy {
            connector.connect(
                ClientTarget.CURSOR,
                ClientScope.USER,
                listOf(McpServerCommand("yandex-wiki", Path.of("/stable/wiki"))),
            ) { _, _ -> true }
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Некорректный JSON")

        assertThat(Files.readString(config)).isEqualTo("{not-json")
        assertThat(Files.list(config.parent).use { files ->
            files.noneMatch { it.fileName.toString().contains("backup") }
        }).isTrue()
    }

    @Test
    @DisplayName("Некорректный TOML не изменяется")
    fun `invalid toml is not modified`() {
        val config = temporaryDirectory.resolve("project/.codex/config.toml")
        Files.createDirectories(config.parent)
        val original = "model = [\"unterminated\"\n"
        Files.writeString(config, original)
        val connector = connector(workingDirectory = temporaryDirectory.resolve("project"))

        assertThatThrownBy {
            connector.connect(
                ClientTarget.CODEX,
                ClientScope.PROJECT,
                listOf(McpServerCommand("yandex-wiki", Path.of("/stable/wiki"))),
            ) { _, _ -> true }
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Некорректный TOML")

        assertThat(Files.readString(config)).isEqualTo(original)
    }

    @Test
    @DisplayName("Отклонённый конфликт оставляет конфигурацию без изменений")
    fun `declined conflict leaves config unchanged`() {
        val config = temporaryDirectory.resolve(".cursor/mcp.json")
        Files.createDirectories(config.parent)
        val original = """{"mcpServers":{"yandex-wiki":{"command":"old"}}}"""
        Files.writeString(config, original)
        val connector = connector()

        val result = connector.connect(
            ClientTarget.CURSOR,
            ClientScope.USER,
            listOf(McpServerCommand("yandex-wiki", Path.of("/stable/wiki"))),
        ) { _, _ -> false }

        assertThat(result.changed).isFalse()
        assertThat(Files.readString(config)).isEqualTo(original)
    }

    @Test
    @DisplayName("Codex пользовательского уровня подключается и проверяется штатным CLI")
    fun `codex user scope uses cli and verifies result`() {
        val bin = temporaryDirectory.resolve("bin")
        Files.createDirectories(bin)
        val codex = bin.resolve("codex")
        Files.writeString(codex, "#!/bin/sh\n")
        codex.toFile().setExecutable(true)
        val commands = CopyOnWriteArrayList<List<String>>()
        var invocation = 0
        val connector = LocalClientConnector(
            objectMapper = jacksonObjectMapper(),
            processExecutor = LocalProcessExecutor { command, _ ->
                commands += command
                invocation += 1
                when (invocation) {
                    1 -> ProcessResult(1, "not found")
                    else -> ProcessResult(0, "configured")
                }
            },
            home = temporaryDirectory,
            workingDirectory = temporaryDirectory,
            environment = mapOf("PATH" to bin.toString()),
        )

        connector.connect(
            ClientTarget.CODEX,
            ClientScope.USER,
            listOf(McpServerCommand("yandex-tracker", Path.of("/stable/tracker"))),
        ) { _, _ -> true }

        assertThat(commands).containsExactly(
            listOf("codex", "mcp", "get", "yandex-tracker"),
            listOf("codex", "mcp", "add", "yandex-tracker", "--", "/stable/tracker"),
            listOf("codex", "mcp", "get", "yandex-tracker"),
        )
    }

    private fun connector(
        workingDirectory: Path = temporaryDirectory.resolve("project"),
    ) = LocalClientConnector(
        objectMapper = jacksonObjectMapper(),
        processExecutor = LocalProcessExecutor { _, _ -> ProcessResult(1, "not found") },
        clock = Clock.fixed(Instant.parse("2026-09-03T10:15:30.123Z"), ZoneOffset.UTC),
        home = temporaryDirectory,
        workingDirectory = workingDirectory,
        environment = emptyMap(),
    )
}
