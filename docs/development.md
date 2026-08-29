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
java -jar yandex-mcp-workspace-tracker/target/yandex-mcp-workspace-tracker-0.1.0-SNAPSHOT.jar setup
```

`setup` запускает интерактивный мастер. Для CI те же значения можно передать короткими параметрами `--client-id`, `--client-secret` и `--org-id`.

Переменные окружения — как в [configuration.md](./configuration.md).

## Docker

Рекомендуемый способ — сначала собрать JAR, затем упаковать runtime-образ (`Dockerfile.runtime`):

```bash
mvn -DskipTests package

docker build -f Dockerfile.runtime \
  --build-arg MCP_MODULE=yandex-mcp-workspace-tracker \
  -t yandex-mcp-workspace-tracker:local .

docker build -f Dockerfile.runtime \
  --build-arg MCP_MODULE=yandex-mcp-workspace-wiki \
  -t yandex-mcp-workspace-wiki:local .
```

Если Maven на машине нет, можно собрать всё внутри Docker (`Dockerfile`):

```bash
docker build --build-arg MCP_MODULE=yandex-mcp-workspace-tracker -t yandex-mcp-workspace-tracker:local .
docker build --build-arg MCP_MODULE=yandex-mcp-workspace-wiki -t yandex-mcp-workspace-wiki:local .
```

## Релизы и CI

- **CI** (`.github/workflows/ci.yml`) — `mvn test` на push/PR в `main`/`master`.
- **Release** (`.github/workflows/release.yml`) — по SemVer-тегу `v*` или через ручной запуск:
  - полный `mvn verify` и установка версии тега в Maven-модули на время CI;
  - переносимые пакеты со встроенной Java для Linux x64/ARM64, macOS Intel/Apple Silicon и Windows x64;
  - параллельная публикация образов `ghcr.io/<owner>/<repo>-tracker` и `ghcr.io/<owner>/<repo>-wiki` из `Dockerfile.runtime` для `linux/amd64` и `linux/arm64`;
  - GitHub Release с пакетами, JAR, SHA-256 и provenance attestations.

Подробно процесс описан в [releases.md](./releases.md).

При репозитории `developerdevpav/yandex-mcp-workspace` образы:

- `ghcr.io/developerdevpav/yandex-mcp-workspace-tracker`
- `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki`

## Интеграционные тесты

В модулях tracker/wiki — Spring Boot тесты с WireMock (профиль `integration-test`). Каждый модуль мокает только свой API (Tracker или Wiki).
