package com.sorface.mcp.yandex.client.application

import java.nio.file.Path

/**
 * Идентификатор поддерживаемого локального клиента MCP.
 *
 * @author Sorface Developer
 */
enum class ClientTarget(
    /** Стабильный идентификатор, используемый в CLI. */
    val id: String,
    /** Название, отображаемое пользователю. */
    val title: String,
    /** Поддерживает ли клиент конфигурацию на уровне проекта. */
    val supportsProjectScope: Boolean,
) {
    CODEX("codex", "Codex CLI/Desktop", true),
    CLAUDE_CODE("claude-code", "Claude Code", true),
    CLAUDE_DESKTOP("claude-desktop", "Claude Desktop", false),
    CURSOR("cursor", "Cursor", true),
}

/** Уровень размещения конфигурации MCP. */
enum class ClientScope {
    /** Конфигурация текущего пользователя, доступная во всех проектах. */
    USER,

    /** Конфигурация в текущем рабочем каталоге. */
    PROJECT,
}

/** Результат обнаружения одного клиента. */
data class ClientAvailability(
    val target: ClientTarget,
    val available: Boolean,
    val reason: String,
)

/** Команда запуска одного устанавливаемого MCP-сервера. */
data class McpServerCommand(
    val name: String,
    val command: Path,
)

/** Результат подключения выбранного клиента. */
data class ClientConnectionResult(
    val target: ClientTarget,
    val changed: Boolean,
    val message: String,
    val restartRequired: Boolean = false,
)

/**
 * Подключает локальные Yandex MCP-серверы к поддерживаемым агентам.
 *
 * @author Sorface Developer
 */
interface ClientConnector {

    /** Возвращает доступность всех поддерживаемых клиентов. */
    fun detect(): List<ClientAvailability>

    /**
     * Записывает выбранные MCP-серверы в конфигурацию клиента.
     *
     * @param target клиент, который нужно настроить
     * @param scope уровень конфигурации
     * @param servers команды установленных MCP-серверов
     * @param confirmReplace подтверждение замены существующей записи с её безопасным описанием
     */
    fun connect(
        target: ClientTarget,
        scope: ClientScope,
        servers: List<McpServerCommand>,
        confirmReplace: (serverName: String, currentValue: String) -> Boolean,
    ): ClientConnectionResult
}
