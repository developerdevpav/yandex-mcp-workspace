package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.common.ApiException
import com.sorface.mcp.yandex.tracker.infrastructure.TrackerClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Сервис чтения Entities API Tracker")
class DefaultTrackerEntityReadServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val client = mockk<TrackerClient>()
    private val service = DefaultTrackerEntityReadService(client, objectMapper)

    @Test
    @DisplayName("Получение сущности нормализует поля и сохраняет строковый shortId")
    fun `get normalizes fields and keeps string id`() {
        val query = slot<Map<String, String?>>()
        every { client.get("/v3/entities/project/42", capture(query)) } returns objectMapper.readTree(
            """{"id":"abc","shortId":42,"custom":{"unknown":true}}""",
        )

        val result = service.get("project", "42", " summary, description,summary, ", "attachments")

        assertThat(query.captured["fields"]).isEqualTo("summary,description")
        assertThat(result.path("custom").path("unknown").asBoolean()).isTrue()
    }

    @Test
    @DisplayName("Поиск формирует тело и нормализует values, hits и pages")
    fun `search normalizes response`() {
        val body = slot<Any>()
        val query = slot<Map<String, String?>>()
        every {
            client.post("/v3/entities/goal/_search", capture(body), capture(query))
        } returns objectMapper.readTree(
            """{"hits":8,"pages":2,"values":[{"id":"g1","extra":"kept"}]}""",
        )

        val result = service.search(
            entityType = "goal",
            input = "okr",
            filter = """{"entityStatus":"at_risk"}""",
            orderBy = "entityStatus",
            orderAsc = true,
            rootOnly = false,
            fields = "summary",
            perPage = 5,
            page = 2,
        )

        val request = body.captured as ObjectNode
        assertThat(request.path("filter").path("entityStatus").asText()).isEqualTo("at_risk")
        assertThat(request.path("rootOnly").asBoolean()).isFalse()
        assertThat(query.captured["page"]).isEqualTo("2")
        assertThat(result.path("totalCount").asLong()).isEqualTo(8)
        assertThat(result.path("items")[0].path("extra").asText()).isEqualTo("kept")
    }

    @Test
    @DisplayName("from и selected отклоняются до запроса истории")
    fun `relative pagination rejects from and selected`() {
        assertThatThrownBy {
            service.listEvents("project", "p1", 50, "e1", "e2", false, "forward")
        }.isInstanceOf(ApiException::class.java)

        verify(exactly = 0) { client.get(any(), any()) }
    }

    @Test
    @DisplayName("История событий нормализуется и сохраняет полиморфные changes")
    fun `events normalize without losing changes`() {
        every {
            client.get("/v3/entities/project/p1/events/_relative", any())
        } returns objectMapper.readTree(
            """{"events":[{"id":"e1","changes":[{"customDiff":{"x":1}}]}],"hasNext":true,"hasPrev":false}""",
        )

        val result = service.listEvents("project", "p1", null, null, null, true, "backward")

        assertThat(result.path("hasNext").asBoolean()).isTrue()
        assertThat(result.path("items")[0].path("changes")[0].path("customDiff").path("x").asInt())
            .isEqualTo(1)
    }

    @Test
    @DisplayName("Чек-лист запрещён для цели")
    fun `goal checklist is rejected`() {
        assertThatThrownBy { service.listChecklist("goal", "g1") }
            .isInstanceOf(ApiException::class.java)
            .hasMessageContaining("недоступен")
    }

    @Test
    @DisplayName("Ключевые результаты извлекаются из fields сущности")
    fun `key results are extracted from entity`() {
        every {
            client.get("/v3/entities/goal/g1", match { it["fields"] == "keyResultItems" })
        } returns objectMapper.readTree("""{"fields":{"keyResultItems":[{"id":"kr1"}]}}""")

        val result = service.listGoalKeyResults("g1")

        assertThat(result[0].path("id").asText()).isEqualTo("kr1")
    }
}
