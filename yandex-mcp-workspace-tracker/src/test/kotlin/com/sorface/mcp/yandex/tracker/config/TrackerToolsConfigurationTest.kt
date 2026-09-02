package com.sorface.mcp.yandex.tracker.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.sorface.mcp.yandex.auth.api.AuthTools
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.system.api.SystemTools
import com.sorface.mcp.yandex.tracker.api.TrackerEntityTools
import com.sorface.mcp.yandex.tracker.api.TrackerEntityWriteTools
import com.sorface.mcp.yandex.tracker.api.TrackerTools
import com.sorface.mcp.yandex.tracker.api.TrackerWriteTools
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallbackProvider

@DisplayName("Регистрация инструментов MCP-сервера Tracker")
class TrackerToolsConfigurationTest {

    private val configuration = TrackerToolsConfiguration()
    private val objectMapper = ObjectMapper()

    private fun provider(readOnly: Boolean): ToolCallbackProvider =
        configuration.trackerToolCallbackProvider(
            properties = YandexProperties(readOnly = readOnly),
            systemTools = SystemTools(YandexProperties(readOnly = readOnly)),
            authTools = AuthTools(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
            trackerTools = TrackerTools(mockk(relaxed = true), objectMapper),
            trackerEntityTools = TrackerEntityTools(mockk(relaxed = true), objectMapper),
            trackerWriteTools = TrackerWriteTools(mockk(relaxed = true), objectMapper),
            trackerEntityWriteTools = TrackerEntityWriteTools(mockk(relaxed = true), objectMapper),
        )

    private fun toolNames(provider: ToolCallbackProvider): List<String> =
        provider.toolCallbacks.map { it.toolDefinition.name() }

    @Test
    @DisplayName("Регистрируются только служебные, авторизационные и Tracker-инструменты")
    fun `registers only tracker tools`() {
        val names = toolNames(provider(readOnly = false))

        assertThat(names).hasSize(84)
        assertThat(names).contains(
            "system_ping",
            "yandex_auth_status",
            "yandex_auth_start",
            "yandex_auth_poll",
            "yandex_auth_logout",
            "tracker_issue_get",
            "tracker_issue_create",
            "tracker_checklist_list",
            "tracker_worklog_list",
            "tracker_field_list",
            "tracker_external_application_list",
            "tracker_external_link_list",
            "tracker_external_link_create",
            "tracker_external_link_delete",
            "tracker_entity_get",
            "tracker_entity_search",
            "tracker_entity_create",
            "tracker_entity_comment_list",
            "tracker_entity_checklist_replace",
            "tracker_temporary_attachment_upload",
            "tracker_entity_access_update",
            "tracker_goal_key_result_update",
            "tracker_entity_metric_clear",
        )
        assertThat(names).noneMatch { it.startsWith("wiki_") }
    }

    @Test
    @DisplayName("В режиме только для чтения изменяющие Tracker-инструменты не регистрируются")
    fun `hides tracker write tools in read-only`() {
        val names = toolNames(provider(readOnly = true))

        assertThat(names).hasSize(43)
        assertThat(names).contains(
            "tracker_issue_get",
            "system_server_info",
            "tracker_external_application_list",
            "tracker_external_link_list",
            "tracker_entity_get",
            "tracker_entity_search",
            "tracker_entity_comment_list",
            "tracker_entity_checklist_list",
            "tracker_entity_attachment_list",
            "tracker_entity_link_list",
            "tracker_entity_access_get",
            "tracker_goal_key_result_list",
            "tracker_entity_metric_list",
        )
        assertThat(names).doesNotContain(
            "tracker_issue_create",
            "tracker_checklist_add",
            "tracker_worklog_add",
            "tracker_external_link_create",
            "tracker_external_link_delete",
            "tracker_entity_create",
            "tracker_entity_update",
            "tracker_entity_delete",
            "tracker_entity_comment_add",
            "tracker_entity_checklist_replace",
            "tracker_temporary_attachment_upload",
            "tracker_entity_access_update",
            "tracker_goal_key_result_add",
            "tracker_entity_metric_clear",
        )
    }

    @Test
    @DisplayName("Схема tracker_external_link_create требует идентификаторы, но не backlink")
    fun `external link create schema keeps backlink optional`() {
        val callback = provider(readOnly = false).toolCallbacks.first {
            it.toolDefinition.name() == "tracker_external_link_create"
        }
        val schema = objectMapper.readTree(callback.toolDefinition.inputSchema())

        assertThat(schema.path("properties").fieldNames().asSequence().toList()).contains(
            "key",
            "relationship",
            "objectKey",
            "origin",
            "backlink",
        )
        assertThat(schema.path("required").map { it.asText() }).contains(
            "key",
            "relationship",
            "objectKey",
            "origin",
        ).doesNotContain("backlink")
    }

    @Test
    @DisplayName("Схема tracker_entity_get требует тип и идентификатор сущности")
    fun `entity get schema requires type and id`() {
        val callback = provider(readOnly = false).toolCallbacks.first {
            it.toolDefinition.name() == "tracker_entity_get"
        }
        val schema = objectMapper.readTree(callback.toolDefinition.inputSchema())

        assertThat(schema.path("required").map { it.asText() })
            .contains("entityType", "entityId")
            .doesNotContain("fields", "expand")
    }
}
