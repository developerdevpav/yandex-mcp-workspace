package com.sorface.mcp.yandex.auth.infrastructure

import com.sorface.mcp.yandex.auth.application.BrowserLauncher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Desktop
import java.net.URI
import java.util.Locale

/**
 * Открывает URL стандартным браузером через Desktop API или системную команду.
 *
 * Ошибка открытия не прерывает Device Flow: MCP всё равно возвращает пользователю URL и код.
 * Это важно для Docker, SSH и других окружений без графической сессии.
 *
 * @author Sorface Developer
 */
@Component
class SystemBrowserLauncher : BrowserLauncher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun open(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrElse {
            logger.debug("Некорректный URL браузера: {}", it.message)
            return false
        }
        if (uri.scheme !in ALLOWED_SCHEMES) return false

        val openedWithDesktop = runCatching {
            if (!Desktop.isDesktopSupported()) return@runCatching false
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Desktop.Action.BROWSE)) return@runCatching false
            desktop.browse(uri)
            true
        }.getOrDefault(false)
        if (openedWithDesktop) {
            return true
        }

        val command = fallbackCommand(uri.toASCIIString()) ?: return false
        return runCatching { ProcessBuilder(command).start() }
            .onFailure { logger.debug("Не удалось открыть браузер: {}", it.message) }
            .isSuccess
    }

    /** Выбирает системную команду без передачи URL через командную оболочку. */
    private fun fallbackCommand(url: String): List<String>? {
        val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        return when {
            os.contains("mac") -> listOf("open", url)
            os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
            os.contains("linux") || os.contains("unix") -> listOf("xdg-open", url)
            else -> null
        }
    }

    private companion object {
        val ALLOWED_SCHEMES = setOf("http", "https")
    }
}
