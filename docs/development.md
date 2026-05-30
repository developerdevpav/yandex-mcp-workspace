# Разработка

## Репозиторий и модули

| Maven-модуль | Назначение |
|---|---|
| `yandex-mcp-workspace` | корневой POM, сборка workspace |
| `yandex-mcp-core` | OAuth, HTTP-инфраструктура, общие MCP-инструменты |
| `yandex-mcp-workspace-tracker` | MCP-сервер Tracker, `TrackerApiProperties`, `trackerRestClient` |
| `yandex-mcp-workspace-wiki` | MCP-сервер Wiki, `WikiApiProperties`, `wikiRestClient` |

## Сборка и тесты

```bash
mvn test
mvn -DskipTests package
```

JAR после сборки:

- `yandex-mcp-workspace-tracker/target/yandex-mcp-workspace-tracker-*.jar`
- `yandex-mcp-workspace-wiki/target/yandex-mcp-workspace-wiki-*.jar`

Локальный запуск (stdio):

```bash
java -jar yandex-mcp-workspace-tracker/target/yandex-mcp-workspace-tracker-0.1.0-SNAPSHOT.jar
java -jar yandex-mcp-workspace-tracker/target/yandex-mcp-workspace-tracker-0.1.0-SNAPSHOT.jar auth
```

Переменные окружения — как в [configuration.md](./configuration.md).

## Docker

Сборка образа Tracker (из корня репозитория):

```bash
docker build --build-arg MCP_MODULE=yandex-mcp-workspace-tracker -t yandex-mcp-workspace-tracker:local .
```

Сборка образа Wiki:

```bash
docker build --build-arg MCP_MODULE=yandex-mcp-workspace-wiki -t yandex-mcp-workspace-wiki:local .
```

## Релизы и CI

- **CI** (`.github/workflows/ci.yml`) — `mvn test` на push/PR в `main`/`master`.
- **Release** (`.github/workflows/release.yml`) — по тегу `v*`:
  - тесты и сборка JAR;
  - публикация образов `ghcr.io/<owner>/<repo>-tracker` и `ghcr.io/<owner>/<repo>-wiki` для `linux/amd64` и `linux/arm64`;
  - GitHub Release с артефактами JAR.

При репозитории `developerdevpav/yandex-mcp-workspace` образы:

- `ghcr.io/developerdevpav/yandex-mcp-workspace-tracker`
- `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki`

## Интеграционные тесты

В модулях tracker/wiki — Spring Boot тесты с WireMock (профиль `integration-test`). Каждый модуль мокает только свой API (Tracker или Wiki).
