package com.sorface.mcp.yandex.wiki.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.wiki.infrastructure.WikiClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Сервис чтения Wiki (DefaultWikiReadService)")
class DefaultWikiReadServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val client = mockk<WikiClient>()
    private val service = DefaultWikiReadService(client, objectMapper)

    @Test
    @DisplayName("Поиск формирует тело по актуальной схеме API Wiki")
    fun `search builds current api body`() {
        val bodySlot = slot<Any>()
        every { client.post("/v1/search", capture(bodySlot), any()) } returns objectMapper.readTree("{}")

        service.search(
            query = "регламент",
            filters = """{"type":"page","cluster":"team"}""",
            cursor = 2,
            limit = 25,
            orderBy = "modified_date",
            highlight = true,
        )

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("query").asText()).isEqualTo("регламент")
        assertThat(body.path("filters").path("type").asText()).isEqualTo("page")
        assertThat(body.path("cursor").asInt()).isEqualTo(2)
        assertThat(body.path("limit").asInt()).isEqualTo(25)
        assertThat(body.path("order_by").asText()).isEqualTo("modified_date")
        assertThat(body.path("highlight").asBoolean()).isTrue()
    }

    @Test
    @DisplayName("Подстраницы по ID передают курсорную пагинацию Wiki")
    fun `descendants by id pass cursor pagination`() {
        every { client.get(any(), any()) } returns objectMapper.readTree("{}")

        service.getDescendantsById("10", "next-1", 100, "actual", true, false)

        verify {
            client.get(
                "/v1/pages/10/descendants",
                match {
                    it["cursor"] == "next-1" && it["page_size"] == "100" &&
                        it["actuality"] == "actual" && it["include_self"] == "true"
                },
            )
        }
    }
}
