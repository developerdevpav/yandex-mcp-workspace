package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.JsonNode

/**
 * Сервис изменения данных в Yandex Tracker.
 *
 * Покрывает создание и изменение задач, переходы по статусам, перенос между очередями,
 * а также работу с комментариями и связями задач. Перед каждой операцией проверяется режим
 * «только для чтения»: при включённом флаге `yandex.read-only` операция отклоняется.
 *
 * Произвольные и пользовательские поля задаются JSON-объектом `fields`, что позволяет
 * передавать любые поля Tracker, включая пользовательские, без расширения сигнатуры методов.
 *
 * @author Sorface Developer
 */
interface TrackerWriteService {

    /**
     * Создаёт задачу.
     *
     * @param queue ключ очереди, например `TREK`
     * @param summary название задачи
     * @param description описание задачи
     * @param type тип задачи (ключ или идентификатор), например `bug`
     * @param priority приоритет (ключ или идентификатор), например `normal`
     * @param assignee логин или идентификатор исполнителя
     * @param parent ключ родительской задачи
     * @param fields JSON-объект дополнительных полей, включая пользовательские
     * @return созданная задача
     */
    fun createIssue(
        queue: String,
        summary: String,
        description: String?,
        type: String?,
        priority: String?,
        assignee: String?,
        parent: String?,
        fields: String?,
    ): JsonNode

    /**
     * Изменяет поля задачи.
     *
     * @param key ключ задачи, например `TREK-42`
     * @param summary новое название задачи
     * @param description новое описание задачи
     * @param priority новый приоритет
     * @param assignee новый исполнитель
     * @param fields JSON-объект дополнительных изменяемых полей
     * @param version номер версии задачи для защиты от конфликтов изменений
     * @return обновлённая задача
     */
    fun updateIssue(
        key: String,
        summary: String?,
        description: String?,
        priority: String?,
        assignee: String?,
        fields: String?,
        version: Int?,
    ): JsonNode

    /**
     * Переносит задачу в другую очередь.
     *
     * @param key ключ задачи
     * @param targetQueue ключ очереди назначения
     * @param fields JSON-объект полей, задаваемых при переносе (например, новый тип)
     * @return перенесённая задача
     */
    fun moveIssue(key: String, targetQueue: String, fields: String?): JsonNode

    /**
     * Выполняет переход задачи по статусу.
     *
     * @param key ключ задачи
     * @param transitionId идентификатор перехода (из списка доступных переходов)
     * @param comment комментарий, добавляемый при переходе
     * @param fields JSON-объект полей, задаваемых при переходе (например, резолюция)
     * @return результат перехода (список доступных переходов после изменения)
     */
    fun executeTransition(key: String, transitionId: String, comment: String?, fields: String?): JsonNode

    /**
     * Добавляет комментарий к задаче.
     *
     * @param key ключ задачи
     * @param text текст комментария
     * @param summonees логины пользователей через запятую для призыва в комментарии
     * @return созданный комментарий
     */
    fun addComment(key: String, text: String, summonees: String?): JsonNode

    /**
     * Изменяет текст комментария.
     *
     * @param key ключ задачи
     * @param commentId идентификатор комментария
     * @param text новый текст комментария
     * @return обновлённый комментарий
     */
    fun updateComment(key: String, commentId: String, text: String): JsonNode

    /**
     * Удаляет комментарий задачи.
     *
     * @param key ключ задачи
     * @param commentId идентификатор комментария
     */
    fun deleteComment(key: String, commentId: String)

    /**
     * Создаёт связь между задачами.
     *
     * @param key ключ исходной задачи
     * @param relationship тип связи, например `relates`, `depends on`, `is subtask for`
     * @param issue ключ связываемой задачи
     * @return созданная связь
     */
    fun createLink(key: String, relationship: String, issue: String): JsonNode

    /**
     * Удаляет связь задачи.
     *
     * @param key ключ задачи
     * @param linkId идентификатор связи
     */
    fun deleteLink(key: String, linkId: String)

    /**
     * Добавляет пункт в чек-лист задачи.
     *
     * @param key ключ задачи
     * @param text текст пункта
     * @param checked признак выполнения
     * @param assignee логин или идентификатор исполнителя пункта
     * @param deadline JSON-объект дедлайна, например `{"date":"2021-05-09T00:00:00.000+0000","deadlineType":"date"}`
     * @param fields JSON-объект дополнительных полей
     * @return обновлённая задача с чек-листом
     */
    fun addChecklistItem(
        key: String,
        text: String,
        checked: Boolean?,
        assignee: String?,
        deadline: String?,
        fields: String?,
    ): JsonNode

    /**
     * Изменяет пункт чек-листа задачи.
     *
     * @param key ключ задачи
     * @param itemId идентификатор пункта чек-листа
     * @param text новый текст пункта
     * @param checked новый признак выполнения
     * @param assignee новый исполнитель пункта
     * @param deadline JSON-объект дедлайна
     * @param fields JSON-объект дополнительных полей
     * @return обновлённая задача с чек-листом
     */
    fun updateChecklistItem(
        key: String,
        itemId: String,
        text: String?,
        checked: Boolean?,
        assignee: String?,
        deadline: String?,
        fields: String?,
    ): JsonNode

    /**
     * Удаляет пункт чек-листа задачи.
     *
     * @param key ключ задачи
     * @param itemId идентификатор пункта чек-листа
     * @return обновлённая задача с чек-листом
     */
    fun deleteChecklistItem(key: String, itemId: String): JsonNode

    /**
     * Добавляет запись учёта времени (worklog) к задаче.
     *
     * @param key ключ задачи
     * @param start дата и время начала работы в формате ISO 8601
     * @param duration затраченное время в формате ISO 8601 (например `PT2H30M`)
     * @param comment комментарий к записи
     * @param fields JSON-объект дополнительных полей
     * @return созданная запись worklog
     */
    fun addWorklog(
        key: String,
        start: String?,
        duration: String,
        comment: String?,
        fields: String?,
    ): JsonNode

    /**
     * Изменяет запись учёта времени задачи.
     *
     * @param key ключ задачи
     * @param worklogId идентификатор записи worklog
     * @param start новое время начала работы
     * @param duration новая длительность
     * @param comment новый комментарий
     * @param fields JSON-объект дополнительных полей
     * @return обновлённая запись worklog
     */
    fun updateWorklog(
        key: String,
        worklogId: String,
        start: String?,
        duration: String?,
        comment: String?,
        fields: String?,
    ): JsonNode

    /**
     * Удаляет запись учёта времени задачи.
     *
     * @param key ключ задачи
     * @param worklogId идентификатор записи worklog
     */
    fun deleteWorklog(key: String, worklogId: String)
}
