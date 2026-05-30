package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.JsonNode

/**
 * Сервис работы с динамическими таблицами (grids) Yandex Wiki.
 *
 * Покрывает чтение таблиц и списка таблиц страницы, а также изменение таблиц, строк, столбцов и
 * ячеек. Структуры тел запросов в API таблиц разнообразны (наборы строк, столбцов, ячеек со
 * значениями), поэтому изменяющие операции принимают тело как JSON-объект, который агент формирует
 * по контракту API. Перед каждой изменяющей операцией проверяется режим «только для чтения».
 *
 * @author Sorface Developer
 */
interface WikiGridService {

    /**
     * Возвращает таблицу по её идентификатору.
     *
     * @param id идентификатор таблицы
     * @return объект таблицы
     */
    fun getGrid(id: String): JsonNode

    /**
     * Возвращает список таблиц страницы.
     *
     * @param pageId идентификатор страницы
     * @return список таблиц страницы
     */
    fun listPageGrids(pageId: String): JsonNode

    /**
     * Создаёт таблицу на странице.
     *
     * @param body JSON-объект тела запроса (расположение, заголовок, структура столбцов)
     * @return созданная таблица
     */
    fun createGrid(body: String): JsonNode

    /**
     * Изменяет заголовок и/или сортировку таблицы.
     *
     * @param id идентификатор таблицы
     * @param body JSON-объект изменяемых полей
     * @return обновлённая таблица
     */
    fun updateGrid(id: String, body: String): JsonNode

    /**
     * Удаляет таблицу.
     *
     * @param id идентификатор таблицы
     * @return результат удаления
     */
    fun deleteGrid(id: String): JsonNode

    /**
     * Клонирует таблицу в целевую страницу.
     *
     * @param id идентификатор исходной таблицы
     * @param body JSON-объект с целевым расположением
     * @return созданный клон
     */
    fun cloneGrid(id: String, body: String): JsonNode

    /**
     * Добавляет строки в таблицу.
     *
     * @param id идентификатор таблицы
     * @param body JSON-объект с добавляемыми строками
     * @return результат добавления
     */
    fun addRows(id: String, body: String): JsonNode

    /**
     * Удаляет строки таблицы.
     *
     * @param id идентификатор таблицы
     * @param body JSON-объект с идентификаторами удаляемых строк
     * @return результат удаления
     */
    fun deleteRows(id: String, body: String): JsonNode

    /**
     * Перемещает строку таблицы.
     *
     * @param id идентификатор таблицы
     * @param body JSON-объект с описанием перемещения строки
     * @return результат перемещения
     */
    fun moveRow(id: String, body: String): JsonNode

    /**
     * Добавляет столбцы в таблицу.
     *
     * @param id идентификатор таблицы
     * @param body JSON-объект с добавляемыми столбцами
     * @return результат добавления
     */
    fun addColumns(id: String, body: String): JsonNode

    /**
     * Удаляет столбцы таблицы.
     *
     * @param id идентификатор таблицы
     * @param body JSON-объект с идентификаторами удаляемых столбцов
     * @return результат удаления
     */
    fun deleteColumns(id: String, body: String): JsonNode

    /**
     * Перемещает столбец таблицы.
     *
     * @param id идентификатор таблицы
     * @param body JSON-объект с описанием перемещения столбца
     * @return результат перемещения
     */
    fun moveColumn(id: String, body: String): JsonNode

    /**
     * Обновляет значения ячеек таблицы.
     *
     * @param id идентификатор таблицы
     * @param body JSON-объект с обновляемыми ячейками
     * @return результат обновления
     */
    fun updateCells(id: String, body: String): JsonNode
}
