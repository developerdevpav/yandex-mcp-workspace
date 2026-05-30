package com.sorface.mcp.yandex.auth.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.auth.application.TokenStore
import com.sorface.mcp.yandex.auth.domain.TokenSet
import com.sorface.mcp.yandex.config.YandexProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Файловое хранилище токенов в подключённом томе.
 *
 * Токены сохраняются в JSON-файле по пути [YandexProperties.tokenStorePath]. Доступ к файлу
 * защищён блокировкой чтения-записи. Там, где это поддерживается файловой системой, на файл
 * выставляются права `600`, чтобы ограничить доступ к секретам.
 *
 * @author Sorface Developer
 */
@Component
class FileTokenStore(
    private val properties: YandexProperties,
    private val objectMapper: ObjectMapper,
) : TokenStore {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val lock = ReentrantReadWriteLock()
    private val path: Path get() = Path.of(properties.tokenStorePath)

    override fun load(): TokenSet? = lock.read {
        if (!Files.exists(path)) {
            return null
        }
        runCatching { objectMapper.readValue(Files.readAllBytes(path), TokenSet::class.java) }
            .onFailure { logger.warn("Не удалось прочитать файл токенов {}: {}", path, it.message) }
            .getOrNull()
    }

    override fun save(tokenSet: TokenSet): Unit = lock.write {
        path.parent?.let { Files.createDirectories(it) }
        objectMapper.writeValue(Files.newOutputStream(path), tokenSet)
        restrictPermissions(path)
    }

    override fun clear(): Unit = lock.write {
        Files.deleteIfExists(path)
    }

    /**
     * Ограничивает права доступа к файлу токенов значением `600` на POSIX-системах.
     * На системах без поддержки POSIX-прав шаг пропускается.
     */
    private fun restrictPermissions(target: Path) {
        runCatching {
            val permissions = PosixFilePermissions.asFileAttribute(
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            ).value()
            Files.setPosixFilePermissions(target, permissions)
        }.onFailure {
            logger.debug("Права POSIX для файла токенов не выставлены: {}", it.message)
        }
    }
}
