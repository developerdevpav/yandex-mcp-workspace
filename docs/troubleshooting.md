# Troubleshooting

## `no matching manifest for linux/arm64`

**Симптом:** Docker не может скачать образ на Mac с Apple Silicon.

**Причина:** образ в GHCR собран только под `amd64` (старый релиз).

**Решение:**

1. Обновите образ после релиза с multi-arch (`linux/amd64`, `linux/arm64`), или
2. Соберите локально: `docker build --build-arg MCP_MODULE=yandex-mcp-workspace-tracker -t yandex-mcp-workspace-tracker:local .`, или
3. Временно: `docker run --platform linux/amd64 ...` (медленнее из-за эмуляции).

## Таймаут MCP-клиента при старте

**Симптом:** Cursor/Codex: timeout creating MCP client (~30 с).

**Возможные причины:**

- образ не скачивается (см. arm64);
- Docker Desktop не запущен;
- долгий cold start JVM в контейнере.

**Решение:** заранее `docker pull` нужного образа; проверьте `docker run ... serve` вручную; укажите полный путь к `docker` в `mcp.json`.

## `OAuth: invalid_client` / `Client not found`

**Симптом:** команда `auth` завершается ошибкой OAuth.

**Решение:**

- проверьте `YANDEX_CLIENT_ID` и `YANDEX_CLIENT_SECRET` из https://oauth.yandex.ru/;
- убедитесь, что переменные переданы в контейнер (`docker run -e ...`);
- пересоберите образ после обновления кода, если видите устаревший stack trace вместо текста ошибки OAuth.

## `401` / «не авторизован» в инструментах

**Симптом:** `yandex_auth_status` показывает отсутствие токена.

**Решение:**

1. Выполните [авторизацию](./setup.md#первичная-авторизация) с тем же томом `-v yandex-mcp-tokens:/data`, что в `mcp.json`.
2. Проверьте `tokens.json` в томе.
3. Убедитесь, что `YANDEX_CLIENT_ID`/`SECRET` в `serve` совпадают с теми, что использовались при `auth`.

## Неверная организация

**Симптом:** API отвечает ошибкой доступа при корректном токене.

**Решение:** проверьте `YANDEX_ORG_ID` и `YANDEX_ORG_TYPE` (`YANDEX_360` → `X-Org-ID`, `YANDEX_CLOUD` → `X-Cloud-Org-ID`).

## Старые имена образов

После переименования репозитория в `yandex-mcp-workspace` актуальные образы:

- `ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest`
- `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest`

Устаревшие имена (`yandex-mcp-server`, модули `yandex-mcp-tracker` / `yandex-mcp-wiki` без префикса `workspace`) в новых релизах не используются — обновите `mcp.json` и Codex config.
