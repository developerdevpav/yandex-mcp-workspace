package com.sorface.mcp.yandex.auth.api

import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer
import org.springframework.boot.diagnostics.FailureAnalysis

/**
 * Преобразует ожидаемые ошибки CLI-авторизации в короткую инструкцию без stack trace.
 *
 * Ненулевой код завершения сохраняется, поэтому команду безопасно использовать в скриптах.
 * Подробности исходной ошибки остаются доступны как причина анализа Spring Boot.
 *
 * @author Sorface Developer
 */
class AuthorizationFailureAnalyzer : AbstractFailureAnalyzer<AuthorizationException>() {

    override fun analyze(
        rootFailure: Throwable,
        cause: AuthorizationException,
    ): FailureAnalysis = FailureAnalysis(
        cause.message ?: "Не удалось выполнить авторизацию в Яндексе.",
        "Запустите setup в интерактивном терминале или передайте --client-id и --client-secret. " +
            "Для диагностики используйте doctor.",
        cause,
    )
}
