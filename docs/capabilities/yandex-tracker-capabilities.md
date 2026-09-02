# Возможности Yandex Tracker в MCP-сервере

## Краткое описание

Документ фиксирует фактически реализованные инструменты `yandex-mcp-workspace-tracker`. Сервер предоставляет 84 MCP-инструмента: 6 общих (`system_*`, `yandex_auth_*`), 37 инструментов чтения Tracker и 41 изменяющий инструмент Tracker.

## Как это работает

Инструменты обращаются к REST API Tracker. Базовый адрес по умолчанию — `https://api.tracker.yandex.net` (переменная `YANDEX_TRACKER_BASE_URL`, только в модуле Tracker). В каждый запрос сервер добавляет:

- `Authorization: OAuth <токен>`;
- `X-Org-ID` для `YANDEX_360` или `X-Cloud-Org-ID` для `YANDEX_CLOUD`.

Постраничные методы возвращают `items`, обычные метаданные `totalCount`/`totalPages` и курсорные поля `nextPageId`/`nextPageUrl`. Для больших поисковых выдач также возвращается `scrollId`.

## Общие инструменты сервера

| Инструмент | Тип | Назначение |
|---|---|---|
| `system_ping` | R | Проверка доступности сервера |
| `system_server_info` | R | Информация о режиме чтения/записи |
| `yandex_auth_status` | R | Состояние OAuth-настроек и сохранённого токена |
| `yandex_auth_start` | W | Начать Device Flow и вернуть ссылку с кодом |
| `yandex_auth_poll` | R | Проверить состояние сессии авторизации |
| `yandex_auth_logout` | W | Удалить локальные токены |

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
| `tracker_issue_search` | Найти задачи по одному из `query`, `filter`, `queue`, `keys`; page/cursor/scroll | `POST /v3/issues/_search` | R |
| `tracker_issue_count` | Посчитать задачи по одному из `query`, `filter`, `queue`, `keys` | `POST /v3/issues/_count` | R |
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

### Связи с внешними приложениями

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_external_application_list` | Получить зарегистрированные внешние приложения и точный `origin` | `GET /v3/applications` | R |
| `tracker_external_link_list` | Получить внешние связи задачи и проверить дубликаты | `GET /v3/issues/{key}/remotelinks` | R |
| `tracker_external_link_create` | Создать внешнюю связь; `backlink` по умолчанию `true` | `POST /v3/issues/{key}/remotelinks?backlink={backlink}` | W |
| `tracker_external_link_delete` | Удалить внешнюю связь | `DELETE /v3/issues/{key}/remotelinks/{id}` | W |

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

### Проекты, портфели и цели: основные операции Entities API

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_entity_get` | Получить сущность по `id` или `shortId` | `GET /v3/entities/{type}/{id}` | R |
| `tracker_entity_search` | Найти сущности и нормализовать `values/hits/pages` | `POST /v3/entities/{type}/_search` | R |
| `tracker_entity_create` | Создать project, portfolio или goal | `POST /v3/entities/{type}` | W |
| `tracker_entity_update` | Изменить поля, комментарий или связи | `PATCH /v3/entities/{type}/{id}` | W |
| `tracker_entity_delete` | Удалить сущность; `withBoard` только для project | `DELETE /v3/entities/{type}/{id}` | W |
| `tracker_entity_bulk_update` | Запустить пакетное изменение | `POST /v3/entities/{type}/bulkchange/_update` | W |
| `tracker_bulk_operation_get` | Получить статус пакетной операции | `GET /v3/bulkchange/{id}` | R |
| `tracker_bulk_operation_error_list` | Получить элементы с ошибками | `GET /v3/bulkchange/{id}/issues` | R |
| `tracker_entity_event_list` | Получить события с относительной пагинацией | `GET /v3/entities/{type}/{id}/events/_relative` | R |

