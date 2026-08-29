package com.sorface.mcp.yandex.auth.application

/**
 * Открывает страницу авторизации в браузере операционной системы.
 *
 * @author Sorface Developer
 */
interface BrowserLauncher {

    /**
     * Пытается открыть URL без ожидания завершения браузера.
     *
     * @param url абсолютный HTTP(S)-адрес
     * @return `true`, если команда открытия была успешно запущена
     */
    fun open(url: String): Boolean
}
