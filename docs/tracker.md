# Yandex Tracker

MCP-сервер модуля `yandex-mcp-workspace-tracker`. Образ: `ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest`.

## API

Базовый URL по умолчанию: `https://api.tracker.yandex.net` (переопределяется `YANDEX_TRACKER_BASE_URL`).

В каждый запрос добавляются:

- `Authorization: OAuth <токен>`;
- `X-Org-ID` или `X-Cloud-Org-ID` в зависимости от `YANDEX_ORG_TYPE`.

## Инструменты

Полный список с HTTP endpoint — [capabilities/yandex-tracker-capabilities.md](./capabilities/yandex-tracker-capabilities.md).

Кратко: пользователи и справочники, задачи (поиск, создание, изменение, переходы), очереди, комментарии, связи, чек-листы, worklog.

## Особенности

- Поиск: `query` (язык запросов Tracker) или `filter` (JSON) — параметры взаимоисключающие.
- `tracker_issue_create` / `tracker_issue_update` — произвольные поля через JSON `fields`.
- `tracker_issue_update` принимает `version` для защиты от конфликтов записи.
- Worklog: длительность в ISO 8601 (например `PT2H30M`).
- Ответы — JSON API без нормализации полей на стороне MCP.

## Read-only

При `YANDEX_READ_ONLY=true` изменяющие `tracker_*` не попадают в `tools/list`.

## Конфигурация

Только общие переменные OAuth/org/retry плюс `YANDEX_TRACKER_BASE_URL`. См. [configuration.md](./configuration.md).
