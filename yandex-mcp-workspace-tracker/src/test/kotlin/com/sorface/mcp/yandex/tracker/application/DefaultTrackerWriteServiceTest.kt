package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.common.ApiException
import com.sorface.mcp.yandex.common.ReadOnlyModeException
import com.sorface.mcp.yandex.common.WriteGuard
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.tracker.infrastructure.TrackerClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Сервис изменения Tracker (DefaultTrackerWriteService)")
class DefaultTrackerWriteServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val client = mockk<TrackerClient>(relaxed = true)

    private fun service(readOnly: Boolean = false): DefaultTrackerWriteService {
        val guard = WriteGuard(YandexProperties(readOnly = readOnly))
        return DefaultTrackerWriteService(client, objectMapper, guard)
    }

    @Test
    @DisplayName("Создание задачи объединяет явные поля и JSON-объект fields")
    fun `create merges explicit and fields`() {
        val bodySlot = slot<Any>()
        every { client.post("/v3/issues", capture(bodySlot), any()) } returns objectMapper.readTree("""{"key":"TREK-1"}""")

        service().createIssue(
            queue = "TREK",
            summary = "Название",
            description = null,
            type = "bug",
            priority = null,
            assignee = null,
            parent = null,
            fields = """{"tags":["backend"],"summary":"Переопределено"}""",
        )

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("queue").asText()).isEqualTo("TREK")
        assertThat(body.path("type").asText()).isEqualTo("bug")
        assertThat(body.path("tags").map { it.asText() }).containsExactly("backend")
        assertThat(body.path("summary").asText()).isEqualTo("Переопределено")
        assertThat(body.has("description")).isFalse()
    }

    @Test
    @DisplayName("Изменение задачи прокидывает version в параметрах запроса")
    fun `update passes version query`() {
        val querySlot = slot<Map<String, String?>>()
        every { client.patch("/v3/issues/TREK-1", any(), capture(querySlot)) } returns objectMapper.readTree("{}")

        service().updateIssue(
            key = "TREK-1",
            summary = "Новое",
            description = null,
            priority = null,
            assignee = null,
            fields = null,
            version = 5,
        )

        assertThat(querySlot.captured["version"]).isEqualTo("5")
    }

    @Test
    @DisplayName("Перенос задачи передаёт целевую очередь в параметрах")
    fun `move passes target queue`() {
        val querySlot = slot<Map<String, String?>>()
        every { client.post("/v3/issues/TREK-1/_move", any(), capture(querySlot)) } returns objectMapper.readTree("{}")

        service().moveIssue("TREK-1", "NEWQ", fields = null)

        assertThat(querySlot.captured["queue"]).isEqualTo("NEWQ")
    }

    @Test
    @DisplayName("Создание связи формирует тело с типом связи и задачей")
    fun `create link builds body`() {
        val bodySlot = slot<Any>()
        every { client.post("/v3/issues/TREK-1/links", capture(bodySlot), any()) } returns objectMapper.readTree("{}")

        service().createLink("TREK-1", "relates", "TREK-2")

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("relationship").asText()).isEqualTo("relates")
        assertThat(body.path("issue").asText()).isEqualTo("TREK-2")
    }

    @Test
    @DisplayName("Добавление комментария собирает массив призываемых пользователей")
    fun `add comment builds summonees`() {
        val bodySlot = slot<Any>()
        every { client.post("/v3/issues/TREK-1/comments", capture(bodySlot), any()) } returns objectMapper.readTree("{}")

        service().addComment("TREK-1", "Текст", summonees = "ivan, petr")

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("text").asText()).isEqualTo("Текст")
        assertThat(body.path("summonees").map { it.asText() }).containsExactly("ivan", "petr")
    }

    @Test
    @DisplayName("Удаление комментария вызывает DELETE")
    fun `delete comment calls delete`() {
        service().deleteComment("TREK-1", "10")

        verify { client.delete("/v3/issues/TREK-1/comments/10", any()) }
    }

    @Test
    @DisplayName("Добавление пункта чек-листа формирует тело запроса")
    fun `add checklist item builds body`() {
        val bodySlot = slot<Any>()
        every {
            client.post("/v3/issues/TREK-1/checklistItems", capture(bodySlot), any())
        } returns objectMapper.readTree("""{"key":"TREK-1"}""")

        service().addChecklistItem(
            key = "TREK-1",
            text = "Сделать ревью",
            checked = true,
            assignee = "ivan",
            deadline = """{"date":"2021-05-09T00:00:00.000+0000","deadlineType":"date"}""",
            fields = null,
        )

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("text").asText()).isEqualTo("Сделать ревью")
        assertThat(body.path("checked").asBoolean()).isTrue()
        assertThat(body.path("assignee").asText()).isEqualTo("ivan")
        assertThat(body.path("deadline").path("deadlineType").asText()).isEqualTo("date")
    }

    @Test
    @DisplayName("Изменение пункта чек-листа вызывает PATCH")
    fun `update checklist item calls patch`() {
        val bodySlot = slot<Any>()
        every {
            client.patch("/v3/issues/TREK-1/checklistItems/item-1", capture(bodySlot), any())
        } returns objectMapper.readTree("{}")

        service().updateChecklistItem(
            key = "TREK-1",
            itemId = "item-1",
            text = null,
            checked = false,
            assignee = null,
            deadline = null,
            fields = null,
        )

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("checked").asBoolean()).isFalse()
        assertThat(body.has("text")).isFalse()
    }

    @Test
    @DisplayName("Удаление пункта чек-листа вызывает DELETE")
    fun `delete checklist item calls delete`() {
        every {
            client.delete("/v3/issues/TREK-1/checklistItems/item-1", any())
        } returns objectMapper.readTree("{}")

        service().deleteChecklistItem("TREK-1", "item-1")

        verify { client.delete("/v3/issues/TREK-1/checklistItems/item-1", any()) }
    }

    @Test
    @DisplayName("Добавление worklog формирует тело запроса")
    fun `add worklog builds body`() {
        val bodySlot = slot<Any>()
        every {
            client.post("/v3/issues/TREK-1/worklog", capture(bodySlot), any())
        } returns objectMapper.readTree("{}")

        service().addWorklog(
            key = "TREK-1",
            start = "2021-09-21T10:30:00.000+0000",
            duration = "PT2H",
            comment = "Разработка",
            fields = null,
        )

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("duration").asText()).isEqualTo("PT2H")
        assertThat(body.path("start").asText()).isEqualTo("2021-09-21T10:30:00.000+0000")
        assertThat(body.path("comment").asText()).isEqualTo("Разработка")
    }

    @Test
    @DisplayName("Добавление worklog без duration приводит к ApiException")
    fun `add worklog without duration raises ApiException`() {
        assertThatThrownBy {
            service().addWorklog("TREK-1", start = null, duration = "", comment = null, fields = null)
        }.isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("Изменение worklog вызывает PATCH")
    fun `update worklog calls patch`() {
        every {
            client.patch("/v3/issues/TREK-1/worklog/5", any(), any())
        } returns objectMapper.readTree("{}")

        service().updateWorklog("TREK-1", "5", start = null, duration = "PT1H", comment = null, fields = null)

        verify { client.patch("/v3/issues/TREK-1/worklog/5", any(), any()) }
    }

    @Test
    @DisplayName("Удаление worklog вызывает DELETE")
    fun `delete worklog calls delete`() {
        service().deleteWorklog("TREK-1", "5")

        verify { client.delete("/v3/issues/TREK-1/worklog/5", any()) }
    }

    @Test
    @DisplayName("Режим только для чтения блокирует изменяющую операцию")
    fun `read-only blocks write`() {
        assertThatThrownBy {
            service(readOnly = true).createIssue("TREK", "x", null, null, null, null, null, null)
        }.isInstanceOf(ReadOnlyModeException::class.java)

        verify(exactly = 0) { client.post(any(), any(), any()) }
    }

    @Test
    @DisplayName("Некорректный JSON в fields приводит к ApiException")
    fun `invalid fields raises ApiException`() {
        assertThatThrownBy {
            service().createIssue("TREK", "x", null, null, null, null, null, fields = "not-json")
        }.isInstanceOf(ApiException::class.java)
    }
}
