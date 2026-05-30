package com.sorface.mcp.yandex.integration

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Тестовый HTTP-фасад для вызова MCP-инструментов через MockMvc.
 *
 * В продакшене сервер работает по stdio и не имеет MVC-контроллеров. Этот контроллер
 * существует только в профиле `integration-test` и проксирует запросы в [ToolCallbackProvider],
 * позволяя проверить полную цепочку компонентов Spring-контекста.
 */
@RestController
@Profile("integration-test")
@RequestMapping("/integration/tools")
class IntegrationTestToolController(
    private val toolCallbackProvider: ToolCallbackProvider,
) {

    /**
     * Возвращает имена зарегистрированных инструментов.
     */
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listTools(): List<String> =
        toolCallbackProvider.toolCallbacks.map { it.toolDefinition.name() }.sorted()

    /**
     * Вызывает инструмент по имени с JSON-параметрами в теле запроса.
     */
    @PostMapping("/{toolName}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun invokeTool(
        @PathVariable toolName: String,
        @RequestBody(required = false) toolInput: String?,
    ): ResponseEntity<String> {
        val callback = toolCallbackProvider.toolCallbacks.find { it.toolDefinition.name() == toolName }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Инструмент '$toolName' не зарегистрирован")

        val result = callback.call(toolInput?.takeIf { it.isNotBlank() } ?: "{}")
        return ResponseEntity.ok(result)
    }
}
