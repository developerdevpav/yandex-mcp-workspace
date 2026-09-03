# Установка и первый запуск

## Быстрая установка через терминал

Для macOS и Linux x64/ARM64 выполните:

```bash
curl -fsSL https://github.com/developerdevpav/yandex-mcp-workspace/releases/latest/download/install.sh | bash
```

Установщик работает без Java, Docker и `sudo`. В обычном сценарии пользователь:

1. Выбирает Tracker, Wiki или оба сервера. По умолчанию устанавливаются оба.
2. Вводит реквизиты собственного приложения Yandex OAuth и идентификатор организации.
3. Подтверждает вход на странице Яндекса, которую установщик открывает в браузере.
4. Выбирает обнаруженные Codex, Claude и Cursor и уровень подключения.

Архивы скачиваются из последнего стабильного GitHub Release и проверяются по `SHA256SUMS`.
Версии хранятся в `~/.local/share/yandex-mcp/releases/<version>/`, а конфигурации клиентов
используют стабильные команды из `~/.local/bin`.

Повторный запуск обновляет выбранные компоненты. Если сохранённый OAuth-токен можно проверить
или обновить, повторный вход не требуется. При ошибке скачивания, проверки или `doctor`
предыдущий launcher остаётся рабочим. Если авторизация отменена, конфигурации агентов не меняются;
её можно повторить командой `~/.local/bin/yandex-mcp-tracker setup` или повторным запуском установки.

Перед прямым изменением JSON/TOML-конфигурации клиента мастер создаёт рядом резервную копию
с суффиксом `.backup-<UTC timestamp>`. Существующие записи `yandex-tracker` и `yandex-wiki`
заменяются только после подтверждения; остальные MCP-серверы сохраняются.

> Windows пока использует ручную установку из архива, описанную ниже.

## Выбор сервера

Tracker и Wiki устанавливаются отдельно. Каждый архив содержит один MCP-сервер и собственную Java Runtime 21. Java и Docker пользователю не нужны.

| Что требуется пользователю | Что скачать | Какие инструменты появятся |
|---|---|---|
| Только задачи Yandex Tracker | `yandex-mcp-tracker-...` | `tracker_*`, `system_*`, `yandex_auth_*` |
| Только страницы Yandex Wiki | `yandex-mcp-wiki-...` | `wiki_*`, `system_*`, `yandex_auth_*` |
| Tracker и Wiki | оба архива | оба набора инструментов как два MCP-сервера |

Если установлены оба сервера, они используют один OAuth-профиль и одно хранилище токенов текущего пользователя. Каталоги приложений объединять не нужно.

## Скачивание

Откройте [последний GitHub Release](https://github.com/developerdevpav/yandex-mcp-workspace/releases/latest). Выберите строку своей ОС и скачайте Tracker, Wiki или оба файла.

| ОС | Tracker | Wiki |
|---|---|---|
| Linux x64 | `yandex-mcp-tracker-<version>-linux-x64.tar.gz` | `yandex-mcp-wiki-<version>-linux-x64.tar.gz` |
| Linux ARM64 | `yandex-mcp-tracker-<version>-linux-arm64.tar.gz` | `yandex-mcp-wiki-<version>-linux-arm64.tar.gz` |
| macOS Intel | `yandex-mcp-tracker-<version>-macos-x64.tar.gz` | `yandex-mcp-wiki-<version>-macos-x64.tar.gz` |
| macOS Apple Silicon | `yandex-mcp-tracker-<version>-macos-arm64.tar.gz` | `yandex-mcp-wiki-<version>-macos-arm64.tar.gz` |
| Windows x64 | `yandex-mcp-tracker-<version>-windows-x64.zip` | `yandex-mcp-wiki-<version>-windows-x64.zip` |

После распаковки каждый архив имеет собственную папку. Относительный путь к запускаемому файлу зависит от ОС:

| ОС | Tracker | Wiki |
|---|---|---|
| Linux | `app/bin/yandex-mcp-tracker` | `app/bin/yandex-mcp-wiki` |
| macOS | `yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker` | `yandex-mcp-wiki.app/Contents/MacOS/yandex-mcp-wiki` |
| Windows | `app\yandex-mcp-tracker.exe` | `app\yandex-mcp-wiki.exe` |

На Linux и Windows оставьте распакованную папку в постоянном месте: MCP-клиенту потребуется абсолютный путь к файлу. На macOS перенесите каждый нужный `.app` в `~/Applications` или `/Applications` до настройки MCP-клиента.

### Пример для macOS Apple Silicon

Для Tracker скачайте `yandex-mcp-tracker-<version>-macos-arm64.tar.gz`, распакуйте архив и перенесите:

```text
yandex-mcp-tracker.app → ~/Applications/yandex-mcp-tracker.app
```

Для Wiki выполните то же действие с `yandex-mcp-wiki-<version>-macos-arm64.tar.gz`:

```text
yandex-mcp-wiki.app → ~/Applications/yandex-mcp-wiki.app
```

В результате в Finder отображаются два самостоятельных приложения. Wiki больше не находится внутри `yandex-mcp-tracker.app`.

У приложений нет собственного графического окна. Не запускайте их двойным кликом в Finder: команду `setup` нужно выполнить один раз через Terminal, а затем нужный `.app` автоматически запускает MCP-клиент — Claude, Codex или Cursor.

### Системные предупреждения

Пока релиз не подписан сертификатами Apple и Microsoft, macOS Gatekeeper или Windows SmartScreen могут показать предупреждение. Сверьте `SHA256SUMS` на странице релиза. На macOS для доверенного скачанного архива может потребоваться:

```bash
xattr -dr com.apple.quarantine "$HOME/Applications/yandex-mcp-tracker.app"
xattr -dr com.apple.quarantine "$HOME/Applications/yandex-mcp-wiki.app"
```

Подпись и notarization должны быть включены отдельно после добавления сертификатов в GitHub Secrets.

## Альтернатива: отдельный JAR

Для JAR требуется JRE 21. При сборке из исходников:

```bash
mvn -q -DskipTests package
```

Исполняемые файлы появятся в `yandex-mcp-workspace-tracker/target` и `yandex-mcp-workspace-wiki/target`.

## Первичная настройка

Один раз выполните `setup` через любой установленный сервер. Если установлены Tracker и Wiki, повторная настройка второго приложения не нужна: они используют общий OAuth-профиль.

Tracker на Linux:

```bash
./app/bin/yandex-mcp-tracker setup
```

Wiki на Linux:

```bash
./app/bin/yandex-mcp-wiki setup
```

Tracker на macOS после переноса в `~/Applications`:

```bash
"$HOME/Applications/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker" setup
```

Wiki на macOS после переноса в `~/Applications`:

```bash
"$HOME/Applications/yandex-mcp-wiki.app/Contents/MacOS/yandex-mcp-wiki" setup
```

Tracker на Windows PowerShell:

```powershell
.\app\yandex-mcp-tracker.exe setup
```

Wiki на Windows PowerShell:

```powershell
.\app\yandex-mcp-wiki.exe setup
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
