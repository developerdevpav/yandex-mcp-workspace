package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.JsonNode
import com.sorface.mcp.yandex.tracker.domain.PagedResult

/**
 * Сервис чтения данных из Yandex Tracker.
 *
 * Объединяет операции получения и поиска задач, очередей, справочников и данных текущего
 * пользователя. Возвращает данные в виде [JsonNode], сохраняя структуру ответа API без
 * потери произвольных и пользовательских полей. Бизнес-нейтральная сборка тела запросов
 * (фильтры поиска, параметры пагинации) выполняется здесь, технические детали HTTP — в слое
 * инфраструктуры.
 *
 * @author Sorface Developer
 */
interface TrackerReadService {

    /**
     * Возвращает данные текущего пользователя, которому принадлежит токен.
     *
     * @return объект пользователя Tracker
     */
    fun myself(): JsonNode

    /**
     * Возвращает задачу по её ключу.
     *
     * @param key ключ задачи, например `TREK-42`
     * @param expand дополнительные блоки данных через запятую (`transitions`, `attachments`)
     * @return объект задачи
     */
    fun getIssue(key: String, expand: String?): JsonNode

    /**
     * Ищет задачи по фильтру или языку запросов Tracker.
     *
     * @param query строка на языке запросов Tracker; используется самостоятельно
     * @param filter JSON-объект структурного фильтра (например `{"queue":"TREK"}`)
     * @param queue ключ очереди для ограничения поиска
     * @param keys ключи задач через запятую для точечной выборки
     * @param order поле и направление сортировки, например `+status`
     * @param expand дополнительные блоки данных через запятую
     * @param perPage число задач на страницу
     * @param page номер страницы, начиная с 1
     * @return страница задач со сведениями о пагинации
     */
    fun searchIssues(
        query: String?,
        filter: String?,
        queue: String?,
        keys: String?,
        order: String?,
        expand: String?,
        perPage: Int?,
        page: Int?,
    ): PagedResult

    /**
     * Считает количество задач, удовлетворяющих фильтру или запросу.
     *
     * @param query строка на языке запросов Tracker; используется самостоятельно
     * @param filter JSON-объект структурного фильтра
     * @param queue ключ очереди для ограничения подсчёта
     * @param keys ключи задач через запятую
     * @return количество задач
     */
    fun countIssues(query: String?, filter: String?, queue: String?, keys: String?): Long

    /**
     * Возвращает список очередей.
     *
     * @param expand дополнительные блоки данных через запятую (`all`, `projects`)
     * @param perPage число очередей на страницу
     * @param page номер страницы, начиная с 1
     * @return страница очередей со сведениями о пагинации
     */
    fun listQueues(expand: String?, perPage: Int?, page: Int?): PagedResult

    /**
     * Возвращает параметры очереди по её идентификатору или ключу.
     *
     * @param id идентификатор или ключ очереди, например `TREK`
     * @param expand дополнительные блоки данных через запятую
     * @return объект очереди
     */
    fun getQueue(id: String, expand: String?): JsonNode

    /**
     * Возвращает список типов задач.
     *
     * @return массив типов задач
     */
    fun listIssueTypes(): JsonNode

    /**
     * Возвращает список приоритетов.
     *
     * @return массив приоритетов
     */
    fun listPriorities(): JsonNode

    /**
     * Возвращает список статусов задач.
     *
     * @return массив статусов
     */
    fun listStatuses(): JsonNode

    /**
     * Возвращает список резолюций задач.
     *
     * @return массив резолюций
     */
    fun listResolutions(): JsonNode

    /**
     * Возвращает список доступных переходов по статусам для задачи.
     *
     * @param key ключ задачи, например `TREK-42`
     * @return массив переходов
     */
    fun listTransitions(key: String): JsonNode

    /**
     * Возвращает историю изменений задачи.
     *
     * @param key ключ задачи
     * @param field идентификатор поля для фильтрации записей истории, если задан
     * @param type тип изменения для фильтрации (`IssueUpdated`, `IssueWorkflow` и т. п.), если задан
     * @param perPage число записей на страницу
     * @return страница записей истории со сведениями о пагинации
     */
    fun getChangelog(key: String, field: String?, type: String?, perPage: Int?): PagedResult

    /**
     * Возвращает список комментариев задачи.
     *
     * @param key ключ задачи
     * @param expand дополнительные блоки данных через запятую (`attachments`, `html`)
     * @return массив комментариев
     */
    fun listComments(key: String, expand: String?): JsonNode

    /**
     * Возвращает список связей задачи.
     *
     * @param key ключ задачи
     * @return массив связей
     */
    fun listLinks(key: String): JsonNode
}
