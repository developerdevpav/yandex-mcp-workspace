package com.sorface.mcp.yandex.auth.api

import com.sorface.mcp.yandex.auth.application.AuthService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Команда интерактивного получения токена по сценарию OAuth 2.0 Device Flow.
 *
 * Запускается только в профиле `auth` (контейнер вызывается командой `auth`). В этом режиме
 * MCP-сервер не стартует. Все сообщения для пользователя выводятся в stderr, чтобы не мешать
 * протоколу MCP в обычном режиме.
 *
 * Шаги:
 * 1. Запросить коды устройства и пользователя.
 * 2. Показать адрес подтверждения и код пользователя.
 * 3. Дождаться подтверждения и сохранить токены.
 *
 * @author Sorface Developer
 */
@Component
@Profile("auth")
class AuthCommandRunner(
    private val authService: AuthService,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val authorization = authService.beginDeviceAuthorization()

        System.err.println("======================================================")
        System.err.println("Подтверждение доступа Яндекс OAuth (Device Flow)")
        System.err.println("Откройте адрес: ${authorization.verificationUrl}")
        System.err.println("Введите код:    ${authorization.userCode}")
        System.err.println("Ожидание подтверждения...")
        System.err.println("======================================================")

        val tokenSet = authService.completeDeviceAuthorization(authorization)

        System.err.println("Авторизация успешна. Токен сохранён, действует до ${tokenSet.expiresAt}.")
        logger.info("Токен получен и сохранён в хранилище")
    }
}
