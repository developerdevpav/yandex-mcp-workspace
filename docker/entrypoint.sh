#!/usr/bin/env sh
set -e

# Точка входа Docker-образа MCP-сервера.
#
# Поддерживаемые команды:
#   serve  — запуск MCP-сервера по транспорту stdio (режим по умолчанию).
#   setup/login/auth — интерактивное получение токена по сценарию OAuth 2.0 Device Flow.
#   logout — удалить локальные токены.
#   doctor — показать безопасную диагностику авторизации.
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
  setup|login|auth|logout|doctor)
    exec java ${JAVA_OPTS} -jar /app/app.jar "$COMMAND"
    ;;
  *)
    echo "Неизвестная команда: $COMMAND. Доступны: serve, setup, login, auth, logout, doctor." >&2
    exit 64
    ;;
esac
