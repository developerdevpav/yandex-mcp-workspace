# Обзор

## Что это

**Yandex MCP Workspace** — набор из двух отдельных MCP-серверов. Каждый сервер запускается своим Docker-образом, общается с агентом по **stdio** и вызывает REST API Яндекса от имени авторизованного пользователя.

| Сервер | Maven-модуль | Образ GHCR | Инструменты |
|---|---|---|---|
| Tracker | `yandex-mcp-workspace-tracker` | `ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest` | 39: `tracker_*`, `system_*`, `yandex_auth_status` |
| Wiki | `yandex-mcp-workspace-wiki` | `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest` | 31: `wiki_*`, `system_*`, `yandex_auth_status` |

Имена образов формируются из имени GitHub-репозитория: `{owner}/{repo}-tracker` и `{owner}/{repo}-wiki` (см. [development.md](./development.md)).

## Структура workspace

```
yandex-mcp-workspace/          # корневой pom (packaging pom)
├── yandex-mcp-core/           # OAuth, retry, read-only, общие инструменты
├── yandex-mcp-workspace-tracker/      # Tracker API + MCP-инструменты tracker_*
└── yandex-mcp-workspace-wiki/          # Wiki API + MCP-инструменты wiki_*
```

- **Общее ядро** (`yandex-mcp-core`) — авторизация Device Flow, хранение токенов, повтор запросов при сбоях, `system_*`, `yandex_auth_status`.
- **Tracker** — только настройки `yandex.tracker` и HTTP-клиент к `api.tracker.yandex.net`.
- **Wiki** — только настройки `yandex.wiki` и HTTP-клиент к `api.wiki.yandex.net`.

При запуске Wiki не требуются переменные Tracker (`YANDEX_TRACKER_BASE_URL` и т.п.) и наоборот. Подробнее — [configuration.md](./configuration.md).

## Возможности

- Задачи, очереди, справочники, комментарии, связи, чек-листы, worklog, пользователи и поля Tracker.
- Страницы, комментарии, вложения и динамические таблицы Wiki (контент страниц — Markdown).
- `YANDEX_READ_ONLY=true` — изменяющие инструменты скрыты из `tools/list`; запись в таблицы Wiki дополнительно блокируется на уровне сервиса.
- Автоповтор при сетевых ошибках, `429` и `5xx`.

Полный перечень инструментов с HTTP endpoint — [capabilities/](./capabilities/README.md).

## Стек

Kotlin, Maven, Spring Boot 3, Spring AI MCP (stdio), RestClient.
