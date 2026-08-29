package com.sorface.mcp.yandex.config

import java.nio.file.Path
import java.util.Locale

/**
 * Вычисляет локальные пути данных приложения по соглашениям операционной системы.
 *
 * @author Sorface Developer
 */
object UserDataPaths {

    /** Возвращает путь к локальному файлу настроек, импортируемому Spring Boot. */
    fun defaultConfigPath(): String = Path.of(
        System.getProperty("user.home", "."),
        ".config",
        "yandex-mcp",
        "config.properties",
    ).toString()

    /**
     * Возвращает путь к локальному файлу токенов для запуска без Docker.
     *
     * Docker-образы по-прежнему задают `YANDEX_TOKEN_STORE_PATH=/data/tokens.json`.
     */
    fun defaultTokenStorePath(): String {
        val home = System.getProperty("user.home", ".")
        val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        val directory = when {
            os.contains("mac") -> Path.of(home, "Library", "Application Support", "yandex-mcp")
            os.contains("win") -> Path.of(
                System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() } ?: home,
                "yandex-mcp",
            )
            else -> Path.of(
                System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }
                    ?: Path.of(home, ".local", "state").toString(),
                "yandex-mcp",
            )
        }
        return directory.resolve("tokens.json").toString()
    }
}
