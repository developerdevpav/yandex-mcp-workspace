# Документация Yandex MCP Workspace

Репозиторий [developerdevpav/yandex-mcp-workspace](https://github.com/developerdevpav/yandex-mcp-workspace) — Maven-workspace с двумя независимыми MCP-серверами для Yandex Tracker и Yandex Wiki.

## С чего начать

| Документ | Для кого | Содержание |
|---|---|---|
| [overview.md](./overview.md) | все | структура проекта, модули, образы Docker |
| [credentials.md](./credentials.md) | пользователь | где создать OAuth-приложение, взять ключи и ID организации |
| [setup.md](./setup.md) | пользователь | локальный запуск без Docker, OAuth, команды setup/login/doctor |
| [configuration.md](./configuration.md) | администратор | переменные окружения по серверам |
| [mcp-clients.md](./mcp-clients.md) | пользователь | Claude, ChatGPT Codex, Cursor и проверка подключения |
| [releases.md](./releases.md) | пользователь / разработчик | скачивание, версии, артефакты и проверка релиза |
| [troubleshooting.md](./troubleshooting.md) | поддержка | типичные ошибки и решения |
| [development.md](./development.md) | разработчик | сборка, тесты, релизы |

## По продуктам

| Документ | Содержание |
|---|---|
| [tracker.md](./tracker.md) | особенности MCP-сервера Tracker |
| [wiki.md](./wiki.md) | особенности MCP-сервера Wiki |
| [capabilities/](./capabilities/README.md) | полный список инструментов и endpoint API |

## Быстрый старт

Краткая инструкция и примеры `mcp.json` — в [README.md](../README.md) в корне репозитория.
