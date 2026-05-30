package com.sorface.mcp.yandex.common

import org.springframework.http.HttpStatusCode

/**
 * Переводчик ошибок HTTP в [ApiException] с понятным сообщением.
 *
 * Единая точка сопоставления статусов с текстом для всех клиентов внешних API Яндекса
 * (Tracker, Wiki). Конкретные клиенты извлекают подробности из тела ответа в своём формате
 * и передают их сюда вместе с именем сервиса.
 *
 * @author Sorface Developer
 */
object ApiErrorTranslator {

    /**
     * Формирует [ApiException] по статусу ответа, имени сервиса и извлечённым подробностям.
     *
     * @param service имя сервиса для подстановки в сообщение, например `Tracker` или `Wiki`
     * @param status HTTP-статус ответа
     * @param details подробности ошибки из тела ответа; могут быть пустыми
     * @return исключение с понятным сообщением и исходным статусом
     */
    fun translate(service: String, status: HttpStatusCode, details: String): ApiException {
        val prefix = when (status.value()) {
            400 -> "Некорректный запрос к API $service"
            401 -> "Не авторизовано: токен недействителен или истёк, выполните авторизацию командой 'auth'"
            403 -> "Недостаточно прав для выполнения операции в $service"
            404 -> "Объект не найден в $service"
            409 -> "Конфликт изменений в $service"
            412 -> "Конфликт версий объекта $service"
            422 -> "Запрос к API $service не прошёл валидацию"
            429 -> "Превышен лимит запросов к API $service"
            in 500..599 -> "Внутренняя ошибка сервиса $service"
            else -> "Ошибка обращения к API $service"
        }
        val message = if (details.isBlank()) {
            "$prefix (HTTP ${status.value()})"
        } else {
            "$prefix: $details (HTTP ${status.value()})"
        }
        return ApiException(status.value(), message)
    }
}
