# Подключение к AI-клиентам

Yandex MCP Workspace работает по локальному транспорту `stdio`. Docker не нужен: AI-клиент сам запускает исполняемый файл из скачанного пакета и общается с ним через стандартные потоки.

## Поддерживаемые клиенты

| Клиент | Локальный пакет | Где настраивать |
|---|---:|---|
| Claude Desktop | да | локальный MCP / `claude_desktop_config.json` |
| Claude Code | да | `claude mcp add` или `.mcp.json` |
| ChatGPT Desktop / Codex | да | экран **MCP servers**, `codex mcp add` или `config.toml` |
| Cursor | да | `~/.cursor/mcp.json` или `.cursor/mcp.json` |
| ChatGPT в браузере | нет | нужен удалённый MCP через plugin |
| Claude.ai в браузере | нет | нужен удалённый MCP connector |

Веб-клиент не может запустить приложение на компьютере пользователя. Текущая версия проекта рассчитана на локальные клиенты; удалённый HTTP-транспорт в ней не включён.

## Подготовка

Если использовалась [быстрая установка](./setup.md#быстрая-установка-через-терминал), этот раздел
можно пропустить: мастер уже запускает команду `connect` после OAuth. Подключение можно повторить:

```bash
~/.local/bin/yandex-mcp-tracker connect
```

Команда обнаруживает Codex CLI/Desktop, Claude Code, Claude Desktop и Cursor, позволяет выбрать
несколько клиентов и пользовательский либо проектный уровень там, где он поддерживается.

Скачайте и распакуйте [отдельный пакет нужного сервера](./setup.md#выбор-сервера). Можно установить только Tracker, только Wiki или оба сервера. Выполните `setup` в любом установленном сервере; OAuth-профиль будет общим. Например, в Linux для Tracker:

```bash
/absolute/path/yandex-mcp-tracker/app/bin/yandex-mcp-tracker setup
```

Мастер сохранит настройки и токены локально. Поэтому `client_secret`, OAuth-токен и переменные окружения не нужно помещать в конфигурацию AI-клиента.

В примерах замените `/absolute/path/to/yandex-mcp-*` на абсолютные пути к исполняемым файлам из таблицы в [setup.md](./setup.md#скачивание). На macOS после переноса приложений в `~/Applications` используйте:

```text
/Users/USERNAME/Applications/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker
/Users/USERNAME/Applications/yandex-mcp-wiki.app/Contents/MacOS/yandex-mcp-wiki
```

Добавляйте в конфигурацию только установленные серверы. Если нужен только Tracker, секция Wiki не требуется, и наоборот.

## Claude

### Claude Code — рекомендуемый способ

Добавить нужные серверы для текущего пользователя. Выполните одну команду для одного сервера или обе команды, если установлены оба:

```bash
claude mcp add --scope user --transport stdio yandex-tracker -- \
  /absolute/path/to/yandex-mcp-tracker

claude mcp add --scope user --transport stdio yandex-wiki -- \
  /absolute/path/to/yandex-mcp-wiki
```

Проверка:

```bash
claude mcp list
claude mcp get yandex-tracker
```

Внутри Claude Code состояние серверов также доступно по команде `/mcp`.

Для конфигурации на уровне проекта создайте `.mcp.json` в корне репозитория:

```json
{
  "mcpServers": {
    "yandex-tracker": {
      "command": "/absolute/path/to/yandex-mcp-tracker"
    },
    "yandex-wiki": {
      "command": "/absolute/path/to/yandex-mcp-wiki"
    }
  }
}
```

Claude Code запросит подтверждение для MCP на уровне проекта. Файл с абсолютными пользовательскими путями обычно не следует коммитить.

### Claude Desktop

В актуальном Claude Desktop основной пользовательский формат локальных интеграций — Desktop Extension (`.dxt`). До появления готового DXT-пакета сервер можно подключить как локальный MCP для разработки с тем же JSON.

Файл конфигурации:

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`;
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`.

Поместите в него JSON из предыдущего раздела, полностью завершите Claude Desktop и запустите снова. Если организация управляет Claude централизованно, администратор должен разрешить локальные MCP для разработки.

Официальные материалы: [Claude Code MCP](https://code.claude.com/docs/en/mcp), [локальные расширения Claude Desktop](https://support.anthropic.com/en/articles/10949351-getting-started-with-local-mcp-servers-on-claude-desktop).

## ChatGPT Desktop и Codex

ChatGPT Desktop, Codex CLI и IDE extension используют MCP-конфигурацию одного Codex-host. Сервер, добавленный одним из способов ниже, становится доступен остальным локальным клиентам на том же host.

### Через интерфейс ChatGPT Desktop

1. Откройте **Settings → MCP servers → Add server**.
2. Выберите транспорт **STDIO**.
3. Для Tracker задайте абсолютный путь к исполняемому файлу как команду; аргументы не нужны.
4. Если установлен Wiki, добавьте его отдельным сервером с путём к `yandex-mcp-wiki`.
5. Сохраните и выберите **Restart**.

В composer команда `/mcp` показывает подключённые серверы.

### Через Codex CLI

```bash
codex mcp add yandex-tracker -- \
  /absolute/path/to/yandex-mcp-tracker

codex mcp add yandex-wiki -- \
  /absolute/path/to/yandex-mcp-wiki

codex mcp list
```

Если установлен только один пакет, выполните только соответствующую команду `codex mcp add`.

### Через `config.toml`

Пользовательский файл — `~/.codex/config.toml`. Для доверенного проекта также поддерживается `.codex/config.toml`:

```toml
[mcp_servers.yandex-tracker]
command = "/absolute/path/to/yandex-mcp-tracker"
startup_timeout_sec = 30
tool_timeout_sec = 120

[mcp_servers.yandex-wiki]
command = "/absolute/path/to/yandex-mcp-wiki"
startup_timeout_sec = 30
tool_timeout_sec = 120
```

`startup_timeout_sec = 30` оставляет запас на холодный запуск JVM. `tool_timeout_sec = 120` полезен для больших выборок Tracker/Wiki.

ChatGPT в браузере не читает локальный `~/.codex/config.toml`; для него потребуется отдельная публикация сервера как удалённого MCP/plugin. Актуальный формат локального подключения описан в [официальной документации OpenAI](https://developers.openai.com/codex/mcp/).

## Cursor

Файл конфигурации:

- глобально: `~/.cursor/mcp.json`;
- для проекта: `.cursor/mcp.json`.

```json
{
  "mcpServers": {
    "yandex-tracker": {
      "command": "/absolute/path/to/yandex-mcp-tracker"
    },
    "yandex-wiki": {
      "command": "/absolute/path/to/yandex-mcp-wiki"
    }
  }
}
```

После сохранения откройте **Settings → Tools & MCP** и выполните Reload или перезапустите Cursor. В Cursor Agent CLI можно проверить подключение командами:

```bash
cursor-agent mcp list
cursor-agent mcp list-tools yandex-tracker
```

Официальная инструкция: [Cursor MCP](https://docs.cursor.com/context/model-context-protocol).

## Windows

В JSON обратные слэши экранируются:

```json
{
      "command": "C:\\Tools\\yandex-mcp-tracker\\app\\yandex-mcp-tracker.exe"
}
```

В TOML можно использовать пути с прямыми слэшами: `C:/Tools/yandex-mcp-tracker/app/yandex-mcp-tracker.exe`.

## Запуск отдельного JAR

Для разработки или минимального размера скачивания можно использовать JAR с версией релиза. Тогда потребуется JRE 21, а запись сервера выглядит так:

```json
{
  "command": "/absolute/path/to/java",
  "args": ["-jar", "/absolute/path/yandex-mcp-workspace-tracker.jar"]
}
```

## Авторизация из чата

Если `setup` ещё не получал OAuth-токен, но настройки приложения уже сохранены:

1. Попросите AI вызвать `yandex_auth_start`.
2. Откройте возвращённый `verificationUrl` и введите `userCode`.
3. Попросите вызвать `yandex_auth_poll` с `sessionId` не раньше `nextPollAt`.
4. После состояния `AUTHORIZED` вызовите `tracker_myself` или инструмент Wiki.

Внутренний `device_code`, access token и refresh token в чат не возвращаются.

## Docker — опционально

В любом JSON-примере локальную команду можно заменить контейнером. Пример для Tracker:

```json
{
  "command": "docker",
  "args": [
    "run", "-i", "--rm",
    "-e", "YANDEX_CLIENT_ID",
    "-e", "YANDEX_CLIENT_SECRET",
    "-e", "YANDEX_ORG_ID",
    "-e", "YANDEX_ORG_TYPE",
    "-v", "yandex-mcp-tokens:/data",
    "ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest",
    "serve"
  ],
  "env": {
    "YANDEX_CLIENT_ID": "<client_id>",
    "YANDEX_CLIENT_SECRET": "<client_secret>",
    "YANDEX_ORG_ID": "<org_id>",
    "YANDEX_ORG_TYPE": "YANDEX_360"
  }
}
```

Для Wiki замените имя образа на `yandex-mcp-workspace-wiki`. Переносимый пакет предпочтительнее для персонального использования, поскольку не требует Java и после `setup` MCP-конфигурация не содержит секретов.

## Проверка

| Вызов | Ожидаемый результат |
|---|---|
| `system_ping` | `pong` |
| `yandex_auth_status` | настройки и состояние токена без секретов |
| `tracker_myself` | данные текущего пользователя Tracker |
| `wiki_page_get_by_slug` | ответ Wiki или доменная ошибка, но не отсутствие токена |

При ошибках смотрите [troubleshooting.md](./troubleshooting.md).
