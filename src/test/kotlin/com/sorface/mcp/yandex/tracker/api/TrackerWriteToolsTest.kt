package com.sorface.mcp.yandex.tracker.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.tracker.application.TrackerWriteService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Инструменты изменения Tracker (TrackerWriteTools)")
class TrackerWriteToolsTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val service = mockk<TrackerWriteService>()
    private val tools = TrackerWriteTools(service, objectMapper)

    @Test
    @DisplayName("tracker_issue_create форматирует созданную задачу в JSON")
    fun `create renders json`() {
        every {
            service.createIssue("TREK", "x", null, null, null, null, null, null)
        } returns objectMapper.readTree("""{"key":"TREK-7"}""")

        val result = tools.issueCreate("TREK", "x", null, null, null, null, null, null)

        assertThat(objectMapper.readTree(result).path("key").asText()).isEqualTo("TREK-7")
    }

    @Test
    @DisplayName("tracker_comment_delete возвращает подтверждение удаления")
    fun `comment delete confirms`() {
        every { service.deleteComment("TREK-1", "10") } just runs

        val result = tools.commentDelete("TREK-1", "10")

        assertThat(result).contains("10").contains("TREK-1").contains("удалён")
    }

    @Test
    @DisplayName("tracker_link_delete возвращает подтверждение удаления")
    fun `link delete confirms`() {
        every { service.deleteLink("TREK-1", "55") } just runs

        val result = tools.linkDelete("TREK-1", "55")

        assertThat(result).contains("55").contains("удалена")
    }
}
