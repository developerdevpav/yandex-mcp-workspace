package com.sorface.mcp.yandex.wiki.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.wiki.application.WikiGridService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

/**
 * Инструменты MCP для работы с динамическими таблицами (grids) Yandex Wiki.
 *
 * Тонкий слой: принимает параметры от агента, делегирует [WikiGridService] и форматирует
 * результат в JSON-текст. Изменяющие операции принимают тело запроса как строку JSON-объекта,
 * которую агент формирует по контракту API таблиц. Изменяющие инструменты блокируются в режиме
 * только для чтения на уровне сервиса.
 *
 * @author Sorface Developer
 */
@Component
class WikiGridTools(
    private val wikiGridService: WikiGridService,
    private val objectMapper: ObjectMapper,
) {

    @Tool(
        name = "wiki_grid_get",
        description = "Возвращает динамическую таблицу Wiki по её идентификатору.",
    )
    fun gridGet(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
    ): String = render(wikiGridService.getGrid(id))

    @Tool(
        name = "wiki_page_grids_list",
        description = "Возвращает список динамических таблиц страницы Wiki.",
    )
    fun pageGridsList(
        @ToolParam(description = "Идентификатор страницы")
        pageId: String,
    ): String = render(wikiGridService.listPageGrids(pageId))

    @Tool(
        name = "wiki_grid_create",
        description = "Создаёт динамическую таблицу на странице Wiki. Тело — JSON-объект с " +
            "расположением, заголовком и структурой столбцов.",
    )
    fun gridCreate(
        @ToolParam(description = "JSON-объект тела запроса создания таблицы")
        body: String,
    ): String = render(wikiGridService.createGrid(body))

    @Tool(
        name = "wiki_grid_update",
        description = "Изменяет заголовок и/или сортировку таблицы Wiki. Тело — JSON-объект полей.",
    )
    fun gridUpdate(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
        @ToolParam(description = "JSON-объект изменяемых полей таблицы")
        body: String,
    ): String = render(wikiGridService.updateGrid(id, body))

    @Tool(
        name = "wiki_grid_delete",
        description = "Удаляет динамическую таблицу Wiki.",
    )
    fun gridDelete(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
    ): String = render(wikiGridService.deleteGrid(id))

    @Tool(
        name = "wiki_grid_clone",
        description = "Клонирует таблицу Wiki в целевую страницу. Тело — JSON-объект с расположением.",
    )
    fun gridClone(
        @ToolParam(description = "Идентификатор исходной таблицы")
        id: String,
        @ToolParam(description = "JSON-объект с целевым расположением клона")
        body: String,
    ): String = render(wikiGridService.cloneGrid(id, body))

    @Tool(
        name = "wiki_grid_add_rows",
        description = "Добавляет строки в таблицу Wiki. Тело — JSON-объект с добавляемыми строками.",
    )
    fun gridAddRows(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
        @ToolParam(description = "JSON-объект с добавляемыми строками")
        body: String,
    ): String = render(wikiGridService.addRows(id, body))

    @Tool(
        name = "wiki_grid_delete_rows",
        description = "Удаляет строки таблицы Wiki. Тело — JSON-объект с идентификаторами строк.",
    )
    fun gridDeleteRows(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
        @ToolParam(description = "JSON-объект с идентификаторами удаляемых строк")
        body: String,
    ): String = render(wikiGridService.deleteRows(id, body))

    @Tool(
        name = "wiki_grid_move_row",
        description = "Перемещает строку таблицы Wiki. Тело — JSON-объект с описанием перемещения.",
    )
    fun gridMoveRow(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
        @ToolParam(description = "JSON-объект с описанием перемещения строки")
        body: String,
    ): String = render(wikiGridService.moveRow(id, body))

    @Tool(
        name = "wiki_grid_add_columns",
        description = "Добавляет столбцы в таблицу Wiki. Тело — JSON-объект с добавляемыми столбцами.",
    )
    fun gridAddColumns(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
        @ToolParam(description = "JSON-объект с добавляемыми столбцами")
        body: String,
    ): String = render(wikiGridService.addColumns(id, body))

    @Tool(
        name = "wiki_grid_delete_columns",
        description = "Удаляет столбцы таблицы Wiki. Тело — JSON-объект с идентификаторами столбцов.",
    )
    fun gridDeleteColumns(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
        @ToolParam(description = "JSON-объект с идентификаторами удаляемых столбцов")
        body: String,
    ): String = render(wikiGridService.deleteColumns(id, body))

    @Tool(
        name = "wiki_grid_move_column",
        description = "Перемещает столбец таблицы Wiki. Тело — JSON-объект с описанием перемещения.",
    )
    fun gridMoveColumn(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
        @ToolParam(description = "JSON-объект с описанием перемещения столбца")
        body: String,
    ): String = render(wikiGridService.moveColumn(id, body))

    @Tool(
        name = "wiki_grid_update_cells",
        description = "Обновляет значения ячеек таблицы Wiki. Тело — JSON-объект с обновляемыми ячейками.",
    )
    fun gridUpdateCells(
        @ToolParam(description = "Идентификатор таблицы")
        id: String,
        @ToolParam(description = "JSON-объект с обновляемыми ячейками")
        body: String,
    ): String = render(wikiGridService.updateCells(id, body))

    /**
     * Форматирует объект ответа в читаемый JSON-текст.
     */
    private fun render(node: JsonNode): String =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
}
