# syntax=docker/dockerfile:1

# Этап сборки: компилируем проект и собираем исполняемый jar.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
ARG MCP_MODULE=yandex-mcp-tracker
COPY pom.xml .
COPY yandex-mcp-core/pom.xml ./yandex-mcp-core/pom.xml
COPY yandex-mcp-tracker/pom.xml ./yandex-mcp-tracker/pom.xml
COPY yandex-mcp-wiki/pom.xml ./yandex-mcp-wiki/pom.xml
# Прогрев кэша зависимостей: офлайн-резолв до копирования исходников.
RUN mvn -q -B -DskipTests dependency:go-offline
COPY yandex-mcp-core/src ./yandex-mcp-core/src
COPY yandex-mcp-tracker/src ./yandex-mcp-tracker/src
COPY yandex-mcp-wiki/src ./yandex-mcp-wiki/src
RUN mvn -q -B -DskipTests package

# Этап выполнения: минимальный образ только с JRE и собранным приложением.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
ARG MCP_MODULE=yandex-mcp-tracker

# Метаданные образа по спецификации OCI.
LABEL org.opencontainers.image.title="yandex-mcp-server" \
      org.opencontainers.image.description="Independent MCP server for Yandex Tracker or Yandex Wiki" \
      org.opencontainers.image.source="https://github.com/developerdevpav/yandex-mcp-server" \
      org.opencontainers.image.licenses="Apache-2.0"

# Путь к файлу токенов в подключённом томе. Может быть переопределён переменной окружения.
ENV YANDEX_TOKEN_STORE_PATH=/data/tokens.json

# Том для хранения токенов авторизации между запусками.
VOLUME ["/data"]

COPY --from=build /build/${MCP_MODULE}/target/${MCP_MODULE}-*.jar /app/app.jar
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh \
    && useradd --uid 1001 --no-create-home --home-dir /app --shell /usr/sbin/nologin mcp \
    && mkdir -p /data \
    && chown -R mcp:mcp /app /data
USER mcp

# Команда по умолчанию — запуск MCP-сервера по транспорту stdio.
ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["serve"]
