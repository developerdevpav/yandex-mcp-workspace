# Yandex MCP Workspace

Два независимых MCP-сервера для **Yandex Tracker** и **Yandex Wiki**. Агент вызывает инструменты `tracker_*` или `wiki_*`, сервер добавляет OAuth и обращается к REST API Яндекса по транспорту **stdio**.

Репозиторий: [github.com/developerdevpav/yandex-mcp-workspace](https://github.com/developerdevpav/yandex-mcp-workspace). Подробная документация — [docs/](./docs/README.md).

| Сервер | Образ |
|---|---|
| Tracker | `ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest` |
| Wiki | `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest` |

## Требования

- Docker
- Приложение [Яндекс OAuth](https://oauth.yandex.ru/) (`client_id`, `client_secret`)
- `YANDEX_ORG_ID` — идентификатор организации
- `YANDEX_ORG_TYPE` — тип организации (по умолчанию `YANDEX_360`):
  - `YANDEX_360` — Яндекс 360, заголовок `X-Org-ID`
  - `YANDEX_CLOUD` — Yandex Cloud, заголовок `X-Cloud-Org-ID`

## Авторизация

Выполняется **один раз** на машине. Откройте ссылку из терминала и введите код подтверждения (OAuth 2.0 Device Flow). Токены сохраняются в Docker-томе `yandex-mcp-tokens`.

```bash
docker run -it --rm \
  -e YANDEX_CLIENT_ID=<client_id> \
  -e YANDEX_CLIENT_SECRET=<client_secret> \
  -e YANDEX_ORG_ID=<org_id> \
  -e YANDEX_ORG_TYPE=YANDEX_360 \
  -v yandex-mcp-tokens:/data \
  ghcr.io/developerdevpav/yandex-mcp-workspace-tracker:latest auth
```

Для Wiki можно выполнить `auth` с образом `ghcr.io/developerdevpav/yandex-mcp-workspace-wiki:latest`. Если оба сервера используют одно OAuth-приложение и организацию, достаточно одной авторизации в общем томе `yandex-mcp-tokens`.

## Cursor и Codex

Готовые примеры `mcp.json` и `config.toml` — в [docs/mcp-clients.md](./docs/mcp-clients.md). После сохранения конфигурации в Cursor — перезапуск или *Reload* в *Settings → Tools & MCP*.

## Инструменты

### Обзор

| Сервер | Модуль | Инструментов | Префиксы |
|---|---|---:|---|
| Tracker | `yandex-mcp-workspace-tracker` | 39 | `tracker_*`, `system_*`, `yandex_auth_status` |
| Wiki | `yandex-mcp-workspace-wiki` | 31 | `wiki_*`, `system_*`, `yandex_auth_status` |

В каждом сервере **3 общих** инструмента (служебные + auth) и **доменные** инструменты своего API.

### Условные обозначения

| Обозначение | Значение |
|---|---|
| **R** | Чтение — только получение данных |
| **W** | Изменение — создание, обновление, удаление |

### Режим только для чтения

При `YANDEX_READ_ONLY=true`:

- инструменты **W** не регистрируются и не попадают в `tools/list`;
- инструменты **R** работают без ограничений;
- в Wiki инструменты таблиц остаются в списке (содержат и чтение, и запись), но **запись блокируется** на уровне сервиса до обращения к API.

---

### Общие инструменты

Доступны в **обоих** серверах.

| Инструмент | Тип | Описание |
|---|---|---|
| `system_ping` | R | Проверка доступности MCP-сервера, возвращает `pong` |
| `system_server_info` | R | Режим работы: read-only или read-write |
| `yandex_auth_status` | R | Состояние OAuth: настройки, наличие токена, срок истечения |

---

### Tracker — 39 инструментов

36 доменных + 3 общих. Префикс доменных: `tracker_*`.

#### Справочники и пользователь

| Инструмент | Тип | Описание |
|---|---|---|
| `tracker_myself` | R | Данные текущего пользователя Tracker |
| `tracker_user_list` | R | Список пользователей организации (пагинация) |
| `tracker_user_get` | R | Пользователь по логину или id |
| `tracker_issuetype_list` | R | Справочник типов задач |
| `tracker_priority_list` | R | Справочник приоритетов |
| `tracker_status_list` | R | Справочник статусов |
| `tracker_resolution_list` | R | Справочник резолюций |
| `tracker_field_list` | R | Глобальные поля организации |
| `tracker_field_get` | R | Поле по id или ключу |
| `tracker_queue_field_list` | R | Поля очереди (обязательные и доступные) |

#### Задачи — чтение

| Инструмент | Тип | Описание |
|---|---|---|
| `tracker_issue_get` | R | Задача по ключу (например, `TREK-42`) |
| `tracker_issue_search` | R | Поиск по языку запросов или JSON-фильтру |
| `tracker_issue_count` | R | Количество задач по запросу или фильтру |
| `tracker_issue_changelog` | R | История изменений задачи |
| `tracker_issue_transitions_list` | R | Доступные переходы по статусам |

#### Задачи — запись

| Инструмент | Тип | Описание |
|---|---|---|
| `tracker_issue_create` | W | Создание задачи (произвольные поля — JSON `fields`) |
| `tracker_issue_update` | W | Изменение полей (опционально `version` против конфликтов) |
| `tracker_issue_move` | W | Перенос в другую очередь |
| `tracker_issue_transition_execute` | W | Выполнение перехода по статусу |

#### Очереди

| Инструмент | Тип | Описание |
|---|---|---|
| `tracker_queue_list` | R | Список очередей |
| `tracker_queue_get` | R | Параметры очереди по id или ключу |

#### Комментарии

| Инструмент | Тип | Описание |
|---|---|---|
| `tracker_comment_list` | R | Комментарии задачи |
| `tracker_comment_add` | W | Добавление комментария |
| `tracker_comment_update` | W | Изменение комментария |
| `tracker_comment_delete` | W | Удаление комментария |

#### Связи задач

| Инструмент | Тип | Описание |
|---|---|---|
| `tracker_link_list` | R | Связи задачи |
| `tracker_link_create` | W | Создание связи между задачами |
| `tracker_link_delete` | W | Удаление связи |

#### Чек-лист

| Инструмент | Тип | Описание |
|---|---|---|
| `tracker_checklist_list` | R | Пункты чек-листа задачи |
| `tracker_checklist_add` | W | Добавление пункта (текст, checked, assignee, deadline) |
| `tracker_checklist_update` | W | Изменение пункта по id |
| `tracker_checklist_delete` | W | Удаление пункта по id |

#### Учёт времени (worklog)

| Инструмент | Тип | Описание |
|---|---|---|
| `tracker_worklog_list` | R | Записи учёта времени задачи |
| `tracker_worklog_add` | W | Добавление записи (duration в ISO 8601, опционально start, comment) |
| `tracker_worklog_update` | W | Изменение записи по id |
| `tracker_worklog_delete` | W | Удаление записи по id |

---

### Wiki — 31 инструмент

28 доменных + 3 общих. Префикс доменных: `wiki_*`. Содержимое страниц — **Markdown**.

#### Страницы — чтение

| Инструмент | Тип | Описание |
|---|---|---|
| `wiki_page_get_by_slug` | R | Страница по slug (например, `team/onboarding`) |
| `wiki_page_get_by_id` | R | Страница по числовому id |
| `wiki_page_get_descendants` | R | Дерево вложенных страниц |
| `wiki_page_get_resources` | R | Ресурсы страницы: вложения и таблицы |

#### Страницы — запись

| Инструмент | Тип | Описание |
|---|---|---|
| `wiki_page_create` | W | Создание страницы |
| `wiki_page_update` | W | Изменение заголовка и/или содержимого |
| `wiki_page_delete` | W | Удаление (в ответе — токен восстановления) |
| `wiki_page_recover` | W | Восстановление по токену |
| `wiki_page_clone` | W | Клонирование страницы |
| `wiki_page_append_content` | W | Дописывание Markdown в начало, конец или к якорю |

#### Комментарии и вложения

| Инструмент | Тип | Описание |
|---|---|---|
| `wiki_page_comments_list` | R | Комментарии страницы |
| `wiki_page_comment_add` | W | Комментарий или ответ на комментарий |
| `wiki_page_attachments_list` | R | Вложения страницы |
| `wiki_page_attachment_upload` | W | Загрузка локального файла и прикрепление |
| `wiki_page_attachment_attach` | W | Прикрепление завершённых сессий загрузки |

#### Динамические таблицы — чтение

| Инструмент | Тип | Описание |
|---|---|---|
| `wiki_grid_get` | R | Таблица по id |
| `wiki_page_grids_list` | R | Список таблиц страницы |

#### Динамические таблицы — запись

Тело изменяющих операций — JSON-строка в параметре `body`.

| Инструмент | Тип | Описание |
|---|---|---|
| `wiki_grid_create` | W | Создание таблицы |
| `wiki_grid_update` | W | Изменение заголовка/сортировки |
| `wiki_grid_delete` | W | Удаление таблицы |
| `wiki_grid_clone` | W | Клонирование таблицы |
| `wiki_grid_add_rows` | W | Добавление строк |
| `wiki_grid_delete_rows` | W | Удаление строк |
| `wiki_grid_move_row` | W | Перемещение строки |
| `wiki_grid_add_columns` | W | Добавление столбцов |
| `wiki_grid_delete_columns` | W | Удаление столбцов |
| `wiki_grid_move_column` | W | Перемещение столбца |
| `wiki_grid_update_cells` | W | Обновление значений ячеек |

---

Подробнее с endpoint API — [docs/capabilities/](./docs/capabilities/README.md).

## Проверка

Попросите агента вызвать `system_ping` → `pong`, затем `yandex_auth_status` → `авторизован: да`.

## Документация

| Раздел | Файл |
|---|---|
| Оглавление | [docs/README.md](./docs/README.md) |
| Обзор и модули | [docs/overview.md](./docs/overview.md) |
| Переменные окружения | [docs/configuration.md](./docs/configuration.md) |
| Установка и OAuth | [docs/setup.md](./docs/setup.md) |
| Cursor / Codex | [docs/mcp-clients.md](./docs/mcp-clients.md) |
| Tracker / Wiki | [docs/tracker.md](./docs/tracker.md), [docs/wiki.md](./docs/wiki.md) |
| Endpoint API | [docs/capabilities/](./docs/capabilities/README.md) |
| Разработка | [docs/development.md](./docs/development.md) |
| Ошибки | [docs/troubleshooting.md](./docs/troubleshooting.md) |
