# Установка и первый запуск

## Рекомендуемый вариант: готовый пакет

Откройте [последний GitHub Release](https://github.com/developerdevpav/yandex-mcp-workspace/releases/latest) и скачайте архив своей платформы. В каждый пакет уже входят Tracker, Wiki и Java Runtime 21 — устанавливать Java или Docker не требуется.

| ОС | Архив |
|---|---|
| Linux x64 | `yandex-mcp-workspace-<version>-linux-x64.tar.gz` |
| Linux ARM64 | `yandex-mcp-workspace-<version>-linux-arm64.tar.gz` |
| macOS Intel | `yandex-mcp-workspace-<version>-macos-x64.tar.gz` |
| macOS Apple Silicon | `yandex-mcp-workspace-<version>-macos-arm64.tar.gz` |
| Windows x64 | `yandex-mcp-workspace-<version>-windows-x64.zip` |

Распакуйте архив в постоянный каталог. Внутри находятся два исполняемых файла:

| ОС | Tracker | Wiki |
|---|---|---|
| Linux | `app/bin/yandex-mcp-tracker` | `app/bin/yandex-mcp-wiki` |
| macOS | `app/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker` | `app/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-wiki` |
| Windows | `app\yandex-mcp-tracker\yandex-mcp-tracker.exe` | `app\yandex-mcp-tracker\yandex-mcp-wiki.exe` |

Пакет переносимый: «установка» заключается в распаковке в выбранный каталог. Его нельзя перемещать после добавления абсолютного пути в конфигурацию MCP-клиента.

### Системные предупреждения

Пока релиз не подписан сертификатами Apple и Microsoft, macOS Gatekeeper или Windows SmartScreen могут показать предупреждение. Сверьте `SHA256SUMS` на странице релиза. На macOS для доверенного скачанного архива может потребоваться:

```bash
xattr -dr com.apple.quarantine /absolute/path/yandex-mcp-workspace-<version>-macos-<arch>
```

Подпись и notarization должны быть включены отдельно после добавления сертификатов в GitHub Secrets.

## Альтернатива: отдельный JAR

Для JAR требуется JRE 21. При сборке из исходников:

```bash
mvn -q -DskipTests package
```

Исполняемые файлы появятся в `yandex-mcp-workspace-tracker/target` и `yandex-mcp-workspace-wiki/target`.

## Первичная настройка

Один раз выполните `setup` через Tracker. Tracker и Wiki используют общий OAuth-профиль, поэтому повторная настройка не нужна.

Linux:

```bash
./app/bin/yandex-mcp-tracker setup
```

macOS:

```bash
./app/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker setup
```

Windows PowerShell:

```powershell
.\app\yandex-mcp-tracker\yandex-mcp-tracker.exe setup
```

Для отдельного JAR:

```bash
java -jar yandex-mcp-workspace-tracker/target/yandex-mcp-workspace-tracker-0.1.0-SNAPSHOT.jar setup
```

Команда:

1. Интерактивно запросит только отсутствующие настройки OAuth и организации. В настоящем терминале `client_secret` вводится без отображения.
2. Запросит Device Flow у Yandex OAuth.
3. Попытается открыть системный браузер.
4. Покажет адрес и код в терминале как fallback.
5. Дождётся подтверждения пользователя.
6. Атомарно сохранит токены и настройки для следующих запусков.

Для CI или скрипта значения можно передать неинтерактивно: `--client-id=...`, `--client-secret=...`, `--org-id=...`, `--org-type=YANDEX_360`. Короткие параметры преобразуются в настройки Spring Boot. Повторно передавать их при запуске MCP не требуется.

Локальные файлы:

| Данные | Путь по умолчанию |
|---|---|
| Настройки OAuth и организации | `~/.config/yandex-mcp/config.properties` |
| Токены Linux | `${XDG_STATE_HOME:-~/.local/state}/yandex-mcp/tokens.json` |
| Токены macOS | `~/Library/Application Support/yandex-mcp/tokens.json` |
| Токены Windows | `%LOCALAPPDATA%\yandex-mcp\tokens.json` |

На POSIX-системах файлам назначаются права `600`. Запись токена выполняется через временный файл и атомарную замену; общий lock-файл предотвращает одновременный refresh в процессах Tracker и Wiki.

Путь настроек можно изменить через `YANDEX_CONFIG_PATH`, путь токенов — через `YANDEX_TOKEN_STORE_PATH`.

## Команды управления

| Команда | Действие |
|---|---|
| `setup` | Авторизоваться и сохранить настройки текущего запуска |
| `login` | Повторно авторизоваться, не изменяя сохранённые настройки |
| `auth` | Совместимый псевдоним `login` |
| `logout` | Удалить локальные токены |
| `doctor` | Показать настройки и состояние токена без секретов |

## Авторизация прямо из MCP

Если сервер уже добавлен в MCP-клиент, отдельный терминал для Device Flow не нужен:

1. Вызовите `yandex_auth_start`.
2. Откройте `verificationUrl` и введите `userCode`; браузер также будет открыт автоматически, если это поддерживает окружение.
3. Не раньше `nextPollAt` вызовите `yandex_auth_poll` с полученным `sessionId`.
4. При состоянии `AUTHORIZED` можно вызывать инструменты Tracker или Wiki.

Внутренний `device_code`, access token и refresh token никогда не возвращаются через MCP.

## Подключение MCP-клиента

После `setup` конфигурации Claude, ChatGPT Codex или Cursor нужен только абсолютный путь к исполняемому файлу — секреты в неё не добавляются. Готовые примеры приведены в [mcp-clients.md](./mcp-clients.md).

## Docker — опционально

Docker остаётся полезен для CI, серверного запуска и изолированного окружения. В образе путь токенов по-прежнему равен `/data/tokens.json`.

Первичная настройка:

```bash
docker run -it --rm \
  -e YANDEX_CLIENT_ID=<client_id> \
  -e YANDEX_CLIENT_SECRET=<client_secret> \
  -e YANDEX_ORG_ID=<org_id> \
  -e YANDEX_ORG_TYPE=YANDEX_360 \
  -v yandex-mcp-tokens:/data \
  ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest setup
```

Запуск MCP:

```bash
docker run -i --rm \
  -e YANDEX_CLIENT_ID \
  -e YANDEX_CLIENT_SECRET \
  -e YANDEX_ORG_ID \
  -e YANDEX_ORG_TYPE \
  -v yandex-mcp-tokens:/data \
  ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest serve
```

В Docker браузер обычно не открывается, но `yandex_auth_start` возвращает ссылку и код непосредственно MCP-клиенту.

## Проверка

Попросите агента вызвать:

1. `system_ping` — ожидается `pong`.
2. `yandex_auth_status` — ожидается `авторизован: да`.
3. `tracker_myself` или чтение страницы Wiki — запрос не должен завершаться `401`.
