# Где взять ключи и идентификаторы

Перед первым запуском MCP-серверов нужны три значения из интерфейсов Яндекса. OAuth-токен пользователя получается отдельно — через команду `auth` (см. [setup.md](./setup.md#первичная-авторизация)).

## Что понадобится

| Переменная | Где взять | Пример |
|---|---|---|
| `YANDEX_CLIENT_ID` | [oauth.yandex.ru](https://oauth.yandex.ru/) → приложение → **ClientID** | `a1b2c3d4e5f6g7h8i9j0` |
| `YANDEX_CLIENT_SECRET` | [oauth.yandex.ru](https://oauth.yandex.ru/) → приложение → **Client secret** (Пароль) | `0123456789abcdef0123456789abcdef` |
| `YANDEX_ORG_ID` | [Трекер](https://tracker.yandex.ru/) → **Администрирование → Организации** → поле **Идентификатор** | `12345678` |
| `YANDEX_ORG_TYPE` | Зависит от типа организации (см. ниже) | `YANDEX_360` или `YANDEX_CLOUD` |

OAuth-токен (`access_token`, `refresh_token`) **не копируется вручную** из браузера — сервер получает его по сценарию Device Flow и сохраняет в томе `yandex-mcp-tokens` (`/data/tokens.json`).

## 1. Создать OAuth-приложение

1. Откройте [oauth.yandex.ru](https://oauth.yandex.ru/) под аккаунтом, от имени которого будет работать агент.
2. На странице **Ваши приложения** нажмите **Создать**.
3. Выберите **Для доступа к API или отладки** → **Перейти к созданию**.
4. Укажите название и почту для связи.
5. Добавьте разрешения в поле **Название доступа** (начните вводить название):

   | Сервис | Только чтение | Чтение и запись |
   |---|---|---|
   | Tracker | `tracker:read` | `tracker:write` |
   | Wiki | `wiki:read` | `wiki:write` |

   Для обоих MCP-серверов удобно создать **одно** приложение с нужными правами сразу (например, `tracker:write` и `wiki:write`).

6. Нажмите **Создать приложение**.
7. Откройте созданное приложение и скопируйте:
   - **ClientID** → `YANDEX_CLIENT_ID`
   - **Client secret** (Пароль) → `YANDEX_CLIENT_SECRET`

Официальная инструкция:

- Tracker: [Доступ к API](https://yandex.ru/support/tracker/ru/api-ref/access)
- Wiki: [Доступ к API](https://yandex.ru/support/wiki/ru/api-ref/access)

> **Важно:** не публикуйте `client_secret` и не коммитьте его в репозиторий. Храните только в переменных окружения или локальном `mcp.json`.

## 2. Узнать ID организации

Идентификатор организации один и тот же для Tracker и Wiki в рамках одной компании.

1. Откройте [tracker.yandex.ru](https://tracker.yandex.ru/).
2. Перейдите **Администрирование → Организации**.
3. Скопируйте значение поля **Идентификатор** → `YANDEX_ORG_ID`.

Прямая ссылка (если есть доступ): [Администрирование → Организации](https://tracker.yandex.ru/admin/orgs).

## 3. Выбрать тип организации

| Ваша организация | `YANDEX_ORG_TYPE` | HTTP-заголовок в запросах к API |
|---|---|---|
| Яндекс 360 для бизнеса | `YANDEX_360` (значение по умолчанию) | `X-Org-ID` |
| Yandex Cloud Organization | `YANDEX_CLOUD` | `X-Cloud-Org-ID` |

Если не уверены: большинство корпоративных аккаунтов на Яндекс 360 — используйте `YANDEX_360`. Cloud-организации обычно работают через [console.cloud.yandex.ru](https://console.cloud.yandex.ru/).

## 4. OAuth-токен (авторизация пользователя)

MCP-сервер использует **OAuth 2.0 Device Flow**, а не ручное копирование токена из браузера.

После того как заданы `YANDEX_CLIENT_ID`, `YANDEX_CLIENT_SECRET` и `YANDEX_ORG_ID`, выполните один раз:

```bash
docker run -it --rm \
  -e YANDEX_CLIENT_ID=<client_id> \
  -e YANDEX_CLIENT_SECRET=<client_secret> \
  -e YANDEX_ORG_ID=<org_id> \
  -e YANDEX_ORG_TYPE=YANDEX_360 \
  -v yandex-mcp-tokens:/data \
  ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest auth
```

Дальнейшие шаги:

1. Откройте ссылку из терминала (обычно https://ya.ru/device).
2. Введите код подтверждения.
3. Разрешите доступ приложению.

Токены сохраняются в Docker-томе `yandex-mcp-tokens`. Проверка:

```bash
docker run --rm -v yandex-mcp-tokens:/data alpine cat /data/tokens.json
```

Подробнее — [setup.md](./setup.md#первичная-авторизация).

## 5. Дополнительные параметры (обычно не нужны)

| Переменная | Когда менять |
|---|---|
| `YANDEX_OAUTH_SCOPES` | Если нужно явно передать scopes в Device Flow (через пробел). По умолчанию scopes берутся из настроек приложения на oauth.yandex.ru |
| `YANDEX_READ_ONLY=true` | Запретить инструменты записи |
| `YANDEX_TOKEN_STORE_PATH` | Другой путь к файлу токенов (по умолчанию `/data/tokens.json`) |

Полный список — [configuration.md](./configuration.md).

## Чеклист перед подключением

- [ ] OAuth-приложение создано на [oauth.yandex.ru](https://oauth.yandex.ru/)
- [ ] Добавлены scopes для Tracker и/или Wiki
- [ ] Скопированы `ClientID` и `Client secret`
- [ ] Скопирован ID организации из **Администрирование → Организации** в Трекере
- [ ] Выбран корректный `YANDEX_ORG_TYPE`
- [ ] Выполнена команда `auth` с томом `yandex-mcp-tokens`
- [ ] В `mcp.json` те же `CLIENT_ID`/`SECRET`, что использовались при `auth`

## Частые проблемы

| Симптом | Что проверить |
|---|---|
| `OAuth: invalid_client` | `YANDEX_CLIENT_ID` и `YANDEX_CLIENT_SECRET` из карточки приложения |
| `401 Unauthorized` | Повторите `auth`; scopes приложения; права пользователя в Tracker/Wiki |
| Ошибка доступа при корректном токене | `YANDEX_ORG_ID` и `YANDEX_ORG_TYPE` |
| Wiki недоступна через API | Администратор мог отключить доступ к API в настройках организации |

Подробнее — [troubleshooting.md](./troubleshooting.md).
