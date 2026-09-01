# Примеры MCP-конфигурации

- `claude.mcp.json` — содержимое Claude Code `.mcp.json` или Claude Desktop `claude_desktop_config.json`.
- `cursor.mcp.json` — содержимое глобального или проектного Cursor `mcp.json`.
- `codex.config.toml` — секции для `~/.codex/config.toml` или доверенного `.codex/config.toml`.

Tracker и Wiki устанавливаются из отдельных архивов. Можно подключить только один сервер или оба. Перед использованием замените пути на абсолютные пути к исполняемым файлам из соответствующих распакованных архивов. Авторизация выполняется один раз для всех установленных серверов, например в Linux:

```bash
/absolute/path/yandex-mcp-tracker/app/bin/yandex-mcp-tracker setup
```

Удалите из примера секцию сервера, который не установлен.

Полная инструкция: [docs/mcp-clients.md](../../docs/mcp-clients.md).
