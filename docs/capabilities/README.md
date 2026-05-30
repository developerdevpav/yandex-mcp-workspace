# Возможности MCP-серверов

| Сервер | Модуль | Образ | Инструментов |
|---|---|---|---|
| Tracker | `yandex-mcp-workspace-tracker` | `ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest` | 39 |
| Wiki | `yandex-mcp-workspace-wiki` | `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest` | 31 |

Обзор workspace и конфигурация — [../overview.md](../overview.md), [../configuration.md](../configuration.md).

## Общие инструменты

| Инструмент | Назначение |
|---|---|
| `system_ping` | Проверка доступности (`pong`) |
| `system_server_info` | Режим: read-write или read-only |
| `yandex_auth_status` | Состояние OAuth без секретов |

## Tracker

| Группа | Инструменты |
|---|---|
| Пользователь | `tracker_myself`, `tracker_user_list`, `tracker_user_get` |
| Задачи | `tracker_issue_get`, `tracker_issue_search`, `tracker_issue_count`, `tracker_issue_create`, `tracker_issue_update`, `tracker_issue_move`, `tracker_issue_changelog` |
| Переходы | `tracker_issue_transitions_list`, `tracker_issue_transition_execute` |
| Очереди | `tracker_queue_list`, `tracker_queue_get`, `tracker_queue_field_list` |
| Справочники | `tracker_issuetype_list`, `tracker_priority_list`, `tracker_status_list`, `tracker_resolution_list`, `tracker_field_list`, `tracker_field_get` |
| Комментарии | `tracker_comment_list`, `tracker_comment_add`, `tracker_comment_update`, `tracker_comment_delete` |
| Связи | `tracker_link_list`, `tracker_link_create`, `tracker_link_delete` |
| Чек-лист | `tracker_checklist_list`, `tracker_checklist_add`, `tracker_checklist_update`, `tracker_checklist_delete` |
| Worklog | `tracker_worklog_list`, `tracker_worklog_add`, `tracker_worklog_update`, `tracker_worklog_delete` |

Подробно: [yandex-tracker-capabilities.md](./yandex-tracker-capabilities.md).

## Wiki

| Группа | Инструменты |
|---|---|
| Страницы | `wiki_page_get_by_slug`, `wiki_page_get_by_id`, `wiki_page_get_descendants`, `wiki_page_get_resources`, `wiki_page_create`, `wiki_page_update`, `wiki_page_delete`, `wiki_page_recover`, `wiki_page_clone`, `wiki_page_append_content` |
| Комментарии | `wiki_page_comments_list`, `wiki_page_comment_add` |
| Вложения | `wiki_page_attachments_list`, `wiki_page_attachment_upload`, `wiki_page_attachment_attach` |
| Таблицы | `wiki_grid_get`, `wiki_page_grids_list`, `wiki_grid_create`, `wiki_grid_update`, `wiki_grid_delete`, `wiki_grid_clone`, `wiki_grid_add_rows`, `wiki_grid_delete_rows`, `wiki_grid_move_row`, `wiki_grid_add_columns`, `wiki_grid_delete_columns`, `wiki_grid_move_column`, `wiki_grid_update_cells` |

Подробно: [yandex-wiki-capabilities.md](./yandex-wiki-capabilities.md).

## Read-only

`YANDEX_READ_ONLY=true` — изменяющие инструменты Tracker и Wiki (кроме read-части таблиц) не регистрируются; запись в таблицы блокируется `WriteGuard`.

## Не реализовано

Tracker: вложения, доски, спринты, компоненты, версии, массовые операции.
