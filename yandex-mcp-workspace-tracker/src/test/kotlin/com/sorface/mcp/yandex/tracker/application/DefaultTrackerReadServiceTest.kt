package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.common.ApiException
import com.sorface.mcp.yandex.tracker.domain.PagedResult
import com.sorface.mcp.yandex.tracker.infrastructure.TrackerClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Сервис чтения Tracker (DefaultTrackerReadService)")
class DefaultTrackerReadServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val client = mockk<TrackerClient>()
    private val service = DefaultTrackerReadService(client, objectMapper)

    @Test
    @DisplayName("Поиск по языку запросов игнорирует структурный фильтр")
    fun `search by query ignores filter`() {
        val bodySlot = slot<Any>()
        every {
            client.postPaged("/v3/issues/_search", capture(bodySlot), any())
        } returns PagedResult(objectMapper.createArrayNode(), null, null)

        service.searchIssues(
            query = "queue: TREK",
            filter = """{"assignee":"me"}""",
            queue = "TREK",
            keys = "TREK-1",
            order = "+status",
            expand = null,
            perPage = null,
            page = null,
        )

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("query").asText()).isEqualTo("queue: TREK")
        assertThat(body.path("order").asText()).isEqualTo("+status")
        assertThat(body.has("filter")).isFalse()
        assertThat(body.has("keys")).isFalse()
    }

    @Test
    @DisplayName("Поиск по фильтру собирает filter, очередь и ключи")
    fun `search by filter builds structured body`() {
        val bodySlot = slot<Any>()
        every {
            client.postPaged("/v3/issues/_search", capture(bodySlot), any())
        } returns PagedResult(objectMapper.createArrayNode(), null, null)

        service.searchIssues(
            query = null,
            filter = """{"assignee":"me"}""",
            queue = "TREK",
            keys = "TREK-1, TREK-2",
            order = null,
            expand = null,
            perPage = null,
            page = null,
        )

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("filter").path("assignee").asText()).isEqualTo("me")
        assertThat(body.path("filter").path("queue").asText()).isEqualTo("TREK")
        assertThat(body.path("keys").map { it.asText() }).containsExactly("TREK-1", "TREK-2")
    }

    @Test
    @DisplayName("Подсчёт читает число из тела ответа")
    fun `count reads number from body`() {
        every { client.post("/v3/issues/_count", any()) } returns objectMapper.readTree("9")

        val count = service.countIssues(query = null, filter = null, queue = "TREK", keys = null)

        assertThat(count).isEqualTo(9)
    }

    @Test
    @DisplayName("Некорректный JSON фильтра приводит к ApiException")
    fun `invalid filter raises ApiException`() {
        assertThatThrownBy {
            service.searchIssues(
                query = null,
                filter = "not-json",
                queue = null,
                keys = null,
                order = null,
                expand = null,
                perPage = null,
                page = null,
            )
        }.isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("Список чек-листа обращается к checklistItems")
    fun `list checklist items calls endpoint`() {
        every {
            client.get("/v3/issues/TREK-1/checklistItems", any())
        } returns objectMapper.createArrayNode()

        service.listChecklistItems("TREK-1")

        verify { client.get("/v3/issues/TREK-1/checklistItems", any()) }
    }

    @Test
    @DisplayName("Список worklog обращается к worklog endpoint")
    fun `list worklogs calls endpoint`() {
        every { client.get("/v3/issues/TREK-1/worklog", any()) } returns objectMapper.createArrayNode()

        service.listWorklogs("TREK-1")

        verify { client.get("/v3/issues/TREK-1/worklog", any()) }
    }

    @Test
    @DisplayName("Список пользователей обращается к users endpoint")
    fun `list users calls endpoint`() {
        every { client.getPaged("/v3/users", any()) } returns PagedResult(objectMapper.createArrayNode(), null, null)

        service.listUsers(perPage = 20, page = 1)

        verify { client.getPaged("/v3/users", match { it["perPage"] == "20" && it["page"] == "1" }) }
    }

    @Test
    @DisplayName("Получение поля очереди обращается к queues fields endpoint")
    fun `list queue fields calls endpoint`() {
        every { client.get("/v3/queues/TREK/fields", any()) } returns objectMapper.createArrayNode()

        service.listQueueFields("TREK")

        verify { client.get("/v3/queues/TREK/fields", any()) }
    }
}
