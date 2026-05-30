package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sorface.mcp.yandex.common.JsonFields
import com.sorface.mcp.yandex.common.WriteGuard
import com.sorface.mcp.yandex.wiki.infrastructure.WikiClient
import org.springframework.stereotype.Service

/**
 * Реализация сервиса таблиц Wiki поверх [WikiClient].
 *
 * Чтение выполняется напрямую, изменяющие операции защищены [WriteGuard]. Тело изменяющих
 * запросов разбирается из JSON-объекта параметра `body` помощником [JsonFields] и пересылается
 * в API без изменения структуры.
 *
 * @author Sorface Developer
 */
@Service
class DefaultWikiGridService(
    private val wikiClient: WikiClient,
    private val objectMapper: ObjectMapper,
    private val writeGuard: WriteGuard,
) : WikiGridService {

    override fun getGrid(id: String): JsonNode = wikiClient.get("/v1/grids/$id")

    override fun listPageGrids(pageId: String): JsonNode = wikiClient.get("/v1/pages/$pageId/grids")

    override fun createGrid(body: String): JsonNode {
        writeGuard.ensureWritable("создание таблицы")
        return wikiClient.post("/v1/grids", parseBody(body))
    }

    override fun updateGrid(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("изменение таблицы")
        return wikiClient.post("/v1/grids/$id", parseBody(body))
    }

    override fun deleteGrid(id: String): JsonNode {
        writeGuard.ensureWritable("удаление таблицы")
        return wikiClient.delete("/v1/grids/$id")
    }

    override fun cloneGrid(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("клонирование таблицы")
        return wikiClient.post("/v1/grids/$id/clone", parseBody(body))
    }

    override fun addRows(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("добавление строк таблицы")
        return wikiClient.post("/v1/grids/$id/rows", parseBody(body))
    }

    override fun deleteRows(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("удаление строк таблицы")
        return wikiClient.deleteWithBody("/v1/grids/$id/rows", parseBody(body))
    }

    override fun moveRow(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("перемещение строки таблицы")
        return wikiClient.post("/v1/grids/$id/rows/move", parseBody(body))
    }

    override fun addColumns(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("добавление столбцов таблицы")
        return wikiClient.post("/v1/grids/$id/columns", parseBody(body))
    }

    override fun deleteColumns(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("удаление столбцов таблицы")
        return wikiClient.deleteWithBody("/v1/grids/$id/columns", parseBody(body))
    }

    override fun moveColumn(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("перемещение столбца таблицы")
        return wikiClient.post("/v1/grids/$id/columns/move", parseBody(body))
    }

    override fun updateCells(id: String, body: String): JsonNode {
        writeGuard.ensureWritable("обновление ячеек таблицы")
        return wikiClient.post("/v1/grids/$id/cells", parseBody(body))
    }

    /**
     * Разбирает тело изменяющего запроса в JSON-объект.
     */
    private fun parseBody(body: String): ObjectNode =
        JsonFields.parseObject(objectMapper, body, "body")
}
