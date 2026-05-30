# Yandex MCP Server

MCP-сервер для работы с Yandex Tracker и Yandex Wiki. Позволяет ИИ-агенту управлять задачами, очередями, досками Tracker и страницами, таблицами Wiki через набор инструментов (tools) по протоколу MCP.

## Содержание

- [Подключение к MCP-клиенту](#подключение-к-mcp-клиенту)
  - [Что понадобится](#что-понадобится)
  - [Подготовка: образ и авторизация](#подготовка-образ-и-авторизация)
  - [Общие параметры Docker](#общие-параметры-docker)
  - [Cursor (IDE)](#cursor-ide)
  - [Codex (CLI и расширение)](#codex-cli-и-расширение)
  - [Cursor Cloud (облачные агенты)](#cursor-cloud-облачные-агенты)
  - [Проверка подключения](#проверка-подключения)
  - [Частые проблемы](#частые-проблемы)
- [Что это такое](#что-это-такое)
- [Возможности](#возможности)
- [Архитектура и стек](#архитектура-и-стек)
- [Авторизация](#авторизация)
- [Сборка и запуск](#сборка-и-запуск)
- [Конфигурация](#конфигурация)
- [Документация](#документация)

## Подключение к MCP-клиенту

Сервер распространяется как **Docker-образ** и общается с клиентом по протоколу **MCP через stdio** (стандартный ввод-вывод). Клиент запускает контейнер как подпроцесс и обменивается с ним JSON-сообщениями.

Ниже — пошаговая инструкция для **Cursor**, **Codex** и **Cursor Cloud**. Во всех случаях используется один и тот же образ; отличается только файл конфигурации клиента.

### Что понадобится

| Требование | Зачем |
|---|---|
| **Docker Desktop** или Docker Engine | Запуск MCP-сервера в контейнере |
| **Приложение Яндекс OAuth** | `client_id` и `client_secret` для доступа к API |
| **Идентификатор организации** | `YANDEX_ORG_ID` — Tracker и Wiki работают в контексте организации |
| **5–10 минут на первичную авторизацию** | Один раз подтвердить доступ в браузере (Device Flow) |

Подробнее про OAuth — в [docs/auth.md](./docs/auth.md).

### Подготовка: образ и авторизация

Выполняется **один раз** на каждой машине, где будет работать MCP.

**1. Получите образ.** Публичный образ публикуется в GitHub Container Registry при каждом релизе (тег `v*`, например `v0.1.0`):

```bash
docker pull ghcr.io/sorface/yandex-mcp-server:latest
```

Локальная сборка — альтернатива для разработки:

```bash
docker build -t ghcr.io/sorface/yandex-mcp-server:latest .
```

**2. Выполните первичную авторизацию.** Команда `auth` откроет сценарий Device Flow: в терминале появятся адрес страницы и код для ввода в браузере.

```bash
docker run -it --rm \
  -e YANDEX_CLIENT_ID=<ваш_client_id> \
  -e YANDEX_CLIENT_SECRET=<ваш_client_secret> \
  -e YANDEX_ORG_ID=<идентификатор_организации> \
  -e YANDEX_ORG_TYPE=YANDEX_360 \
  -v yandex-mcp-tokens:/data \
  ghcr.io/sorface/yandex-mcp-server:latest auth
```

После подтверждения в браузере токены сохранятся в Docker-томе `yandex-mcp-tokens`. Повторять `auth` нужно только если токен отозван или истёк без возможности обновления.

**3. Проверьте, что том создан:**

```bash
docker volume inspect yandex-mcp-tokens
```

### Общие параметры Docker

В конфигурации любого клиента команда запуска сводится к одному шаблону:

```text
docker run -i --rm \
  -e YANDEX_CLIENT_ID … -e YANDEX_CLIENT_SECRET … \
  -e YANDEX_ORG_ID … -e YANDEX_ORG_TYPE … \
  -v yandex-mcp-tokens:/data \
  ghcr.io/sorface/yandex-mcp-server:latest serve
```

| Флаг / параметр | Назначение |
|---|---|
| `-i` | **Обязателен.** Без него MCP-клиент не сможет обмениваться сообщениями с сервером |
| `--rm` | Удалять контейнер после завершения сессии |
| `-v yandex-mcp-tokens:/data` | Том с сохранёнными токенами (тот же, что при `auth`) |
| `serve` | Режим MCP-сервера (по умолчанию) |

Дополнительные переменные (по желанию):

- `YANDEX_READ_ONLY=true` — режим только для чтения; изменяющие инструменты не будут доступны агенту.
- `YANDEX_ORG_TYPE=YANDEX_CLOUD` — если организация в Yandex Cloud (заголовок `X-Cloud-Org-ID`).

### Cursor (IDE)

Cursor запускает MCP-сервер как локальный процесс. Конфигурация хранится в JSON-файле `mcp.json`.

**Где лежит файл:**

| Область | Путь |
|---|---|
| Все проекты (рекомендуется) | `~/.cursor/mcp.json` |
| Только текущий проект | `.cursor/mcp.json` в корне репозитория |

**Как добавить сервер через интерфейс:** *Cursor Settings → Tools & MCP → Add new MCP server* (или *Edit Config*).

**Пример конфигурации** — вставьте блок `yandex` в `mcpServers`:

```json
{
  "mcpServers": {
    "yandex": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "-e", "YANDEX_CLIENT_ID",
        "-e", "YANDEX_CLIENT_SECRET",
        "-e", "YANDEX_ORG_ID",
        "-e", "YANDEX_ORG_TYPE",
        "-v", "yandex-mcp-tokens:/data",
        "ghcr.io/sorface/yandex-mcp-server:latest", "serve"
      ],
      "env": {
        "YANDEX_CLIENT_ID": "<ваш_client_id>",
        "YANDEX_CLIENT_SECRET": "<ваш_client_secret>",
        "YANDEX_ORG_ID": "<идентификатор_организации>",
        "YANDEX_ORG_TYPE": "YANDEX_360"
      }
    }
  }
}
```

**На Windows** (если Cursor не находит Docker): замените `"command": "docker"` на `"command": "docker.exe"`.

После сохранения файла **перезапустите Cursor** или нажмите *Reload* в настройках MCP. В списке инструментов должны появиться `system_ping`, `tracker_*`, `wiki_*`.

### Codex (CLI и расширение)

OpenAI Codex использует файл **`~/.codex/config.toml`** (формат TOML, не JSON). Конфигурация общая для CLI и расширения в VS Code.

**Как открыть файл:** в расширении Codex — *MCP settings → Open config.toml*, или вручную:

```bash
mkdir -p ~/.codex && nano ~/.codex/config.toml
```

**Пример конфигурации:**

```toml
[mcp_servers.yandex]
command = "docker"
args = [
  "run", "-i", "--rm",
  "-e", "YANDEX_CLIENT_ID",
  "-e", "YANDEX_CLIENT_SECRET",
  "-e", "YANDEX_ORG_ID",
  "-e", "YANDEX_ORG_TYPE",
  "-v", "yandex-mcp-tokens:/data",
  "ghcr.io/sorface/yandex-mcp-server:latest", "serve",
]
env = { YANDEX_CLIENT_ID = "<ваш_client_id>", YANDEX_CLIENT_SECRET = "<ваш_client_secret>", YANDEX_ORG_ID = "<идентификатор_организации>", YANDEX_ORG_TYPE = "YANDEX_360" }
```

Проверка, что сервер виден Codex:

```bash
codex mcp list
```

Альтернатива — интерактивное добавление:

```bash
codex mcp add yandex -- docker run -i --rm \
  -e YANDEX_CLIENT_ID -e YANDEX_CLIENT_SECRET \
  -e YANDEX_ORG_ID -e YANDEX_ORG_TYPE \
  -v yandex-mcp-tokens:/data \
  ghcr.io/sorface/yandex-mcp-server:latest serve
```

(секреты после этого нужно дописать в `env` вручную в `config.toml`).

### Cursor Cloud (облачные агенты)

**Cursor Cloud** (агенты на [cursor.com/agents](https://cursor.com/agents)) **не запускают Docker на вашем компьютере**. Конфигурация `~/.cursor/mcp.json` с `command: docker` работает только в **локальном** Cursor и Codex.

Для облака возможны два рабочих варианта:

| Вариант | Когда подходит | Что сделать |
|---|---|---|
| **Локальный Cursor / Codex** | Агент работает на вашей машине | Используйте конфигурацию из разделов выше |
| **Self-hosted workers (Team plan)** | Нужны облачные агенты с доступом к внутренним API | Настройте пул воркеров в [Cursor Dashboard → Integrations](https://cursor.com/dashboard/integrations); укажите MCP с транспортом **stdio** и командой `docker run … serve` на машине воркера, где установлен Docker и выполнена авторизация `auth` |

На воркере self-hosted pool должны быть:

1. Установлены Docker и образ `ghcr.io/sorface/yandex-mcp-server:latest`.
2. Выполнена команда `auth` с тем же томом `yandex-mcp-tokens`.
3. В dashboard добавлен MCP-сервер с той же командой `docker run -i --rm … serve`, что и для локального Cursor.

Облачные агенты **без** self-hosted pool не смогут вызвать Tracker/Wiki через этот Docker-образ — используйте локальный Cursor или Codex.

### Проверка подключения

После настройки любого клиента:

1. Убедитесь, что авторизация выполнена (`auth` завершился успешно).
2. В чате с агентом попросите вызвать инструмент **`system_ping`** — ответ должен быть `pong`.
3. Попросите вызвать **`yandex_auth_status`** — в ответе должно быть `авторизован: да`.
4. Для проверки Tracker — **`tracker_myself`** (вернёт профиль текущего пользователя).

### Частые проблемы

| Симптом | Вероятная причина | Решение |
|---|---|---|
| «Connection closed» / сервер не стартует | Нет флага `-i` у `docker run` | Добавьте `-i` в `args` |
| `401 Unauthorized` в инструментах | Токен не сохранён или истёк | Повторите `docker run … auth` с тем же томом `/data` |
| Docker не найден | Docker не в PATH клиента | Укажите полный путь к `docker` / `docker.exe` |
| Пустой список инструментов | Ошибка в JSON/TOML | Проверьте синтаксис; перезапустите клиент |
| Изменения не применяются | Клиент не перечитал конфиг | Reload MCP / перезапуск Cursor или Codex |
| Cloud-агент не видит MCP | Используется только локальный `mcp.json` | Настройте MCP в Dashboard или self-hosted pool |

Подробная документация по авторизации и архитектуре — в [docs/auth.md](./docs/auth.md) и [docs/architecture.md](./docs/architecture.md).

## Что это такое

Yandex Tracker и Yandex Wiki предоставляют только REST API. Этот сервер — посредник между ИИ-агентом и этими API: агент вызывает понятные инструменты, а сервер обращается к API Яндекса, добавляет авторизацию и возвращает результат.

Сервер работает по транспорту **stdio** и распространяется как **Docker-образ**.

## Возможности

- Около 60 инструментов для Yandex Tracker (задачи, комментарии, связи, чек-листы, учёт времени, вложения, очереди, доски, спринты, компоненты, версии, поля, справочники, пользователи, массовые операции, проекты и портфели).
- Около 25 инструментов для Yandex Wiki (страницы, содержимое, комментарии, вложения, динамические таблицы).
- Режим только для чтения (`READ_ONLY`): изменяющие инструменты не регистрируются вовсе — агент их не видит в `tools/list` и не может вызвать; дополнительно действует защита на уровне сервиса.
- Автоматические повторные запросы при временных сбоях API (сетевые ошибки, `429`, `5xx`) с экспоненциальной задержкой и учётом заголовка `Retry-After`.
- Служебный инструмент `system_server_info` сообщает текущий режим работы сервера.

Полный список — в разделе [docs/capabilities](./docs/capabilities/README.md).

## Архитектура и стек

- Язык: Kotlin, сборка Maven.
- Фреймворк: Spring Boot 3 + Spring AI MCP Server (транспорт stdio).
- HTTP-клиент: Spring RestClient.
- Тесты: JUnit 5, MockK, AssertJ, WireMock.

Подробное описание архитектуры — в [docs/architecture.md](./docs/architecture.md).

## Авторизация

Авторизация выполняется по протоколу **OAuth 2.0, сценарий Device Flow**. Передаются `client_id` и `client_secret` приложения Яндекс OAuth. Токены сохраняются в подключённом томе Docker и обновляются автоматически.

Первичная авторизация выполняется командой `auth`, последующая работа — командой `serve`. Подробности — в документе [docs/auth.md](./docs/auth.md).

## Сборка и запуск

### Публикация релиза

При push тега вида `v0.1.0` GitHub Actions (workflow **Release**):

1. Запускает тесты.
2. Собирает jar и создаёт [GitHub Release](https://github.com/sorface/yandex-mcp-server/releases) с артефактом.
3. Публикует Docker-образ в `ghcr.io/sorface/yandex-mcp-server` с тегами `latest`, `0.1.0`, `0.1`, `0`.

```bash
git tag v0.1.0
git push origin v0.1.0
```

После первой публикации откройте **GitHub → Packages → yandex-mcp-server → Package settings** и установите видимость **Public**, чтобы образ можно было скачивать без авторизации.

### Локальная разработка

Сборка jar:

```bash
mvn -DskipTests package
```

Локальный запуск (stdio):

```bash
java -jar target/yandex-mcp-server-0.1.0-SNAPSHOT.jar
```

Сборка Docker-образа:

```bash
docker build -t ghcr.io/sorface/yandex-mcp-server:latest .
```

Запуск MCP-сервера в Docker:

```bash
docker run -i --rm \
  -e YANDEX_CLIENT_ID -e YANDEX_CLIENT_SECRET -e YANDEX_ORG_ID -e YANDEX_ORG_TYPE \
  -v yandex-mcp-tokens:/data \
  ghcr.io/sorface/yandex-mcp-server:latest serve
```

## Конфигурация

Все настройки задаются переменными окружения.


| Переменная                | Назначение                                       | По умолчанию                     |
| ------------------------- | ------------------------------------------------ | -------------------------------- |
| `YANDEX_CLIENT_ID`        | Идентификатор приложения Яндекс OAuth            | пусто                            |
| `YANDEX_CLIENT_SECRET`    | Секретный ключ приложения Яндекс OAuth           | пусто                            |
| `YANDEX_ORG_ID`           | Идентификатор организации                        | пусто                            |
| `YANDEX_ORG_TYPE`         | Тип организации: `YANDEX_360` или `YANDEX_CLOUD` | `YANDEX_360`                     |
| `YANDEX_TOKEN_STORE_PATH` | Путь к файлу токенов                             | `/data/tokens.json`              |
| `YANDEX_READ_ONLY`        | Режим только для чтения                          | `false`                          |
| `YANDEX_TRACKER_BASE_URL` | Базовый адрес API Tracker                        | `https://api.tracker.yandex.net` |
| `YANDEX_WIKI_BASE_URL`    | Базовый адрес API Wiki                           | `https://api.wiki.yandex.net`    |
| `YANDEX_OAUTH_BASE_URL`   | Базовый адрес OAuth-сервиса                      | `https://oauth.yandex.com`       |
| `YANDEX_OAUTH_SCOPES`     | Запрашиваемые разрешения                         | пусто                            |
| `YANDEX_RETRY_ENABLED`    | Повторять запросы при временных сбоях            | `true`                           |
| `YANDEX_RETRY_MAX_ATTEMPTS` | Максимум попыток, включая первую               | `3`                              |
| `YANDEX_RETRY_INITIAL_DELAY` | Начальная задержка перед повтором             | `500ms`                          |
| `YANDEX_RETRY_MULTIPLIER` | Множитель экспоненциального роста задержки        | `2.0`                            |
| `YANDEX_RETRY_MAX_DELAY`  | Верхняя граница задержки между попытками         | `10s`                            |

### Повторные запросы

Запросы к API Tracker и Wiki повторяются автоматически при временных сбоях: сетевых ошибках
(соединение не установлено или разорвано), превышении лимита запросов (`429`) и временных ошибках
сервиса (`5xx`). Задержка между попытками растёт экспоненциально (`initial-delay`,
`initial-delay × multiplier`, …) и ограничена сверху `max-delay`. Если ответ `429` содержит
заголовок `Retry-After`, используется указанное в нём время. Остальные ошибки (например, `4xx`,
кроме `429`) не повторяются. Повторы можно отключить переменной `YANDEX_RETRY_ENABLED=false`.

## Документация

- [Возможности Tracker и Wiki](./docs/capabilities/README.md)
- [Архитектура](./docs/architecture.md)
- [Авторизация (OAuth 2.0 Device Flow)](./docs/auth.md)
