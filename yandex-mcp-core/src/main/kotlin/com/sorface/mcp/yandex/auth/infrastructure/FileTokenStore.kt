package com.sorface.mcp.yandex.auth.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.auth.application.TokenStore
import com.sorface.mcp.yandex.auth.domain.TokenSet
import com.sorface.mcp.yandex.config.YandexProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.nio.channels.FileChannel
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

/**
 * Файловое хранилище токенов для локального каталога или подключённого Docker-тома.
 *
 * Токены сохраняются в JSON-файле по пути [YandexProperties.tokenStorePath]. Доступ к файлу
 * защищён блокировкой чтения-записи и межпроцессной файловой блокировкой. Сохранение выполняется
 * через временный файл и атомарную замену, поэтому аварийное завершение не оставляет частично
 * записанный JSON. Там, где это поддерживается файловой системой, на файл выставляются права `600`.
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
    private val lockPath: Path get() = path.resolveSibling("${path.fileName}.lock")

    override fun load(): TokenSet? = lock.read {
        withFileLock { readUnlocked() }
    }

    override fun save(tokenSet: TokenSet): Unit = lock.write {
        withFileLock { writeUnlocked(tokenSet) }
    }

    override fun clear(): Unit = lock.write {
        withFileLock { Files.deleteIfExists(path) }
    }

    override fun update(transform: (TokenSet?) -> TokenSet?): TokenSet? = lock.write {
        withFileLock {
            val current = readUnlocked()
            val updated = transform(current)
            if (updated != current) {
                if (updated == null) Files.deleteIfExists(path) else writeUnlocked(updated)
            }
            updated
        }
    }

    /** Читает токен при уже удерживаемой файловой блокировке. */
    private fun readUnlocked(): TokenSet? {
        if (!Files.exists(path)) return null
        return runCatching { objectMapper.readValue(Files.readAllBytes(path), TokenSet::class.java) }
            .onFailure { logger.warn("Не удалось прочитать файл токенов {}: {}", path, it.message) }
            .getOrNull()
    }

    /** Атомарно записывает токен при уже удерживаемой файловой блокировке. */
    private fun writeUnlocked(tokenSet: TokenSet) {
        val parent = requireNotNull(path.toAbsolutePath().parent) { "У файла токенов отсутствует родительский каталог" }
        Files.createDirectories(parent)
        restrictDirectoryPermissions(parent)
        val temporary = Files.createTempFile(parent, ".tokens-", ".tmp")
        try {
            objectMapper.writeValue(temporary.toFile(), tokenSet)
            restrictPermissions(temporary)
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { it.force(true) }
            moveReplacing(temporary, path)
            restrictPermissions(path)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    /** Координирует чтение и обновление токена между отдельными MCP-процессами. */
    private fun <T> withFileLock(action: () -> T): T {
        lockPath.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        val normalizedLockPath = lockPath.toAbsolutePath().normalize()
        return JVM_FILE_LOCKS.computeIfAbsent(normalizedLockPath) { ReentrantLock() }.withLock {
            FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use { action() }
            }
        }
    }

    /** Атомарно заменяет файл, используя обычную замену только для ФС без ATOMIC_MOVE. */
    private fun moveReplacing(source: Path, target: Path) {
        runCatching {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
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

    /** Ограничивает просмотр каталога токенов текущим пользователем на POSIX-системах. */
    private fun restrictDirectoryPermissions(target: Path) {
        runCatching {
            Files.setPosixFilePermissions(
                target,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                ),
            )
        }.onFailure {
            logger.debug("Права POSIX для каталога токенов не выставлены: {}", it.message)
        }
    }

    private companion object {
        /** Не допускает OverlappingFileLockException между экземплярами внутри одной JVM. */
        val JVM_FILE_LOCKS = ConcurrentHashMap<Path, ReentrantLock>()
    }
}
