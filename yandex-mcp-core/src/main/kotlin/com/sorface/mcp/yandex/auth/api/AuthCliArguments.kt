package com.sorface.mcp.yandex.auth.api

/**
 * Преобразует короткие параметры CLI в свойства Spring Boot.
 *
 * Это позволяет выполнить первичную настройку одной командой без предварительного объявления
 * переменных окружения. Неизвестные параметры передаются Spring без изменений.
 *
 * @param args исходные аргументы запуска
 * @return аргументы с нормализованными именами свойств
 */
fun normalizeYandexCliArguments(args: Array<String>): Array<String> = args.map { argument ->
    SHORT_OPTIONS.entries.firstNotNullOfOrNull { (shortName, propertyName) ->
        argument.takeIf { it.startsWith("--$shortName=") }
            ?.substringAfter('=')
            ?.let { "--$propertyName=$it" }
    } ?: argument
}.toTypedArray()

private val SHORT_OPTIONS = mapOf(
    "client-id" to "yandex.client-id",
    "client-secret" to "yandex.client-secret",
    "org-id" to "yandex.org-id",
    "org-type" to "yandex.org-type",
    "scopes" to "yandex.oauth.scopes",
    "read-only" to "yandex.read-only",
)
