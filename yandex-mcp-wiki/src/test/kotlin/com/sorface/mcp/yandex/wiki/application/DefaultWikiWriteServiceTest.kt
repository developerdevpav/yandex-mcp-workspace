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
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Сервис изменения Wiki (DefaultWikiWriteService)")
class DefaultWikiWriteServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val client = mockk<WikiClient>(relaxed = true)

    private fun service(readOnly: Boolean = false): DefaultWikiWriteService {
        val guard = WriteGuard(YandexProperties(readOnly = readOnly))
        return DefaultWikiWriteService(client, objectMapper, guard)
    }

    @Test
    @DisplayName("Создание страницы собирает тело из заголовка, адреса и содержимого")
    fun `create page builds body`() {
        val bodySlot = slot<Any>()
        every { client.post("/v1/pages", capture(bodySlot), any()) } returns objectMapper.readTree("{}")

        service().createPage("Заголовок", "team/page", null, "# Markdown", fields = """{"page_type":"page"}""")

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("title").asText()).isEqualTo("Заголовок")
        assertThat(body.path("slug").asText()).isEqualTo("team/page")
        assertThat(body.path("content").asText()).isEqualTo("# Markdown")
        assertThat(body.path("page_type").asText()).isEqualTo("page")
        assertThat(body.has("parent_id")).isFalse()
    }

    @Test
    @DisplayName("Восстановление страницы вызывает POST без тела по токену")
    fun `recover page posts empty`() {
        every { client.postEmpty(any(), any()) } returns objectMapper.readTree("{}")

        service().recoverPage("rec-1")

        verify { client.postEmpty("/v1/recovery_tokens/rec-1/recover", any()) }
    }

    @Test
    @DisplayName("Добавление комментария передаёт текст и родителя")
    fun `add comment builds body`() {
        val bodySlot = slot<Any>()
        every { client.post("/v1/pages/10/comments", capture(bodySlot), any()) } returns objectMapper.readTree("{}")

        service().addComment("10", "текст", parentId = "5")

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("content").asText()).isEqualTo("текст")
        assertThat(body.path("parent_id").asText()).isEqualTo("5")
    }

    @Test
    @DisplayName("Прикрепление сессий формирует массив upload_sessions")
    fun `attach builds sessions array`() {
        val bodySlot = slot<Any>()
        every { client.post("/v1/pages/10/attachments", capture(bodySlot), any()) } returns objectMapper.readTree("{}")

        service().attachUploadSessions("10", "s-1, s-2")

        val body = bodySlot.captured as ObjectNode
        assertThat(body.path("upload_sessions").map { it.asText() }).containsExactly("s-1", "s-2")
    }

    @Test
    @DisplayName("Пустой список сессий приводит к ApiException")
    fun `empty sessions raises`() {
        assertThatThrownBy { service().attachUploadSessions("10", " , ") }
            .isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("Загрузка вложения создаёт сессию, грузит части и прикрепляет к странице")
    fun `upload attachment orchestrates session`(@TempDir dir: Path) {
        val file = dir.resolve("doc.txt")
        Files.write(file, ByteArray(5 * 1024 * 1024 + 10) { 1 })

        every { client.post("/v1/upload_sessions", any(), any()) } returns objectMapper.readTree("""{"id":"sess-1"}""")
        every { client.putBinary(any(), any(), any()) } returns objectMapper.readTree("{}")
        every { client.postEmpty("/v1/upload_sessions/sess-1/finish", any()) } returns objectMapper.readTree("{}")
        every { client.post("/v1/pages/10/attachments", any(), any()) } returns objectMapper.readTree("""{"ok":true}""")

        val partSlot = mutableListOf<Map<String, String?>>()
        every { client.putBinary("/v1/upload_sessions/sess-1/upload_part", any(), capture(partSlot)) } returns
            objectMapper.readTree("{}")

        service().uploadAttachment("10", file.toString(), name = null)

        // Файл чуть больше 5 МБ — ожидаем две части с номерами 1 и 2.
        assertThat(partSlot.map { it["part_number"] }).containsExactly("1", "2")
        verifyOrder {
            client.post("/v1/upload_sessions", any(), any())
            client.putBinary("/v1/upload_sessions/sess-1/upload_part", any(), any())
            client.postEmpty("/v1/upload_sessions/sess-1/finish", any())
            client.post("/v1/pages/10/attachments", any(), any())
        }
    }

    @Test
    @DisplayName("Отсутствующий файл приводит к ApiException")
    fun `missing file raises`() {
        assertThatThrownBy { service().uploadAttachment("10", "/no/such/file.bin", null) }
            .isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("Режим только для чтения блокирует изменение страницы")
    fun `read-only blocks write`() {
        assertThatThrownBy { service(readOnly = true).updatePage("10", "t", null, null) }
            .isInstanceOf(ReadOnlyModeException::class.java)

        verify(exactly = 0) { client.post(any(), any(), any()) }
    }
}
