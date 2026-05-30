# Возможности Yandex Tracker в MCP-сервере

## Краткое описание

Документ фиксирует фактически реализованные инструменты `yandex-mcp-workspace-tracker`. Сервер предоставляет 39 MCP-инструментов: 3 общих (`system_*`, `yandex_auth_status`), 21 инструмент чтения Tracker и 15 изменяющих инструментов Tracker.

## Как это работает

Инструменты обращаются к REST API Tracker. Базовый адрес по умолчанию — `https://api.tracker.yandex.net` (переменная `YANDEX_TRACKER_BASE_URL`, только в модуле Tracker). В каждый запрос сервер добавляет:

- `Authorization: OAuth <токен>`;
- `X-Org-ID` для `YANDEX_360` или `X-Cloud-Org-ID` для `YANDEX_CLOUD`.

Постраничные методы возвращают объект с `items` и метаданными `totalCount`, `totalPages`, `page`, `perPage`, если эти данные доступны в ответе API.

## Общие инструменты сервера

| Инструмент | Тип | Назначение |
|---|---|---|
| `system_ping` | R | Проверка доступности сервера |
| `system_server_info` | R | Информация о режиме чтения/записи |
| `yandex_auth_status` | R | Состояние OAuth-настроек и сохранённого токена |

## Инструменты Tracker

В таблицах: `R` — чтение, `W` — изменение.

### Пользователь и справочники

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_myself` | Получить текущего пользователя | `GET /v3/myself` | R |
| `tracker_user_list` | Получить список пользователей | `GET /v3/users` | R |
| `tracker_user_get` | Получить пользователя | `GET /v3/users/{id}` | R |
| `tracker_issuetype_list` | Получить типы задач | `GET /v3/issuetypes` | R |
| `tracker_priority_list` | Получить приоритеты | `GET /v3/priorities` | R |
| `tracker_status_list` | Получить статусы | `GET /v3/statuses` | R |
| `tracker_resolution_list` | Получить резолюции | `GET /v3/resolutions` | R |
| `tracker_field_list` | Получить глобальные поля | `GET /v3/fields` | R |
| `tracker_field_get` | Получить поле | `GET /v3/fields/{id}` | R |
| `tracker_queue_field_list` | Получить поля очереди | `GET /v3/queues/{queue}/fields` | R |

### Задачи

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_issue_get` | Получить задачу по ключу | `GET /v3/issues/{key}` | R |
| `tracker_issue_search` | Найти задачи по query или filter | `POST /v3/issues/_search` | R |
| `tracker_issue_count` | Посчитать задачи по query или filter | `POST /v3/issues/_count` | R |
| `tracker_issue_create` | Создать задачу | `POST /v3/issues` | W |
| `tracker_issue_update` | Изменить поля задачи | `PATCH /v3/issues/{key}` | W |
| `tracker_issue_move` | Перенести задачу в другую очередь | `POST /v3/issues/{key}/_move` | W |
| `tracker_issue_changelog` | Получить историю изменений | `GET /v3/issues/{key}/changelog` | R |

### Переходы по статусам

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_issue_transitions_list` | Получить доступные переходы | `GET /v3/issues/{key}/transitions` | R |
| `tracker_issue_transition_execute` | Выполнить переход | `POST /v3/issues/{key}/transitions/{id}/_execute` | W |

### Очереди

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_queue_list` | Получить список очередей | `GET /v3/queues` | R |
| `tracker_queue_get` | Получить очередь по id или ключу | `GET /v3/queues/{id}` | R |

### Комментарии

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_comment_list` | Получить комментарии задачи | `GET /v3/issues/{key}/comments` | R |
| `tracker_comment_add` | Добавить комментарий | `POST /v3/issues/{key}/comments` | W |
| `tracker_comment_update` | Изменить комментарий | `PATCH /v3/issues/{key}/comments/{id}` | W |
| `tracker_comment_delete` | Удалить комментарий | `DELETE /v3/issues/{key}/comments/{id}` | W |

### Связи задач

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_link_list` | Получить связи задачи | `GET /v3/issues/{key}/links` | R |
| `tracker_link_create` | Создать связь | `POST /v3/issues/{key}/links` | W |
| `tracker_link_delete` | Удалить связь | `DELETE /v3/issues/{key}/links/{id}` | W |

### Чек-лист

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_checklist_list` | Получить пункты чек-листа | `GET /v3/issues/{key}/checklistItems` | R |
| `tracker_checklist_add` | Добавить пункт | `POST /v3/issues/{key}/checklistItems` | W |
| `tracker_checklist_update` | Изменить пункт | `PATCH /v3/issues/{key}/checklistItems/{id}` | W |
| `tracker_checklist_delete` | Удалить пункт | `DELETE /v3/issues/{key}/checklistItems/{id}` | W |

### Учёт времени (worklog)

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_worklog_list` | Получить записи worklog | `GET /v3/issues/{key}/worklog` | R |
| `tracker_worklog_add` | Добавить запись | `POST /v3/issues/{key}/worklog` | W |
| `tracker_worklog_update` | Изменить запись | `PATCH /v3/issues/{key}/worklog/{id}` | W |
| `tracker_worklog_delete` | Удалить запись | `DELETE /v3/issues/{key}/worklog/{id}` | W |

## Режим только для чтения

При `YANDEX_READ_ONLY=true` изменяющие инструменты Tracker не регистрируются в MCP-провайдере и не попадают в `tools/list`. Сюда входят `tracker_worklog_add`, `tracker_checklist_add`, `tracker_checklist_update`, `tracker_checklist_delete` и остальные инструменты **W**.

## Не реализовано сейчас

В текущем коде нет инструментов Tracker для вложений, досок, спринтов, компонентов, версий, массовых операций, проектов и портфелей.
