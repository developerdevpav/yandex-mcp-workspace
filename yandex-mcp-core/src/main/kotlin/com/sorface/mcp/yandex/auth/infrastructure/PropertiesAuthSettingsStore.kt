package com.sorface.mcp.yandex.auth.infrastructure

import com.sorface.mcp.yandex.auth.application.AuthSettingsStore
import com.sorface.mcp.yandex.config.UserDataPaths
import com.sorface.mcp.yandex.config.YandexProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.Properties

/**
 * Локальное хранилище настроек Spring в формате `.properties`.
 *
 * Файл содержит `client_secret`, необходимый Yandex OAuth для обновления токена, поэтому на
 * POSIX-системах ему назначаются права `600`. Токены в этот файл не записываются.
 *
 * @author Sorface Developer
 */
@Component
class PropertiesAuthSettingsStore : AuthSettingsStore {

    override val path: Path = Path.of(
        System.getenv("YANDEX_CONFIG_PATH")?.takeIf { it.isNotBlank() }
            ?: UserDataPaths.defaultConfigPath(),
    )

    override fun save(properties: YandexProperties) {
        require(properties.clientId.isNotBlank()) { "Не задан client_id" }
        require(properties.clientSecret.isNotBlank()) { "Не задан client_secret" }
        require(properties.orgId.isNotBlank()) { "Не задан идентификатор организации" }

        val parent = requireNotNull(path.toAbsolutePath().parent) { "У файла настроек отсутствует каталог" }
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, ".config-", ".tmp")
        try {
            val values = Properties().apply {
                setProperty("yandex.client-id", properties.clientId)
                setProperty("yandex.client-secret", properties.clientSecret)
                setProperty("yandex.org-id", properties.orgId)
                setProperty("yandex.org-type", properties.orgType.name)
                setProperty("yandex.oauth.scopes", properties.oauth.scopes)
                setProperty("yandex.read-only", properties.readOnly.toString())
            }
            Files.newOutputStream(temporary).use { values.store(it, "Yandex MCP local settings") }
            restrictPermissions(temporary)
            runCatching {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }.getOrElse {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
            restrictPermissions(path)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    /** Ограничивает чтение файла текущим пользователем на POSIX-системах. */
    private fun restrictPermissions(target: Path) {
        runCatching {
            Files.setPosixFilePermissions(
                target,
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            )
        }
    }
}
