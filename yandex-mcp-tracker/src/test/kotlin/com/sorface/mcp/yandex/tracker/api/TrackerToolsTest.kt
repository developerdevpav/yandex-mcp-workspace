package com.sorface.mcp.yandex.tracker.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.tracker.application.TrackerReadService
import com.sorface.mcp.yandex.tracker.domain.PagedResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Инструменты чтения Tracker (TrackerTools)")
class TrackerToolsTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val service = mockk<TrackerReadService>()
    private val tools = TrackerTools(service, objectMapper)

    @Test
    @DisplayName("tracker_myself возвращает форматированный JSON пользователя")
    fun `myself renders json`() {
        every { service.myself() } returns objectMapper.readTree("""{"login":"john"}""")

        val result = tools.myself()

        assertThat(objectMapper.readTree(result).path("login").asText()).isEqualTo("john")
    }

    @Test
    @DisplayName("tracker_issue_search оборачивает результат полями пагинации")
    fun `search wraps pagination`() {
        every {
            service.searchIssues("queue: TREK", null, null, null, null, null, 50, 2)
        } returns PagedResult(
            items = objectMapper.readTree("""[{"key":"TREK-1"}]"""),
            totalCount = 120,
            totalPages = 3,
        )

        val result = tools.issueSearch("queue: TREK", null, null, null, null, null, 50, 2)

        val node = objectMapper.readTree(result)
        assertThat(node.path("totalCount").asLong()).isEqualTo(120)
        assertThat(node.path("totalPages").asLong()).isEqualTo(3)
        assertThat(node.path("page").asInt()).isEqualTo(2)
        assertThat(node.path("perPage").asInt()).isEqualTo(50)
        assertThat(node.path("items")[0].path("key").asText()).isEqualTo("TREK-1")
    }

    @Test
    @DisplayName("tracker_issue_count возвращает число строкой")
    fun `count returns number`() {
        every { service.countIssues(null, """{"queue":"TREK"}""", null, null) } returns 42

        val result = tools.issueCount(null, """{"queue":"TREK"}""", null, null)

        assertThat(result).isEqualTo("42")
    }
}