### Комментарии сущностей

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_entity_comment_list` | Получить комментарии с относительной пагинацией | `GET /v3/entities/{type}/{id}/comments/_relative` | R |
| `tracker_entity_comment_get` | Получить комментарий | `GET /v3/entities/{type}/{id}/comments/{commentId}` | R |
| `tracker_entity_comment_add` | Добавить комментарий | `POST /v3/entities/{type}/{id}/comments` | W |
| `tracker_entity_comment_update` | Изменить комментарий | `PATCH /v3/entities/{type}/{id}/comments/{commentId}` | W |
| `tracker_entity_comment_delete` | Удалить комментарий | `DELETE /v3/entities/{type}/{id}/comments/{commentId}` | W |

### Чек-листы проектов и портфелей

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_entity_checklist_list` | Получить пункты | `GET /v3/entities/{type}/{id}?fields=checklistItems` | R |
| `tracker_entity_checklist_add` | Добавить пункт | `POST /v3/entities/{type}/{id}/checklistItems` | W |
| `tracker_entity_checklist_replace` | Безопасно заменить актуальные пункты целиком | `PATCH /v3/entities/{type}/{id}/checklistItems` | W |
| `tracker_entity_checklist_item_update` | Изменить пункт | `PATCH /v3/entities/{type}/{id}/checklistItems/{itemId}` | W |
| `tracker_entity_checklist_item_move` | Переместить пункт | `POST /v3/entities/{type}/{id}/checklistItems/{itemId}/_move` | W |
| `tracker_entity_checklist_item_delete` | Удалить пункт | `DELETE /v3/entities/{type}/{id}/checklistItems/{itemId}` | W |
| `tracker_entity_checklist_clear` | Удалить весь чек-лист | `DELETE /v3/entities/{type}/{id}/checklistItems` | W |

### Вложения сущностей

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_temporary_attachment_upload` | Загрузить локальный файл multipart | `POST /v3/attachments/` | W |
| `tracker_entity_attachment_list` | Получить вложения | `GET /v3/entities/{type}/{id}/attachments` | R |
| `tracker_entity_attachment_get` | Получить метаданные файла | `GET /v3/entities/{type}/{id}/attachments/{fileId}` | R |
| `tracker_entity_attachment_attach` | Прикрепить временный файл и подтвердить чтением | `POST /v3/entities/{type}/{id}/attachments/{temporaryFileId}` | W |
| `tracker_entity_attachment_delete` | Удалить вложение | `DELETE /v3/entities/{type}/{id}/attachments/{fileId}` | W |

### Связи и настройки доступа сущностей

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_entity_link_list` | Получить связи | `GET /v3/entities/{type}/{id}/links` | R |
| `tracker_entity_link_create` | Проверить дубликат, создать и подтвердить связь | `POST /v3/entities/{type}/{id}/links` | W |
| `tracker_entity_link_delete` | Удалить связь по `rightEntityId` и подтвердить | `DELETE /v3/entities/{type}/{id}/links?right={rightId}` | W |
| `tracker_entity_access_get` | Получить ACL; по умолчанию с наследованием | `GET .../extendedPermissions` или `GET .../permissions` | R |
| `tracker_entity_access_update` | Изменить ACL и/или наследование, затем перечитать | `PATCH .../extendedPermissions` или `PATCH .../permissions` | W |

### Ключевые результаты и метрики

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `tracker_goal_key_result_list` | Получить ключевые результаты цели | `GET /v3/entities/goal/{id}?fields=keyResultItems` | R |
| `tracker_goal_key_result_add` | Добавить ключевой результат оператором `add` | `PATCH /v3/entities/goal/{id}` | W |
| `tracker_goal_key_result_update` | Перечитать и заменить выбранный результат | `GET`, затем `PATCH /v3/entities/goal/{id}` | W |
| `tracker_goal_key_result_delete` | Перечитать и удалить полный объект оператором `remove` | `GET`, затем `PATCH /v3/entities/goal/{id}` | W |
| `tracker_entity_metric_list` | Получить метрики | `GET /v3/entities/{type}/{id}?fields=metricItems` | R |
| `tracker_entity_metric_replace` | Полностью заменить метрики | `PATCH /v3/entities/{type}/{id}` | W |
| `tracker_entity_metric_clear` | Очистить метрики значением `null` | `PATCH /v3/entities/{type}/{id}` | W |

## Режим только для чтения

При `YANDEX_READ_ONLY=true` изменяющие инструменты Tracker не регистрируются в MCP-провайдере и не попадают в `tools/list`. Сюда входят `tracker_worklog_add`, `tracker_checklist_add`, все изменяющие `tracker_entity_*` и остальные инструменты **W**. Прямой вызов write-сервиса дополнительно блокируется `WriteGuard`.

## Не реализовано сейчас

В текущем коде нет инструментов Tracker для вложений задач, досок, спринтов, компонентов,
версий и массовых операций над задачами. Вложения и пакетное изменение сущностей реализованы
в Entities API. Полный HTTP- и MCP-контракт проектов, портфелей и целей описан в
[yandex-tracker-entities-api-contracts.md](./yandex-tracker-entities-api-contracts.md).
