package com.sorface.mcp.yandex.common

/**
 * Ошибка попытки изменения данных при включённом режиме «только для чтения».
 *
 * Бросается перед обращением к изменяющему методу API, если сервер запущен с флагом
 * `yandex.read-only=true`. Это защищает от случайных изменений, когда сервер должен
 * работать в безопасном режиме чтения.
 *
 * @author Sorface Developer
 */
class ReadOnlyModeException(message: String) : RuntimeException(message)
