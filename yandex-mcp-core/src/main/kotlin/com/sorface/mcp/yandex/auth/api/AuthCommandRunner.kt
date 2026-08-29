package com.sorface.mcp.yandex.auth.api

import com.sorface.mcp.yandex.auth.application.AuthService
import com.sorface.mcp.yandex.auth.application.AuthSettingsStore
import com.sorface.mcp.yandex.auth.application.BrowserLauncher
import com.sorface.mcp.yandex.auth.domain.AuthorizationException
import com.sorface.mcp.yandex.config.OrgType
import com.sorface.mcp.yandex.config.YandexProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Команда интерактивного получения токена по сценарию OAuth 2.0 Device Flow.
 *
 * Запускается только в профиле `auth`, который активируют команды `setup`, `login`, `auth`,
 * `logout` и `doctor`. В этом режиме MCP-сервер не стартует. Все сообщения для пользователя
 * выводятся в stderr, чтобы не мешать протоколу MCP в обычном режиме.
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
    private val browserLauncher: BrowserLauncher,
    private val settingsStore: AuthSettingsStore,
    private val properties: YandexProperties,
    private val setupPrompter: AuthSetupPrompter,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        when (args.nonOptionArgs.firstOrNull()?.lowercase() ?: "login") {
            "auth", "login" -> login()
            "setup" -> setup()
            "logout" -> logout()
            "doctor" -> doctor()
            else -> throw AuthorizationException(
                "Неизвестная команда. Доступны: setup, login, auth, logout, doctor.",
            )
        }
    }

    /** Авторизует пользователя и сохраняет настройки для следующих запусков. */
    private fun setup() {
        promptForMissingSettings()
        login()
        settingsStore.save(properties)
        System.err.println("Настройки сохранены: ${settingsStore.path}")
    }

    /** Запрашивает только те обязательные настройки, которых нет в файле или параметрах запуска. */
    private fun promptForMissingSettings() {
        if (properties.clientId.isBlank() || properties.clientSecret.isBlank() || properties.orgId.isBlank()) {
            System.err.println("Первичная настройка Yandex MCP")
            System.err.println("Создать или открыть OAuth-приложение: https://oauth.yandex.ru/")
        }

        if (properties.clientId.isBlank()) {
            properties.clientId = requireInput("OAuth client_id: ")
        }
        if (properties.clientSecret.isBlank()) {
            properties.clientSecret = requireInput("OAuth client_secret: ", secret = true)
        }
        if (properties.orgId.isBlank()) {
            properties.orgId = requireInput("Идентификатор организации: ")
            val type = setupPrompter.read(
                "Тип организации [YANDEX_360/YANDEX_CLOUD, Enter = YANDEX_360]: ",
            ).orEmpty()
            if (type.isNotBlank()) {
                properties.orgType = runCatching { OrgType.valueOf(type.uppercase()) }
                    .getOrElse {
                        throw AuthorizationException(
                            "Неизвестный тип организации '$type'. " +
                                "Допустимы YANDEX_360 и YANDEX_CLOUD.",
                        )
                    }
            }
        }
    }

    /** Читает обязательное значение и формирует понятную ошибку при запуске без терминала. */
    private fun requireInput(prompt: String, secret: Boolean = false): String =
        setupPrompter.read(prompt, secret).orEmpty().takeIf { it.isNotBlank() }
            ?: throw AuthorizationException(
                "Не удалось прочитать '$prompt'. Запустите setup в терминале или передайте значение параметром CLI.",
            )

    /** Выполняет интерактивную авторизацию и сохраняет токены. */
    private fun login() {
        val authorization = authService.beginDeviceAuthorization()
        val browserOpened = browserLauncher.open(authorization.verificationUrl)

        System.err.println("======================================================")
        System.err.println("Подтверждение доступа Яндекс OAuth (Device Flow)")
        System.err.println("Откройте адрес: ${authorization.verificationUrl}")
        System.err.println("Введите код:    ${authorization.userCode}")
        System.err.println(
            if (browserOpened) "Браузер открыт автоматически."
            else "Браузер открыть не удалось — откройте адрес вручную.",
        )
        System.err.println("Ожидание подтверждения...")
        System.err.println("======================================================")

        val tokenSet = authService.completeDeviceAuthorization(authorization)

        System.err.println("Авторизация успешна. Токен сохранён, действует до ${tokenSet.expiresAt}.")
        logger.info("Токен получен и сохранён в хранилище")
    }

    /** Удаляет локальный набор токенов. */
    private fun logout() {
        authService.logout()
        System.err.println("Локальные OAuth-токены удалены.")
    }

    /** Выводит безопасную диагностику конфигурации и токена. */
    private fun doctor() {
        val status = authService.status()
        System.err.println("настройки API заданы: ${if (status.configured) "да" else "нет"}")
        System.err.println("авторизован: ${if (status.authorized) "да" else "нет"}")
        System.err.println("токен истекает: ${status.expiresAt ?: "—"}")
        System.err.println("заголовок организации: ${status.orgHeader}")
        System.err.println("режим только для чтения: ${if (status.readOnly) "да" else "нет"}")
        System.err.println("локальные настройки: ${settingsStore.path}")
    }
}
