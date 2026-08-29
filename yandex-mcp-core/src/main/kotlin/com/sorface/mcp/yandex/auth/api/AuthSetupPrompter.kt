package com.sorface.mcp.yandex.auth.api

import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Arrays

/**
 * Вводит отсутствующие параметры первичной настройки из терминала.
 *
 * Интерфейс отделяет CLI от системной консоли и позволяет проверять диалог без настоящего TTY.
 */
interface AuthSetupPrompter {
    /** Возвращает введённое значение или `null`, если стандартный ввод недоступен. */
    fun read(prompt: String, secret: Boolean = false): String?
}

/** Системная реализация с маскировкой секрета, когда процесс запущен в настоящем терминале. */
@Component
class SystemAuthSetupPrompter : AuthSetupPrompter {

    private val fallbackReader by lazy { BufferedReader(InputStreamReader(System.`in`)) }

    override fun read(prompt: String, secret: Boolean): String? {
        val console = System.console()
        if (console != null) {
            if (!secret) return console.readLine("%s", prompt)?.trim()

            val password = console.readPassword("%s", prompt) ?: return null
            return try {
                String(password).trim()
            } finally {
                Arrays.fill(password, '\u0000')
            }
        }

        System.err.print(prompt)
        if (secret) {
            System.err.println("(ввод не скрыт: процесс запущен без TTY)")
            System.err.print("> ")
        }
        return fallbackReader.readLine()?.trim()
    }
}
