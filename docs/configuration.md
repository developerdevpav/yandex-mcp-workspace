# Конфигурация

Команда `setup` сохраняет основные параметры в `~/.config/yandex-mcp/config.properties`; оба сервера импортируют этот файл автоматически. Переменные окружения и аргументы Spring имеют больший приоритет и подходят для Docker/CI. Префикс Spring — `yandex`.

## Общие переменные (оба сервера)

Используются в `yandex-mcp-workspace-tracker` и `yandex-mcp-workspace-wiki` (модуль `yandex-mcp-core`).

| Переменная | Свойство | По умолчанию | Назначение |
|---|---|---|---|
| `YANDEX_CLIENT_ID` | `yandex.client-id` | — | Client ID OAuth-приложения |
| `YANDEX_CLIENT_SECRET` | `yandex.client-secret` | — | Client secret |
| `YANDEX_ORG_ID` | `yandex.org-id` | — | Идентификатор организации |
| `YANDEX_ORG_TYPE` | `yandex.org-type` | `YANDEX_360` | `YANDEX_360` или `YANDEX_CLOUD` |
| `YANDEX_CONFIG_PATH` | `spring.config.import` | `~/.config/yandex-mcp/config.properties` | Другой файл локальных настроек |
| `YANDEX_TOKEN_STORE_PATH` | `yandex.token-store-path` | каталог данных ОС; `/data/tokens.json` в Docker | Путь к файлу токенов |
| `YANDEX_READ_ONLY` | `yandex.read-only` | `false` | Режим только чтения |
| `YANDEX_HTTP_CONNECT_TIMEOUT` | `yandex.http.connect-timeout` | `5s` | Тайм-аут подключения к API |
| `YANDEX_HTTP_READ_TIMEOUT` | `yandex.http.read-timeout` | `30s` | Тайм-аут ожидания ответа API |
| `YANDEX_OAUTH_BASE_URL` | `yandex.oauth.base-url` | `https://oauth.yandex.com` | Базовый URL OAuth |
| `YANDEX_OAUTH_SCOPES` | `yandex.oauth.scopes` | пусто | Scopes через пробел |
| `YANDEX_RETRY_ENABLED` | `yandex.retry.enabled` | `true` | Повторы при сбоях |
| `YANDEX_RETRY_MAX_ATTEMPTS` | `yandex.retry.max-attempts` | `3` | Число попыток |
| `YANDEX_RETRY_INITIAL_DELAY` | `yandex.retry.initial-delay` | `500ms` | Начальная задержка |
| `YANDEX_RETRY_MULTIPLIER` | `yandex.retry.multiplier` | `2.0` | Множитель задержки |
| `YANDEX_RETRY_MAX_DELAY` | `yandex.retry.max-delay` | `10s` | Максимальная задержка |

Сетевые ошибки и ответы `5xx` автоматически повторяются только для идемпотентных HTTP-методов. Это предотвращает дублирование задач, страниц и комментариев при неоднозначном сбое после `POST`. Ответ `429` повторяется с учётом `Retry-After`.

Короткие аргументы команды `setup`: `--client-id`, `--client-secret`, `--org-id`, `--org-type`, `--scopes`, `--read-only`. Они нормализуются в свойства `yandex.*` и после успешной авторизации сохраняются локально.

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

## Переменные окружения как альтернативный режим

Достаточно передать общие переменные — без `YANDEX_WIKI_BASE_URL`:

```json
"env": {
  "YANDEX_CLIENT_ID": "<client_id>",
  "YANDEX_CLIENT_SECRET": "<client_secret>",
  "YANDEX_ORG_ID": "<org_id>",
  "YANDEX_ORG_TYPE": "YANDEX_360"
}
```

При рекомендуемом локальном запуске после `setup` этот блок в `mcp.json` не нужен. Для Wiki в Docker используются те же общие переменные; `YANDEX_TRACKER_BASE_URL` не передаётся.

## Read-only

При `YANDEX_READ_ONLY=true`:

- инструменты записи Tracker/Wiki не регистрируются в `tools/list`;
- инструменты таблиц Wiki (`wiki_grid_*`) остаются в списке, но изменения отклоняются до вызова API.

Подробнее — [capabilities/README.md](./capabilities/README.md).
