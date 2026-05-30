package com.sorface.mcp.yandex.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Тип организации в Яндексе. От него зависит заголовок идентификатора организации,
 * который добавляется в запросы к API Tracker и Wiki.
 *
 * @author Sorface Developer
 */
enum class OrgType {
    /** Организация Яндекс 360 для бизнеса. Используется заголовок `X-Org-ID`. */
    YANDEX_360,

    /** Организация Yandex Cloud. Используется заголовок `X-Cloud-Org-ID`. */
    YANDEX_CLOUD,
}

/**
 * Конфигурация подключения к сервисам Яндекса.
 *
 * Значения задаются через переменные окружения (для запуска в Docker) или файл настроек.
 * Префикс свойств — `yandex`.
 *
 * @property clientId идентификатор приложения Яндекс OAuth (`client_id`)
 * @property clientSecret секретный ключ приложения Яндекс OAuth (`client_secret`)
 * @property orgId идентификатор организации для заголовка `X-Org-ID`/`X-Cloud-Org-ID`
 * @property orgType тип организации, определяющий имя заголовка идентификатора организации
 * @property oauth настройки OAuth-сервиса Яндекса
 * @property tokenStorePath путь к файлу хранения токенов в подключённом томе
 * @property readOnly режим только для чтения: при `true` изменяющие инструменты отключены
 * @property retry настройки повторных запросов при временных сбоях API
 *
 * @author Sorface Developer
 */
@ConfigurationProperties(prefix = "yandex")
data class YandexProperties(
    val clientId: String = "",
    val clientSecret: String = "",
    val orgId: String = "",
    val orgType: OrgType = OrgType.YANDEX_360,
    val oauth: OAuthProperties = OAuthProperties(),
    val tokenStorePath: String = "/data/tokens.json",
    val readOnly: Boolean = false,
    val retry: RetryProperties = RetryProperties(),
) {
    /**
     * Настройки сервиса авторизации Яндекс OAuth.
     *
     * @property baseUrl базовый адрес OAuth-сервиса
     * @property scopes запрашиваемые разрешения через пробел
     */
    data class OAuthProperties(
        val baseUrl: String = "https://oauth.yandex.com",
        val scopes: String = "",
    )

    /**
     * Настройки повторных запросов к API при временных сбоях.
     *
     * Повтор выполняется только для безопасных в смысле идемпотентности ситуаций: сетевые ошибки
     * (соединение не установлено, разрыв), превышение лимита запросов (`429`) и временные ошибки
     * сервиса (`5xx`). Задержка между попытками растёт экспоненциально: `initialDelay`,
     * `initialDelay * multiplier`, и так далее, но не превышает `maxDelay`. Если ответ `429`
     * содержит заголовок `Retry-After`, используется указанное в нём время.
     *
     * @property enabled включены ли повторные запросы
     * @property maxAttempts максимальное число попыток, включая первую (значение `1` отключает повторы)
     * @property initialDelay начальная задержка перед первым повтором
     * @property multiplier множитель экспоненциального роста задержки
     * @property maxDelay верхняя граница задержки между попытками
     */
    data class RetryProperties(
        val enabled: Boolean = true,
        val maxAttempts: Int = 3,
        val initialDelay: Duration = Duration.ofMillis(500),
        val multiplier: Double = 2.0,
        val maxDelay: Duration = Duration.ofSeconds(10),
    )

    /**
     * Возвращает имя HTTP-заголовка идентификатора организации в зависимости от типа организации.
     */
    fun orgHeaderName(): String = when (orgType) {
        OrgType.YANDEX_360 -> "X-Org-ID"
        OrgType.YANDEX_CLOUD -> "X-Cloud-Org-ID"
    }
}
