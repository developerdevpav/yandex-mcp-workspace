# Возможности Yandex Wiki в MCP-сервере

## Краткое описание

Документ фиксирует фактически реализованные инструменты `yandex-mcp-workspace-wiki`. Сервер предоставляет 31 MCP-инструмент: 3 общих (`system_*`, `yandex_auth_status`), 8 инструментов чтения Wiki и таблиц, 20 изменяющих инструментов страниц, вложений и динамических таблиц.

## Как это работает

Инструменты обращаются к REST API Wiki. Базовый адрес по умолчанию — `https://api.wiki.yandex.net` (переменная `YANDEX_WIKI_BASE_URL`, только в модуле Wiki). В каждый запрос сервер добавляет:

- `Authorization: OAuth <токен>`;
- `X-Org-ID` для `YANDEX_360` или `X-Cloud-Org-ID` для `YANDEX_CLOUD`.

Содержимое страниц передаётся в параметрах `content` в формате Markdown. Для операций с динамическими таблицами тело запроса передаётся как JSON-строка в параметре `body`.

## Общие инструменты сервера

| Инструмент | Тип | Назначение |
|---|---|---|
| `system_ping` | R | Проверка доступности сервера |
| `system_server_info` | R | Информация о режиме чтения/записи |
| `yandex_auth_status` | R | Состояние OAuth-настроек и сохранённого токена |

## Инструменты Wiki

В таблицах: `R` — чтение, `W` — изменение.

### Страницы

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `wiki_page_get_by_slug` | Получить страницу по slug | `GET /v1/pages?slug=...` | R |
| `wiki_page_get_by_id` | Получить страницу по id | `GET /v1/pages/{id}` | R |
| `wiki_page_get_descendants` | Получить дерево вложенных страниц | `GET /v1/pages/descendants?slug=...` | R |
| `wiki_page_get_resources` | Получить ресурсы страницы | `GET /v1/pages/{id}/resources` | R |
| `wiki_page_create` | Создать страницу | `POST /v1/pages` | W |
| `wiki_page_update` | Изменить заголовок, содержимое или поля | `POST /v1/pages/{id}` | W |
| `wiki_page_delete` | Удалить страницу | `DELETE /v1/pages/{id}` | W |
| `wiki_page_recover` | Восстановить страницу по токену | `POST /v1/recovery_tokens/{token}/recover` | W |
| `wiki_page_clone` | Клонировать страницу | `POST /v1/pages/{id}/clone` | W |
| `wiki_page_append_content` | Дописать Markdown-содержимое | `POST /v1/pages/{id}/append-content` | W |

### Комментарии и вложения

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `wiki_page_comments_list` | Получить комментарии страницы | `GET /v1/pages/{id}/comments` | R |
| `wiki_page_comment_add` | Добавить комментарий или ответ | `POST /v1/pages/{id}/comments` | W |
| `wiki_page_attachments_list` | Получить вложения страницы | `GET /v1/pages/{id}/attachments` | R |
| `wiki_page_attachment_upload` | Загрузить локальный файл и прикрепить к странице | `POST /v1/upload_sessions` -> `PUT /v1/upload_sessions/{id}/upload_part` -> `POST /v1/upload_sessions/{id}/finish` -> `POST /v1/pages/{id}/attachments` | W |
| `wiki_page_attachment_attach` | Прикрепить завершённые сессии загрузки | `POST /v1/pages/{id}/attachments` | W |

### Динамические таблицы

| Инструмент | Действие | Метод и endpoint | Тип |
|---|---|---|---|
| `wiki_grid_get` | Получить таблицу | `GET /v1/grids/{id}` | R |
| `wiki_page_grids_list` | Получить таблицы страницы | `GET /v1/pages/{id}/grids` | R |
| `wiki_grid_create` | Создать таблицу | `POST /v1/grids` | W |
| `wiki_grid_update` | Изменить таблицу | `POST /v1/grids/{id}` | W |
| `wiki_grid_delete` | Удалить таблицу | `DELETE /v1/grids/{id}` | W |
| `wiki_grid_clone` | Клонировать таблицу | `POST /v1/grids/{id}/clone` | W |
| `wiki_grid_add_rows` | Добавить строки | `POST /v1/grids/{id}/rows` | W |
| `wiki_grid_delete_rows` | Удалить строки | `DELETE /v1/grids/{id}/rows` с телом | W |
| `wiki_grid_move_row` | Переместить строку | `POST /v1/grids/{id}/rows/move` | W |
| `wiki_grid_add_columns` | Добавить столбцы | `POST /v1/grids/{id}/columns` | W |
| `wiki_grid_delete_columns` | Удалить столбцы | `DELETE /v1/grids/{id}/columns` с телом | W |
| `wiki_grid_move_column` | Переместить столбец | `POST /v1/grids/{id}/columns/move` | W |
| `wiki_grid_update_cells` | Обновить значения ячеек | `POST /v1/grids/{id}/cells` | W |

## Формат содержимого страниц

Содержимое страниц передаётся и принимается в формате **Markdown**. Это относится к `wiki_page_create`, `wiki_page_update` и `wiki_page_append_content`.

Дописывание содержимого поддерживает взаимоисключающие способы размещения (только один из них):

- `body.location`: `bottom` или `top` — вставка в начало или конец страницы;
- `anchor.name`: якорь, к которому нужно выполнить вставку (например, `#heading`).

В MCP-инструменте `wiki_page_append_content` это соответствует параметрам `location` и `anchor`.

## Загрузка вложений

`wiki_page_attachment_upload` читает файл по локальному пути, доступному серверу, создаёт сессию загрузки, отправляет файл частями до 5 МБ, завершает сессию и прикрепляет её к странице. Для Docker-запуска файл должен находиться в подключённом томе или другом пути, доступном контейнеру.

## Режим только для чтения

При `YANDEX_READ_ONLY=true` основные изменяющие инструменты `WikiWriteTools` не регистрируются в `tools/list`. Инструменты `WikiGridTools` содержат и чтение, и запись, поэтому регистрируются целиком; изменяющие операции таблиц отклоняются `WriteGuard` до обращения к API.

## Не реализовано сейчас

В текущем коде нет инструментов для управления правами доступа к страницам и низкоуровневого ручного управления сессиями загрузки как отдельными MCP-инструментами. Сессии загрузки используются внутри `wiki_page_attachment_upload`.
