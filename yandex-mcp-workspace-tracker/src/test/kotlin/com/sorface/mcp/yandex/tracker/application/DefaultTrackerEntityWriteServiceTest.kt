package com.sorface.mcp.yandex.tracker.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
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

@DisplayName("Сервис изменения Entities API Tracker")
class DefaultTrackerEntityWriteServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val client = mockk<TrackerClient>(relaxed = true)

    private fun service(readOnly: Boolean = false): DefaultTrackerEntityWriteService =
        DefaultTrackerEntityWriteService(
            client,
            objectMapper,
            WriteGuard(YandexProperties(readOnly = readOnly)),
        )

    @Test
    @DisplayName("Создание проекта формирует вложенный fields и links")
    fun `create builds entity request`() {
        val body = slot<Any>()
        val query = slot<Map<String, String?>>()
        every {
            client.post("/v3/entities/project", capture(body), capture(query))
        } returns objectMapper.readTree("""{"id":"p1"}""")

        service().create(
            entityType = "project",
            summary = "Запуск продукта",
            description = "Описание",
            lead = "ivan",
            start = "2026-09-01",
            end = null,
            entityStatus = "draft",
            parentEntity = """{"primary":"portfolio-1"}""",
            fields = """{"followers":{"add":["petr"]}}""",
            links = """[{"relationship":"works towards","entity":"goal-1"}]""",
            responseFields = "summary, entityStatus",
        )

        val request = body.captured as ObjectNode
        assertThat(request.path("fields").path("summary").asText()).isEqualTo("Запуск продукта")
        assertThat(request.path("fields").path("followers").path("add")[0].asText()).isEqualTo("petr")
        assertThat(request.path("links")[0].path("entity").asText()).isEqualTo("goal-1")
        assertThat(query.captured["fields"]).isEqualTo("summary,entityStatus")
    }

    @Test
    @DisplayName("Создание цели отклоняет start и read-only поля")
    fun `create rejects incompatible and read only fields`() {
        assertThatThrownBy {
            service().create("goal", "Цель", null, null, "2026-09-01", null, null, null, null, null, null)
        }.isInstanceOf(ApiException::class.java).hasMessageContaining("start")

        assertThatThrownBy {
            service().create(
                "goal",
                "Цель",
                null,
                null,
                null,
                null,
                null,
                null,
                """{"progressPercentage":0.5}""",
                null,
                null,
            )
        }.isInstanceOf(ApiException::class.java).hasMessageContaining("только для чтения")
    }

    @Test
    @DisplayName("PATCH сохраняет оператор add без преобразования коллекции")
    fun `update preserves add operator`() {
        val body = slot<Any>()
        every { client.patch("/v3/entities/project/p1", capture(body), any()) } returns objectMapper.readTree("{}")

        service().update(
            "project",
            "p1",
            """{"followers":{"add":["ivan"]}}""",
            null,
            null,
            null,
            null,
        )

        assertThat((body.captured as ObjectNode).path("fields").path("followers").path("add")[0].asText())
            .isEqualTo("ivan")
    }

    @Test
    @DisplayName("withBoard запрещён для портфеля и цели")
    fun `delete rejects board for non project`() {
        assertThatThrownBy { service().delete("portfolio", "10", true) }
            .isInstanceOf(ApiException::class.java)
            .hasMessageContaining("project")
    }

    @Test
    @DisplayName("Пакетное изменение отклоняет повторяющиеся идентификаторы")
    fun `bulk update rejects duplicate ids`() {
        assertThatThrownBy {
            service().bulkUpdate("project", "p1,p1", """{"entityStatus":"at_risk"}""", null, null)
        }.isInstanceOf(ApiException::class.java).hasMessageContaining("повторяющиеся")
    }

    @Test
    @DisplayName("Полная замена чек-листа перечитывает его и сохраняет неуказанные свойства")
    fun `checklist replace reads and preserves fields`() {
        every {
            client.get("/v3/entities/project/p1", match { it["fields"] == "checklistItems" })
        } returns objectMapper.readTree(
            """{"fields":{"checklistItems":[{"id":"i1","text":"Старое","checked":true,"custom":"kept"}]}}""",
        )
        val body = slot<Any>()
        every {
            client.patch("/v3/entities/project/p1/checklistItems", capture(body), any())
        } returns objectMapper.readTree("{}")

        service().replaceChecklist(
            "project",
            "p1",
            """[{"id":"i1","text":"Новое"}]""",
            null,
            null,
            null,
            null,
        )

        val item = (body.captured as ArrayNode)[0]
        assertThat(item.path("text").asText()).isEqualTo("Новое")
        assertThat(item.path("checked").asBoolean()).isTrue()
        assertThat(item.path("custom").asText()).isEqualTo("kept")
    }

    @Test
    @DisplayName("Дубликат связи отклоняется до POST")
    fun `link duplicate is rejected`() {
        every { client.get("/v3/entities/project/p1/links", any()) } returns objectMapper.readTree(
            """[{"relationship":"works towards","entity":{"id":"g1"}}]""",
        )

        assertThatThrownBy { service().createLink("project", "p1", "works towards", "g1") }
            .isInstanceOf(ApiException::class.java)
            .hasMessageContaining("уже существует")
        verify(exactly = 0) { client.post("/v3/entities/project/p1/links", any(), any()) }
    }

    @Test
    @DisplayName("Удаление связи адресуется query-параметром rightEntityId и подтверждается чтением")
    fun `link delete uses right query and confirms result`() {
        every { client.get("/v3/entities/goal/g1/links", any()) } returns objectMapper.createArrayNode()

        val result = service().deleteLink("goal", "g1", "g2")

        assertThat(result.path("deleted").asBoolean()).isTrue()
        assertThat(result.path("rightEntityId").asText()).isEqualTo("g2")
        verify {
            client.delete(
                "/v3/entities/goal/g1/links",
                match { it["right"] == "g2" },
            )
        }
    }

    @Test
    @DisplayName("ACL нельзя изменить при активном наследовании без permissionSources=[]")
    fun `access update rejects inherited acl`() {
        every {
            client.get("/v3/entities/project/p1/extendedPermissions", any())
        } returns objectMapper.readTree("""{"permissionSources":[{"id":"portfolio-1"}]}""")

        assertThatThrownBy {
            service().updateAccess("project", "p1", null, """{"READ":{"users":["ivan"]}}""", null, true)
        }.isInstanceOf(ApiException::class.java).hasMessageContaining("permissionSources=[]")
    }

    @Test
    @DisplayName("Обычный permissions endpoint получает grant и revoke без обёртки acl")
    fun `plain access update sends acl body directly`() {
        val body = slot<Any>()
        every {
            client.get("/v3/entities/portfolio/p1/extendedPermissions", any())
        } returns objectMapper.readTree("""{"permissionSources":[]}""")
        every {
            client.patch("/v3/entities/portfolio/p1/permissions", capture(body), any())
        } returns objectMapper.readTree("{}")
        every {
            client.get("/v3/entities/portfolio/p1/permissions", any())
        } returns objectMapper.readTree("""{"grant":{"READ":{"users":["ivan"]}}}""")

        service().updateAccess(
            "portfolio",
            "p1",
            null,
            """{"READ":{"users":["ivan"]}}""",
            null,
            false,
        )

        val request = body.captured as ObjectNode
        assertThat(request.has("acl")).isFalse()
        assertThat(request.path("grant").path("READ").path("users")[0].asText()).isEqualTo("ivan")
    }

    @Test
    @DisplayName("Удаление ключевого результата передаёт полный актуальный объект оператору remove")
    fun `key result delete uses full current object`() {
        every {
            client.get("/v3/entities/goal/g1", match { it["fields"] == "keyResultItems" })
        } returns objectMapper.readTree(
            """{"fields":{"keyResultItems":[{"id":"kr1","type":"binary","text":"Запуск","achieved":false}]}}""",
        )
        val body = slot<Any>()
        every { client.patch("/v3/entities/goal/g1", capture(body), any()) } returns objectMapper.readTree("{}")

        service().deleteGoalKeyResult("g1", "kr1")

        val remove = (body.captured as ObjectNode).path("fields").path("keyResultItems").path("remove")
        assertThat(remove.path("id").asText()).isEqualTo("kr1")
        assertThat(remove.path("text").asText()).isEqualTo("Запуск")
    }

    @Test
    @DisplayName("Прикрепление временного файла подтверждается повторным чтением")
    fun `attachment attach confirms result`() {
        every {
            client.get("/v3/entities/goal/g1/attachments", any())
        } returns objectMapper.readTree("""[{"id":"30","name":"roadmap.pdf"}]""")

        val result = service().attachTemporaryFile("goal", "g1", "30", null, null, null, null)

        assertThat(result.path("attached").asBoolean()).isTrue()
        assertThat(result.path("attachment").path("name").asText()).isEqualTo("roadmap.pdf")
        verify { client.postEmpty("/v3/entities/goal/g1/attachments/30", any()) }
    }

    @Test
    @DisplayName("Режим только для чтения блокирует прямой вызов write-сервиса")
    fun `read only blocks direct write service`() {
        assertThatThrownBy {
            service(readOnly = true).delete("project", "p1", false)
        }.isInstanceOf(ReadOnlyModeException::class.java)

        verify(exactly = 0) { client.delete(any(), any()) }
    }
}
