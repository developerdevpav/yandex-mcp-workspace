# Yandex MCP Workspace

Два независимых MCP-сервера для **Yandex Tracker** и **Yandex Wiki**. Агент вызывает инструменты `tracker_*` или `wiki_*`, сервер добавляет OAuth и обращается к REST API Яндекса по транспорту **stdio**.

Репозиторий: [github.com/developerdevpav/yandex-mcp-workspace](https://github.com/developerdevpav/yandex-mcp-workspace). Подробная документация — [docs/](./docs/README.md).

## Требования

- готовый архив из [GitHub Releases](https://github.com/developerdevpav/yandex-mcp-workspace/releases/latest) — Java уже включена
- либо JRE 21 для запуска отдельного JAR
- либо Docker как опциональный серверный вариант
- Приложение [Яндекс OAuth](https://oauth.yandex.ru/) (`client_id`, `client_secret`)
- `YANDEX_ORG_ID` — идентификатор организации

Где создать приложение и найти все ключи — [docs/credentials.md](./docs/credentials.md).
- `YANDEX_ORG_TYPE` — тип организации (по умолчанию `YANDEX_360`):
  - `YANDEX_360` — Яндекс 360, заголовок `X-Org-ID`
  - `YANDEX_CLOUD` — Yandex Cloud, заголовок `X-Cloud-Org-ID`

## Авторизация

Docker и установленная Java не обязательны. Скачайте архив своей ОС из GitHub Release, распакуйте и выполните `setup` один раз. Например, в Linux:

```bash
./app/bin/yandex-mcp-tracker setup
```

Команда откроет браузер, сохранит настройки в `~/.config/yandex-mcp/config.properties`, а токены — в локальном каталоге данных ОС. Для запуска отдельного JAR команда выглядит так:

```bash
java -jar yandex-mcp-workspace-tracker.jar setup
```

Мастер запросит отсутствующие `client_id`, `client_secret`, идентификатор и тип организации; секрет в обычном терминале вводится без отображения. Параметры CLI остаются доступны для автоматизации.

После подключения MCP авторизацию также можно начать прямо из чата инструментом `yandex_auth_start`, а затем проверить `yandex_auth_poll`. Ссылка и код возвращаются структурированно — искать их в логах не нужно. Docker-сценарий сохранён в [docs/setup.md](./docs/setup.md#docker-опционально).

## Claude, ChatGPT Codex и Cursor

Готовые команды и конфигурации для Claude Desktop/Claude Code, ChatGPT Desktop/Codex и Cursor — в [docs/mcp-clients.md](./docs/mcp-clients.md). Инструкция по скачиванию для каждой ОС — в [docs/setup.md](./docs/setup.md), копируемые файлы — в [examples/mcp](./examples/mcp/).

## Инструменты

### Обзор

| Сервер | Модуль | Инструментов | Префиксы |
|---|---|---:|---|
| Tracker | `yandex-mcp-workspace-tracker` | 42 | `tracker_*`, `system_*`, `yandex_auth_*` |
| Wiki | `yandex-mcp-workspace-wiki` | 39 | `wiki_*`, `system_*`, `yandex_auth_*` |

В каждом сервере **6 общих** инструментов (служебные + auth) и **доменные** инструменты своего API.

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
| `yandex_auth_start` | W | Начать Device Flow, открыть браузер и вернуть ссылку с кодом |
| `yandex_auth_poll` | R | Проверить состояние сессии с соблюдением интервала OAuth |
| `yandex_auth_logout` | W | Удалить локальные токены |

---

### Tracker — 42 инструмента

36 доменных + 6 общих. Префикс доменных: `tracker_*`.

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
| `tracker_issue_search` | R | Поиск по одному критерию (`query`, `filter`, `queue` или `keys`), включая cursor/scroll |
| `tracker_issue_count` | R | Количество задач по запросу или фильтру |
| `tracker_issue_changelog` | R | История изменений задачи с курсором `nextPageId` |
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
| `tracker_comment_list` | R | Комментарии задачи с курсором `nextPageId` |
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
| `tracker_worklog_add` | W | Добавление записи (обязательные start и duration в ISO 8601) |
| `tracker_worklog_update` | W | Изменение записи по id |
| `tracker_worklog_delete` | W | Удаление записи по id |

---

### Wiki — 39 инструментов

33 доменных + 6 общих. Префикс доменных: `wiki_*`. Содержимое страниц — **Markdown**.

#### Страницы — чтение

| Инструмент | Тип | Описание |
|---|---|---|
| `wiki_page_get_by_slug` | R | Страница по slug (например, `team/onboarding`) |
| `wiki_page_get_by_id` | R | Страница по числовому id |
| `wiki_page_get_descendants` | R | Дерево вложенных страниц |
| `wiki_page_get_descendants_by_id` | R | Подстраницы по id родительской страницы |
| `wiki_page_get_resources` | R | Ресурсы страницы: вложения и таблицы |
| `wiki_search` | R | Полнотекстовый поиск страниц и файлов |
| `wiki_clone_operation_get` | R | Статус асинхронного клонирования |

#### Страницы — запись

| Инструмент | Тип | Описание |
|---|---|---|
| `wiki_page_create` | W | Создание страницы |
| `wiki_page_update` | W | Изменение заголовка и/или содержимого |
| `wiki_page_delete` | W | Удаление (в ответе — токен восстановления) |
| `wiki_page_recover` | W | Восстановление по токену |
| `wiki_page_clone` | W | Асинхронное клонирование страницы в обязательный `target` |
| `wiki_page_append_content` | W | Дописывание Markdown в начало, конец или к якорю |

#### Комментарии и вложения

| Инструмент | Тип | Описание |
|---|---|---|
| `wiki_page_comments_list` | R | Комментарии страницы |
| `wiki_page_comment_thread` | R | Ветка ответов на комментарий |
| `wiki_page_comment_add` | W | Комментарий или ответ на комментарий |
| `wiki_page_comment_delete` | W | Удаление комментария |
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

Попросите агента вызвать `system_ping` → `pong`, затем `yandex_auth_status`. Если токена нет, агент может вызвать `yandex_auth_start`, показать ссылку и после подтверждения выполнить `yandex_auth_poll`.

## Документация

| Раздел | Файл |
|---|---|
| Оглавление | [docs/README.md](./docs/README.md) |
| Обзор и модули | [docs/overview.md](./docs/overview.md) |
| Переменные окружения | [docs/configuration.md](./docs/configuration.md) |
| Установка и OAuth | [docs/setup.md](./docs/setup.md) |
| Claude / ChatGPT Codex / Cursor | [docs/mcp-clients.md](./docs/mcp-clients.md) |
| Tracker / Wiki | [docs/tracker.md](./docs/tracker.md), [docs/wiki.md](./docs/wiki.md) |
| Endpoint API | [docs/capabilities/](./docs/capabilities/README.md) |
| Разработка | [docs/development.md](./docs/development.md) |
| Ошибки | [docs/troubleshooting.md](./docs/troubleshooting.md) |
