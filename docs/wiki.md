# Yandex Wiki

MCP-сервер модуля `yandex-mcp-workspace-wiki`. Образ: `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest`.

## API

Базовый URL по умолчанию: `https://api.wiki.yandex.net` (переопределяется `YANDEX_WIKI_BASE_URL`).

В каждый запрос добавляются:

- `Authorization: OAuth <токен>`;
- `X-Org-ID` или `X-Cloud-Org-ID` в зависимости от `YANDEX_ORG_TYPE`.

## Инструменты

Полный список с HTTP endpoint — [capabilities/yandex-wiki-capabilities.md](./capabilities/yandex-wiki-capabilities.md).

Кратко: страницы (чтение и запись, clone, append Markdown), комментарии, вложения, динамические таблицы (`wiki_grid_*`).

## Особенности

- Содержимое страниц — **Markdown** в параметре `content`.
- Операции с таблицами: тело запроса — JSON-строка в параметре `body`.
- `wiki_page_delete` возвращает токен восстановления; `wiki_page_recover` принимает этот токен.

## Read-only

При `YANDEX_READ_ONLY=true` инструменты `WikiWriteTools` не регистрируются. `WikiGridTools` остаются в `tools/list`, но запись блокируется `WriteGuard` до обращения к API.

## Конфигурация

Только общие переменные OAuth/org/retry плюс `YANDEX_WIKI_BASE_URL`. Переменные Tracker не используются. См. [configuration.md](./configuration.md).
