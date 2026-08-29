package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.auth.domain.AuthSessionView

/**
 * Управляет короткоживущей интерактивной сессией Device Flow для MCP-инструментов.
 *
 * @author Sorface Developer
 */
interface AuthSessionService {

    /**
     * Создаёт новую сессию, отменяя предыдущую незавершённую сессию процесса.
     *
     * @return безопасные данные для подтверждения доступа
     */
    fun start(): AuthSessionView

    /**
     * Проверяет состояние сессии с соблюдением минимального интервала Yandex OAuth.
     *
     * @param sessionId идентификатор, возвращённый методом [start]
     * @return актуальное состояние сессии
     */
    fun poll(sessionId: String): AuthSessionView
}
