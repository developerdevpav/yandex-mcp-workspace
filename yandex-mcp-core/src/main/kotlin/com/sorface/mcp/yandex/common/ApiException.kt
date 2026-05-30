package com.sorface.mcp.yandex.common

/**
 * Ошибка обращения к REST API Яндекса (Tracker или Wiki).
 *
 * Несёт исходный HTTP-статус и человекочитаемое сообщение, в которое уже включены
 * сведения из тела ответа API. Бросается слоем инфраструктуры после разбора ответа,
 * чтобы инструменты и сервисы работали с понятной ошибкой, а не с деталями транспорта.
 *
 * @property status HTTP-статус ответа, вызвавшего ошибку
 *
 * @author Sorface Developer
 */
class ApiException(
    val status: Int,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
