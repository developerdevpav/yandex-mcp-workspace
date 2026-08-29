package com.sorface.mcp.yandex.auth.application

import com.sorface.mcp.yandex.config.YandexProperties
import java.nio.file.Path

/**
 * Сохраняет настройки подключения, введённые однократно командой `setup`.
 *
 * @author Sorface Developer
 */
interface AuthSettingsStore {

    /** Путь к локальному файлу настроек. */
    val path: Path

    /**
     * Сохраняет настройки OAuth и организации без токенов.
     *
     * @param properties проверенные настройки текущего запуска
     */
    fun save(properties: YandexProperties)
}
