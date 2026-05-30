# Подключение MCP-клиентов

## Cursor

Файл `~/.cursor/mcp.json` или `.cursor/mcp.json` в корне проекта. После изменений — перезапуск Cursor или *Reload* в *Settings → Tools & MCP*.

Два независимых сервера (рекомендуется):

```json
{
  "mcpServers": {
    "yandex-tracker": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "-e", "YANDEX_CLIENT_ID",
        "-e", "YANDEX_CLIENT_SECRET",
        "-e", "YANDEX_ORG_ID",
        "-e", "YANDEX_ORG_TYPE",
        "-v", "yandex-mcp-tokens:/data",
        "ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest", "serve"
      ],
      "env": {
        "YANDEX_CLIENT_ID": "<client_id>",
        "YANDEX_CLIENT_SECRET": "<client_secret>",
        "YANDEX_ORG_ID": "<org_id>",
        "YANDEX_ORG_TYPE": "YANDEX_360"
      }
    },
    "yandex-wiki": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "-e", "YANDEX_CLIENT_ID",
        "-e", "YANDEX_CLIENT_SECRET",
        "-e", "YANDEX_ORG_ID",
        "-e", "YANDEX_ORG_TYPE",
        "-v", "yandex-mcp-tokens:/data",
        "ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest", "serve"
      ],
      "env": {
        "YANDEX_CLIENT_ID": "<client_id>",
        "YANDEX_CLIENT_SECRET": "<client_secret>",
        "YANDEX_ORG_ID": "<org_id>",
        "YANDEX_ORG_TYPE": "YANDEX_360"
      }
    }
  }
}
```

Если Docker не в `PATH`:

- macOS: `"command": "/opt/homebrew/bin/docker"`
- Windows: `"command": "docker.exe"`

Перед первым использованием выполните [авторизацию](./setup.md#первичная-авторизация).

## Codex

Файл `~/.codex/config.toml`. Проверка списка серверов: `codex mcp list`.

```toml
[mcp_servers.yandex-tracker]
command = "docker"
args = [
  "run", "-i", "--rm",
  "-e", "YANDEX_CLIENT_ID",
  "-e", "YANDEX_CLIENT_SECRET",
  "-e", "YANDEX_ORG_ID",
  "-e", "YANDEX_ORG_TYPE",
  "-v", "yandex-mcp-tokens:/data",
  "ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest", "serve",
]
env = { YANDEX_CLIENT_ID = "<client_id>", YANDEX_CLIENT_SECRET = "<client_secret>", YANDEX_ORG_ID = "<org_id>", YANDEX_ORG_TYPE = "YANDEX_360" }

[mcp_servers.yandex-wiki]
command = "docker"
args = [
  "run", "-i", "--rm",
  "-e", "YANDEX_CLIENT_ID",
  "-e", "YANDEX_CLIENT_SECRET",
  "-e", "YANDEX_ORG_ID",
  "-e", "YANDEX_ORG_TYPE",
  "-v", "yandex-mcp-tokens:/data",
  "ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest", "serve",
]
env = { YANDEX_CLIENT_ID = "<client_id>", YANDEX_CLIENT_SECRET = "<client_secret>", YANDEX_ORG_ID = "<org_id>", YANDEX_ORG_TYPE = "YANDEX_360" }
```

## Проверка

| Шаг | Ожидание |
|---|---|
| `system_ping` | ответ `pong` |
| `yandex_auth_status` | авторизован, срок действия токена |
| `tracker_myself` (Tracker) | данные пользователя Tracker |
| `wiki_page_get_by_slug` (Wiki) | страница или ошибка API, но не `401` из-за отсутствия токена |

При ошибках — [troubleshooting.md](./troubleshooting.md).
