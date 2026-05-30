package com.sorface.mcp.yandex.common

import com.sorface.mcp.yandex.config.YandexProperties
import org.springframework.stereotype.Component

/**
 * Страж изменяющих операций.
 *
 * Единая точка проверки режима «только для чтения». Изменяющие сервисы вызывают
 * [ensureWritable] перед обращением к API; при включённом флаге `yandex.read-only`
 * операция отклоняется исключением [ReadOnlyModeException]. Вынесено в общий слой,
 * чтобы переиспользоваться будущими изменяющими сервисами (Tracker, Wiki).
 *
 * @author Sorface Developer
 */
@Component
class WriteGuard(
    private val properties: YandexProperties,
) {

    /**
     * Проверяет, что сервер не находится в режиме только для чтения.
     *
     * @param operation краткое описание операции для понятного сообщения об ошибке
     * @throws ReadOnlyModeException если включён режим только для чтения
     */
    fun ensureWritable(operation: String) {
        if (properties.readOnly) {
            throw ReadOnlyModeException(
                "Операция '$operation' недоступна: сервер запущен в режиме только для чтения (read-only).",
            )
        }
    }
}
