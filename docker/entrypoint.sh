#!/usr/bin/env sh
set -e

# Точка входа Docker-образа MCP-сервера.
#
# Поддерживаемые команды:
#   serve  — запуск MCP-сервера по транспорту stdio (режим по умолчанию).
#   auth   — интерактивное получение токена по сценарию OAuth 2.0 Device Flow.
#
# В режиме serve поток stdout зарезервирован под протокол MCP, поэтому ничего,
# кроме протокольных сообщений, в stdout не пишется. Диагностика идёт в stderr.
#
# Дополнительные аргументы JVM можно передать через переменную окружения JAVA_OPTS.

COMMAND="${1:-serve}"

case "$COMMAND" in
  serve)
    exec java ${JAVA_OPTS} -jar /app/app.jar
    ;;
  auth)
    # Интерактивное получение токена по сценарию OAuth 2.0 Device Flow.
    exec java ${JAVA_OPTS} -jar /app/app.jar auth
    ;;
  *)
    echo "Неизвестная команда: $COMMAND. Доступны: serve, auth." >&2
    exit 64
    ;;
esac
