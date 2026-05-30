# Конфигурация

Параметры задаются переменными окружения (Docker, `mcp.json`) или через `application.yml` модуля. Префикс Spring — `yandex`.

## Общие переменные (оба сервера)

Используются в `yandex-mcp-workspace-tracker` и `yandex-mcp-workspace-wiki` (модуль `yandex-mcp-core`).

| Переменная | Свойство | По умолчанию | Назначение |
|---|---|---|---|
| `YANDEX_CLIENT_ID` | `yandex.client-id` | — | Client ID OAuth-приложения |
| `YANDEX_CLIENT_SECRET` | `yandex.client-secret` | — | Client secret |
| `YANDEX_ORG_ID` | `yandex.org-id` | — | Идентификатор организации |
| `YANDEX_ORG_TYPE` | `yandex.org-type` | `YANDEX_360` | `YANDEX_360` или `YANDEX_CLOUD` |
| `YANDEX_TOKEN_STORE_PATH` | `yandex.token-store-path` | `/data/tokens.json` | Путь к файлу токенов |
| `YANDEX_READ_ONLY` | `yandex.read-only` | `false` | Режим только чтения |
| `YANDEX_OAUTH_BASE_URL` | `yandex.oauth.base-url` | `https://oauth.yandex.com` | Базовый URL OAuth |
| `YANDEX_OAUTH_SCOPES` | `yandex.oauth.scopes` | пусто | Scopes через пробел |
| `YANDEX_RETRY_ENABLED` | `yandex.retry.enabled` | `true` | Повторы при сбоях |
| `YANDEX_RETRY_MAX_ATTEMPTS` | `yandex.retry.max-attempts` | `3` | Число попыток |
| `YANDEX_RETRY_INITIAL_DELAY` | `yandex.retry.initial-delay` | `500ms` | Начальная задержка |
| `YANDEX_RETRY_MULTIPLIER` | `yandex.retry.multiplier` | `2.0` | Множитель задержки |
| `YANDEX_RETRY_MAX_DELAY` | `yandex.retry.max-delay` | `10s` | Максимальная задержка |

## Только Tracker (`yandex-mcp-workspace-tracker`)

| Переменная | Свойство | По умолчанию | Назначение |
|---|---|---|---|
| `YANDEX_TRACKER_BASE_URL` | `yandex.tracker.base-url` | `https://api.tracker.yandex.net` | Базовый URL API Tracker |

В конфигурации Wiki секции `yandex.tracker` **нет** — указывать `YANDEX_TRACKER_BASE_URL` для образа Wiki не нужно.

## Только Wiki (`yandex-mcp-workspace-wiki`)

| Переменная | Свойство | По умолчанию | Назначение |
|---|---|---|---|
| `YANDEX_WIKI_BASE_URL` | `yandex.wiki.base-url` | `https://api.wiki.yandex.net` | Базовый URL API Wiki |

В конфигурации Tracker секции `yandex.wiki` **нет**.

## Пример для Cursor (Tracker)

Достаточно передать общие переменные — без `YANDEX_WIKI_BASE_URL`:

```json
"env": {
  "YANDEX_CLIENT_ID": "<client_id>",
  "YANDEX_CLIENT_SECRET": "<client_secret>",
  "YANDEX_ORG_ID": "<org_id>",
  "YANDEX_ORG_TYPE": "YANDEX_360"
}
```

Для Wiki — те же общие переменные и образ `yandex-mcp-workspace-wiki`; `YANDEX_TRACKER_BASE_URL` не передаётся.

## Read-only

При `YANDEX_READ_ONLY=true`:

- инструменты записи Tracker/Wiki не регистрируются в `tools/list`;
- инструменты таблиц Wiki (`wiki_grid_*`) остаются в списке, но изменения отклоняются до вызова API.

Подробнее — [capabilities/README.md](./capabilities/README.md).
