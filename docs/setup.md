# Установка и первый запуск

## Требования

- Docker (для готовых образов) или JDK 21 + Maven (для локальной сборки)
- Приложение [Яндекс OAuth](https://oauth.yandex.ru/) с `client_id` и `client_secret`
- Идентификатор организации `YANDEX_ORG_ID`
- Тип организации `YANDEX_ORG_TYPE`:
  - `YANDEX_360` — заголовок `X-Org-ID`
  - `YANDEX_CLOUD` — заголовок `X-Cloud-Org-ID`

## Образы Docker

| Сервер | Образ |
|---|---|
| Tracker | `ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest` |
| Wiki | `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest` |

На Apple Silicon (arm64) используйте образы из релиза с multi-arch (`linux/amd64`, `linux/arm64`) или соберите образ локально — см. [troubleshooting.md](./troubleshooting.md).

## Том с токенами

Токены OAuth сохраняются в файл `/data/tokens.json` внутри контейнера. Подключите именованный том, чтобы не проходить авторизацию при каждом запуске:

```text
yandex-mcp-tokens:/data
```

Оба сервера могут использовать **один том**, если OAuth-приложение и организация совпадают.

## Первичная авторизация

Выполняется **один раз** на машине в интерактивном режиме (`-it`):

```bash
docker run -it --rm \
  -e YANDEX_CLIENT_ID=<client_id> \
  -e YANDEX_CLIENT_SECRET=<client_secret> \
  -e YANDEX_ORG_ID=<org_id> \
  -e YANDEX_ORG_TYPE=YANDEX_360 \
  -v yandex-mcp-tokens:/data \
  ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest auth
```

Для Wiki подойдёт тот же сценарий с образом `yandex-mcp-workspace-wiki` — токен попадёт в тот же файл тома.

Дальше:

1. Откройте ссылку из stderr (обычно https://ya.ru/device).
2. Введите код подтверждения.
3. Разрешите доступ приложению в аккаунте Яндекса.

Проверка файла токенов:

```bash
docker run --rm -v yandex-mcp-tokens:/data alpine cat /data/tokens.json
```

## Запуск MCP-сервера

В Cursor и Codex контейнер стартует с командой `serve` (по умолчанию в образе). Пример вручную:

```bash
docker run -i --rm \
  -e YANDEX_CLIENT_ID=<client_id> \
  -e YANDEX_CLIENT_SECRET=<client_secret> \
  -e YANDEX_ORG_ID=<org_id> \
  -e YANDEX_ORG_TYPE=YANDEX_360 \
  -v yandex-mcp-tokens:/data \
  ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest serve
```

Подключение к агентам — [mcp-clients.md](./mcp-clients.md).

## Проверка после настройки

Попросите агента вызвать:

1. `system_ping` → ожидается `pong`
2. `yandex_auth_status` → `авторизован: да` (при успешном Device Flow)
