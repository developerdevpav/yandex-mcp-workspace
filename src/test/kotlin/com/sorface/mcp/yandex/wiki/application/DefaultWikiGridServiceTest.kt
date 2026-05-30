package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.common.ApiException
import com.sorface.mcp.yandex.common.ReadOnlyModeException
import com.sorface.mcp.yandex.common.WriteGuard
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.wiki.infrastructure.WikiClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Сервис таблиц Wiki (DefaultWikiGridService)")
class DefaultWikiGridServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val client = mockk<WikiClient>(relaxed = true)

    private fun service(readOnly: Boolean = false): DefaultWikiGridService {
        val guard = WriteGuard(YandexProperties(readOnly = readOnly))
        return DefaultWikiGridService(client, objectMapper, guard)
    }

    @Test
    @DisplayName("Получение таблицы обращается к нужному endpoint")
    fun `get grid`() {
        every { client.get("/v1/grids/7") } returns objectMapper.readTree("""{"id":7}""")

        val result = service().getGrid("7")

        assertThat(result.path("id").asInt()).isEqualTo(7)
    }

    @Test
    @DisplayName("Список таблиц страницы обращается к нужному endpoint")
    fun `list page grids`() {
        every { client.get("/v1/pages/10/grids") } returns objectMapper.readTree("[]")

        service().listPageGrids("10")

        verify { client.get("/v1/pages/10/grids") }
    }

    @Test
    @DisplayName("Добавление строк пересылает разобранное тело JSON")
    fun `add rows forwards parsed body`() {
        val bodySlot = slot<Any>()
        every { client.post("/v1/grids/7/rows", capture(bodySlot), any()) } returns objectMapper.readTree("{}")

        service().addRows("7", """{"rows":[{"cells":[{"value":"x"}]}]}""")

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("rows")[0].path("cells")[0].path("value").asText()).isEqualTo("x")
    }

    @Test
    @DisplayName("Удаление строк использует DELETE с телом")
    fun `delete rows uses delete with body`() {
        every { client.deleteWithBody(any(), any(), any()) } returns objectMapper.readTree("{}")

        service().deleteRows("7", """{"row_ids":["r1"]}""")

        verify { client.deleteWithBody("/v1/grids/7/rows", any(), any()) }
    }

    @Test
    @DisplayName("Обновление ячеек обращается к endpoint ячеек")
    fun `update cells endpoint`() {
        every { client.post("/v1/grids/7/cells", any(), any()) } returns objectMapper.readTree("{}")

        service().updateCells("7", """{"cells":[]}""")

        verify { client.post("/v1/grids/7/cells", any(), any()) }
    }

    @Test
    @DisplayName("Некорректный JSON тела приводит к ApiException")
    fun `invalid body raises`() {
        assertThatThrownBy { service().createGrid("not-json") }
            .isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("Режим только для чтения блокирует изменение таблицы")
    fun `read-only blocks write`() {
        assertThatThrownBy { service(readOnly = true).deleteGrid("7") }
            .isInstanceOf(ReadOnlyModeException::class.java)

        verify(exactly = 0) { client.delete(any(), any()) }
    }
}
