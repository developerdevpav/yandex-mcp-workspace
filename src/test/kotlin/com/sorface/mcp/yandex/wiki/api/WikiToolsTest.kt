package com.sorface.mcp.yandex.wiki.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sorface.mcp.yandex.wiki.application.WikiReadService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Инструменты чтения Wiki (WikiTools)")
class WikiToolsTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val service = mockk<WikiReadService>()
    private val tools = WikiTools(service, objectMapper)

    @Test
    @DisplayName("wiki_page_get_by_slug форматирует страницу в JSON")
    fun `page by slug renders json`() {
        every { service.getPageBySlug("team/onboarding", null) } returns
            objectMapper.readTree("""{"id":10,"title":"Onboarding"}""")

        val result = tools.pageGetBySlug("team/onboarding", null)

        assertThat(objectMapper.readTree(result).path("title").asText()).isEqualTo("Onboarding")
    }
}
