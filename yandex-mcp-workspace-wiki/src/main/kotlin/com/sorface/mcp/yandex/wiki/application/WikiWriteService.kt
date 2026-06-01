package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.JsonNode

/**
 * Сервис изменения данных в Yandex Wiki.
 *
 * Покрывает создание, изменение, удаление, восстановление и клонирование страниц, дополнение
 * содержимого, добавление комментариев и загрузку вложений. Содержимое страниц передаётся в
 * формате Markdown. Перед каждой операцией проверяется режим «только для чтения».
 *
 * Произвольные поля задаются JSON-объектом `fields`, что позволяет передавать любые поля API
 * без расширения сигнатуры методов.
 *
 * @author Sorface Developer
 */
interface WikiWriteService {

    /**
     * Создаёт страницу.
     *
     * @param title заголовок страницы
     * @param slug адрес страницы; если не задан, расположение определяется по `parentId`
     * @param parentId идентификатор родительской страницы
     * @param content содержимое страницы в формате Markdown
     * @param fields JSON-объект дополнительных полей
     * @return созданная страница
     */
    fun createPage(title: String, slug: String?, parentId: String?, content: String?, fields: String?): JsonNode

    /**
     * Изменяет заголовок и/или содержимое страницы.
     *
     * @param id идентификатор страницы
     * @param title новый заголовок
     * @param content новое содержимое в формате Markdown
     * @param fields JSON-объект дополнительных изменяемых полей
     * @return обновлённая страница
     */
    fun updatePage(id: String, title: String?, content: String?, fields: String?): JsonNode

    /**
     * Удаляет страницу. Ответ содержит токен восстановления.
     *
     * @param id идентификатор страницы
     * @return результат удаления с токеном восстановления
     */
    fun deletePage(id: String): JsonNode

    /**
     * Восстанавливает удалённую страницу по токену восстановления.
     *
     * @param recoveryToken токен восстановления, полученный при удалении
     * @return восстановленная страница
     */
    fun recoverPage(recoveryToken: String): JsonNode

    /**
     * Клонирует страницу в целевое расположение.
     *
     * @param id идентификатор исходной страницы
     * @param slug адрес целевого расположения
     * @param parentId идентификатор целевой родительской страницы
     * @param title заголовок клона
     * @param fields JSON-объект дополнительных полей
     * @return созданный клон
     */
    fun clonePage(id: String, slug: String?, parentId: String?, title: String?, fields: String?): JsonNode

    /**
     * Дописывает содержимое к странице.
     *
     * @param id идентификатор страницы
     * @param content добавляемое содержимое в формате Markdown
     * @param location место вставки в теле страницы: `bottom` (в конец) или `top` (в начало);
     *   взаимоисключимо с `anchor`
     * @param anchor имя якоря для вставки (например, `#heading`); взаимоисключимо с `location`
     * @return результат дополнения
     */
    fun appendContent(id: String, content: String, location: String?, anchor: String?): JsonNode

    /**
     * Добавляет комментарий к странице или ответ на комментарий.
     *
     * @param id идентификатор страницы
     * @param content текст комментария
     * @param parentId идентификатор родительского комментария для ответа
     * @return созданный комментарий
     */
    fun addComment(id: String, content: String, parentId: String?): JsonNode

    /**
     * Загружает локальный файл и прикрепляет его к странице.
     *
     * Файл читается по пути, доступному серверу (например, в подключённом томе), разбивается на
     * части и загружается через сессию загрузки, после чего сессия прикрепляется к странице.
     *
     * @param pageId идентификатор страницы
     * @param filePath путь к файлу, доступному серверу
     * @param name имя вложения; по умолчанию имя файла
     * @return результат прикрепления вложения
     */
    fun uploadAttachment(pageId: String, filePath: String, name: String?): JsonNode

    /**
     * Прикрепляет к странице уже загруженные сессии загрузки по их идентификаторам.
     *
     * @param pageId идентификатор страницы
     * @param sessionIds идентификаторы завершённых сессий загрузки через запятую
     * @return результат прикрепления
     */
    fun attachUploadSessions(pageId: String, sessionIds: String): JsonNode
}
