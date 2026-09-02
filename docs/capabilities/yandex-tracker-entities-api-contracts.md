# Контракты унифицированного Entities API Yandex Tracker

## Содержание

- [Статус и назначение документа](#статус-и-назначение-документа)
- [Что входит в Entities API](#что-входит-в-entities-api)
- [Общие правила внешнего API](#общие-правила-внешнего-api)
- [Модель сущности](#модель-сущности)
- [Каталог endpoint](#каталог-endpoint)
- [Подробные контракты внешнего API](#подробные-контракты-внешнего-api)
- [Контракты MCP-инструментов](#контракты-mcp-инструментов)
- [Безопасные сценарии работы агента](#безопасные-сценарии-работы-агента)
- [Ошибки, повторы и конкурентные изменения](#ошибки-повторы-и-конкурентные-изменения)
- [Требования к реализации](#требования-к-реализации)
- [Критерии приёмки](#критерии-приёмки)
- [Открытые вопросы](#открытые-вопросы)
- [Источники](#источники)

## Статус и назначение документа

Документ описывает:

1. Подтверждённые официальной документацией HTTP-контракты унифицированного Entities API Yandex Tracker.
2. Реализованные контракты MCP-инструментов для `yandex-mcp-workspace-tracker`.
3. Правила валидации, защиты от ошибочных изменений и подтверждения результата после записи.

Контракты подготовлены для разработчиков MCP-сервера, QA и авторов Skills. Перечисленные MCP-инструменты реализованы в модуле `yandex-mcp-workspace-tracker`; этот документ является их нормативным контрактом.

Актуальность проверки официальной документации: **2 сентября 2026 года**.

## Что входит в Entities API

Entities API — единый API для трёх типов объектов:

| `entityType` | Объект | Назначение |
|---|---|---|
| `project` | Проект | Объединяет задачи, участников, сроки и ожидаемый результат работы |
| `portfolio` | Портфель | Объединяет проекты и другие портфели |
| `goal` | Цель | Описывает ожидаемый результат и позволяет оценивать прогресс по ключевым результатам |

API поддерживает:

- создание, чтение, изменение и удаление сущностей;
- поиск и пакетное изменение;
- историю событий;
- комментарии;
- чек-листы проектов и портфелей;
- ключевые результаты целей;
- метрики;
- вложения;
- связи сущностей;
- настройки доступа и наследование прав.

Старый API `/v3/projects` не входит в область этого документа. Для новой реализации следует использовать `/v3/entities`, поскольку он одинаково работает с проектами, портфелями и целями.

## Общие правила внешнего API

### Базовый адрес

```text
https://api.tracker.yandex.net
```

Все рассматриваемые методы используют версию `/v3`.

### Авторизация и организация

Каждый запрос должен содержать:

```http
Authorization: OAuth <OAuth-токен>
X-Org-ID: <идентификатор организации>
```

или для организации Yandex Identity Hub:

```http
Authorization: Bearer <IAM-токен>
X-Cloud-Org-ID: <идентификатор организации>
```

Для JSON-запросов дополнительно используется:

```http
Content-Type: application/json
```

Операция выполняется с правами пользователя или сервисного аккаунта, которому принадлежит токен. Наличие `tracker:write` не отменяет объектные ограничения доступа в Tracker.

### Идентификатор сущности

В URI используется:

```text
/v3/entities/{entityType}/{entityId}
```

В качестве `entityId` большинство методов принимают:

- `id` — внутренний строковый идентификатор Entities API;
- `shortId` — числовой идентификатор, отображаемый в интерфейсе Tracker.

MCP должен принимать идентификатор как строку, не пытаться определить его тип и не преобразовывать числовой `shortId` в число JavaScript.

### Выбор дополнительных полей

Основные метаданные сущности возвращаются на верхнем уровне. Предметные данные находятся в объекте `fields` и включаются параметром запроса:

```text
?fields=summary,description,entityStatus,parentEntity
```

Параметр MCP `fields` предлагается принимать строкой с ключами через запятую. Пустые элементы и пробелы должны удаляться до отправки запроса.

### Расширение ответа

Для чтения сущности и части изменяющих методов поддерживается:

```text
?expand=attachments
```

Не следует автоматически запрашивать вложения: это увеличивает ответ и может раскрыть метаданные файлов, которые не нужны вызывающему агенту.

### Изменение коллекций

Внутри `fields` Tracker поддерживает операторы:

- `set` — заменить значение или коллекцию;
- `add` — добавить элементы;
- `remove` — удалить элементы;
- `replace` — заменить отдельные элементы;
- `null` — очистить значение;
- `[]` — очистить коллекцию.

Пример добавления дополнительного портфеля без замены существующих:

```json
{
  "fields": {
    "parentEntity": {
      "secondary": {
        "add": ["670e702fd000000000000001"]
      }
    }
  }
}
```

MCP не должен превращать `add` в полную замену массива.

### Пагинация

Поиск сущностей использует обычную пагинацию:

| Параметр | Тип | Значение по умолчанию |
|---|---:|---:|
| `perPage` | integer | `50` |
| `page` | integer | `1` |

История событий и комментарии имеют относительную пагинацию:

| Параметр | Назначение |
|---|---|
| `perPage` | Максимальное число элементов, по умолчанию `50` |
| `from` | Начать после указанного элемента; сам элемент не включается |
| `selected` | Сформировать страницу вокруг указанного элемента |
| `newEventsOnTop` / `newCommentsOnTop` | Изменить базовый порядок |
| `direction` | `forward` или `backward` |

`from` и `selected` взаимоисключающие. MCP должен отклонять запрос, если переданы оба параметра.

## Модель сущности

### Базовый ответ `Entity`

```json
{
  "self": "https://api.tracker.yandex.net/v3/entities/project/655f328da000000000000001",
  "id": "655f328da000000000000001",
  "version": 3,
  "shortId": 2,
  "entityType": "project",
  "createdBy": {
    "self": "https://api.tracker.yandex.net/v3/users/1000000000000001",
    "id": "1000000000000001",
    "display": "Имя Фамилия",
    "passportUid": 1000000000000001,
    "cloudUid": "aje00000000000000001"
  },
  "createdAt": "2026-08-01T10:00:00.000+0000",
  "updatedAt": "2026-08-10T12:30:00.000+0000",
  "fields": {
    "summary": "Запуск продукта",
    "entityStatus": "in_progress"
  }
}
```

| Поле | Тип | Назначение |
|---|---|---|
| `self` | string | URL ресурса |
| `id` | string | Внутренний идентификатор Entities API |
| `version` | integer | Версия, увеличивающаяся после каждого изменения |
| `shortId` | integer | Идентификатор из интерфейса Tracker |
| `entityType` | string | `project`, `portfolio` или `goal` |
| `createdBy` | object | Создатель сущности |
| `createdAt` | string | Дата и время создания |
| `updatedAt` | string | Дата и время последнего изменения |
| `attachments` | array | Вложения, если запрошен `expand=attachments` |
| `fields` | object | Запрошенные предметные поля |

### Основные изменяемые поля

| Поле | Тип | Применимость | Особенности |
|---|---|---|---|
| `summary` | string | все | Обязательно при создании |
| `description` | string | все | Поддерживает Yandex Flavored Markdown |
| `markupType` | string | все | Для YFM передаётся `md` |
| `author` | string | все | Логин или идентификатор пользователя |
| `lead` | string | все | Ответственный |
| `teamUsers` | string[] | все | Участники |
| `clients` | string[] | все | Заказчики |
| `followers` | string[] | все | Наблюдатели |
| `start` | date-time | project, portfolio | Дата начала |
| `end` | date-time | все | Дедлайн |
| `tags` | string[] | все | Теги |
| `parentEntity` | object | все | Основной и дополнительные родители |
| `teamAccess` | boolean | все | Не действует при наследовании ACL |
| `entityStatus` | string | все | Набор значений зависит от типа сущности |

### Дополнительные и вычисляемые поля

| Поле | Тип | Применимость | Изменение |
|---|---|---|---|
| `quarter` | string[] | project, portfolio | В официальном CRUD-контракте изменение отдельно не описано |
| `metricItems` | object[] | все | Изменяется через `PATCH` сущности |
| `checklistItems` | object[] | project, portfolio | Через `PATCH` сущности или специализированные endpoint |
| `keyResultItems` | object[] | goal | Изменяется через `PATCH` цели |
| `progressPercentage` | number или null | goal | Только чтение, диапазон `0..1` |
| `issueQueues` | object[] | project | Только чтение |
| `lastCommentUpdatedAt` | string | все | Только чтение |
| `linkedGoalsCount` | integer | project, portfolio | Только чтение |
| `linkedProjectsCount` | integer | goal | Только чтение |

MCP должен отклонять попытки записи явно read-only полей.

### `parentEntity`

Запрос на создание или изменение:

```json
{
  "primary": "67ffd7e3a000000000000001",
  "secondary": [
    "67ffd7e3a000000000000002"
  ]
}
```

| Поле | project / portfolio | goal |
|---|---|---|
| `primary` | Основной портфель | Родительская цель |
| `secondary` | Дополнительные портфели | Не поддерживается |

### Статусы проектов и портфелей

| Значение | Смысл |
|---|---|
| `draft` | Новый |
| `draft2` | Черновик |
| `in_progress` | В работе |
| `according_to_plan` | По плану |
| `postponed` | Отложен |
| `at_risk` | Есть риски |
| `blocked` | Заблокирован |
| `launched` | Завершён |
| `cancelled` | Отменён |

### Статусы целей

| Значение | Смысл |
|---|---|
| `draft` | Новая |
| `according_to_plan` | По плану |
| `at_risk` | Есть риски |
| `blocked` | Заблокирована |
| `achieved` | Достигнута |
| `partially_achieved` | Частично достигнута |
| `not_achieved` | Не достигнута |
| `exceeded` | Превышена |
| `cancelled` | Отменена |

### Ключевой результат цели

```json
{
  "type": "value",
  "text": "Достичь 100 активных клиентов",
  "assignee": "username",
  "deadline": {
    "date": "2026-12-31",
    "deadlineType": "date"
  },
  "progress": {
    "start": 0,
    "end": 100,
    "current": 42
  }
}
```

| Поле | Тип | Условие |
|---|---|---|
| `type` | string | Обязательно: `value` или `binary` |
| `text` | string | Обязательно |
| `assignee` | string или integer | Логин или идентификатор |
| `deadline.date` | string | `YYYY-MM-DD` |
| `deadline.deadlineType` | string | Для ключевого результата — `date` |
| `progress.start` | number | Обязательно при `type=value` |
| `progress.end` | number | Обязательно при `type=value` |
| `progress.current` | number | Текущее значение при `type=value` |
| `achieved` | boolean | Используется при `type=binary` |

### Метрика

```json
{
  "text": "Cycle time",
  "url": "https://tracker.yandex.ru/dashboard/12/widget/34?_embedded=1&_no_controls=1"
}
```

`text` обязателен. `url` содержит ссылку на виджет, пригодную для встраивания через `iframe`.

## Каталог endpoint

### Основные операции

| Операция | Метод и путь | Успех |
|---|---|---:|
| Создать сущность | `POST /v3/entities/{type}` | `201` |
| Получить сущность | `GET /v3/entities/{type}/{id}` | `200` |
| Изменить сущность | `PATCH /v3/entities/{type}/{id}` | `200` |
| Удалить сущность | `DELETE /v3/entities/{type}/{id}` | `204` |
| Найти сущности | `POST /v3/entities/{type}/_search` | `200` |
| Пакетно изменить | `POST /v3/entities/{type}/bulkchange/_update` | `200` |
| Получить события | `GET /v3/entities/{type}/{id}/events/_relative` | `200` |

### Комментарии

| Операция | Метод и путь | Успех |
|---|---|---:|
| Получить все комментарии | `GET /v3/entities/{type}/{id}/comments` | `200` |
| Получить страницу относительно курсора | `GET /v3/entities/{type}/{id}/comments/_relative` | `200` |
| Получить комментарий | `GET /v3/entities/{type}/{id}/comments/{commentId}` | `200` |
| Добавить комментарий | `POST /v3/entities/{type}/{id}/comments` | `201` |
| Изменить комментарий | `PATCH /v3/entities/{type}/{id}/comments/{commentId}` | `200` |
| Удалить комментарий | `DELETE /v3/entities/{type}/{id}/comments/{commentId}` | `204` |

### Чек-листы проектов и портфелей

| Операция | Метод и путь | Успех |
|---|---|---:|
| Получить чек-лист | `GET /v3/entities/{type}/{id}?fields=checklistItems` | `200` |
| Добавить пункты | `POST /v3/entities/{type}/{id}/checklistItems` | `200` |
| Заменить содержимое | `PATCH /v3/entities/{type}/{id}/checklistItems` | `200` |
| Изменить пункт | `PATCH /v3/entities/{type}/{id}/checklistItems/{itemId}` | `200` |
| Переместить пункт | `POST /v3/entities/{type}/{id}/checklistItems/{itemId}/_move` | `200` |
| Удалить пункт | `DELETE /v3/entities/{type}/{id}/checklistItems/{itemId}` | `200` |
| Удалить весь чек-лист | `DELETE /v3/entities/{type}/{id}/checklistItems` | `200` |

### Вложения

| Операция | Метод и путь | Успех |
|---|---|---:|
| Получить список | `GET /v3/entities/{type}/{id}/attachments` | `200` |
| Получить метаданные файла | `GET /v3/entities/{type}/{id}/attachments/{fileId}` | `200` |
| Прикрепить временный файл | `POST /v3/entities/{type}/{id}/attachments/{temporaryFileId}` | `200` |
| Удалить файл | `DELETE /v3/entities/{type}/{id}/attachments/{fileId}` | `204` |

Перед прикреплением файл загружается как временный через общий endpoint Tracker `POST /v3/attachments/`. Endpoint Entities принимает **идентификатор временного файла**, а не локальный путь и не содержимое файла.

### Связи сущностей

| Операция | Метод и путь | Успех |
|---|---|---:|
| Получить связи | `GET /v3/entities/{type}/{id}/links` | `200` |
| Создать связи | `POST /v3/entities/{type}/{id}/links` | `200` |
| Удалить связь | `DELETE /v3/entities/{type}/{id}/links?right={rightId}` | `200` |

### Настройки доступа

| Операция | Метод и путь | Особенности |
|---|---|---|
| Получить полный ACL | `GET /v3/entities/{type}/{id}/extendedPermissions` | Возвращает ACL, наследование и родителей |
| Получить только ACL | `GET /v3/entities/{type}/{id}/permissions` | Не возвращает `permissionSources` |
| Изменить ACL и наследование | `PATCH /v3/entities/{type}/{id}/extendedPermissions` | Предпочтительный endpoint |
| Изменить только ACL | `PATCH /v3/entities/{type}/{id}/permissions` | Не управляет наследованием |

## Подробные контракты внешнего API

### Создание сущности

```http
POST /v3/entities/{entityType}?fields=summary,entityStatus
Content-Type: application/json
```

```json
{
  "fields": {
    "summary": "Запуск продукта",
    "description": "Описание проекта",
    "markupType": "md",
    "lead": "username",
    "end": "2026-12-01T00:00:00.000+0000",
    "entityStatus": "draft"
  },
  "links": [
    {
      "relationship": "works towards",
      "entity": "655f328da000000000000010"
    }
  ]
}
```

Обязателен объект `fields`, внутри него обязательно поле `summary`. Ответ — созданный `Entity` и статус `201 Created`.

### Получение сущности

```http
GET /v3/entities/{entityType}/{entityId}?fields=summary,description,entityStatus&expand=attachments
```

Ответ содержит базовые поля и только запрошенные дополнительные поля. Если агенту нужен `summary`, он должен явно передать его в `fields`.

### Изменение сущности

```http
PATCH /v3/entities/{entityType}/{entityId}?fields=summary,entityStatus
Content-Type: application/json
```

```json
{
  "fields": {
    "summary": "Новое название",
    "followers": {
      "add": ["username"]
    }
  },
  "comment": "Обновлены параметры проекта",
  "links": [
    {
      "relationship": "depends on",
      "entity": "655f328da000000000000020"
    }
  ]
}
```

Все поля тела опциональны, но пустое тело не имеет полезного эффекта и должно отклоняться MCP-валидацией. Ответ — обновлённый `Entity`.

### Удаление сущности

```http
DELETE /v3/entities/{entityType}/{entityId}?withBoard=true
```

`withBoard` означает удаление связанной доски. В MCP параметр следует разрешать только для `entityType=project`. Успешный ответ не имеет тела (`204`).

### Поиск сущностей

```http
POST /v3/entities/{entityType}/_search?fields=summary,entityStatus&perPage=50&page=1
Content-Type: application/json
```

```json
{
  "input": "запуск",
  "filter": {
    "followers": "notEmpty()",
    "entityStatus": "in_progress"
  },
  "orderBy": "entityStatus",
  "orderAsc": true,
  "rootOnly": false
}
```

Все поля тела опциональны. Пустое тело означает выборку без дополнительного фильтра.

Официальный ответ:

```json
{
  "hits": 8,
  "pages": 1,
  "values": [
    {
      "id": "655f328da000000000000001",
      "shortId": 2,
      "entityType": "project",
      "fields": {
        "summary": "Запуск продукта",
        "entityStatus": "in_progress"
      }
    }
  ]
}
```

Для единообразия MCP должен преобразовать `values` в `items`, `hits` в `totalCount`, `pages` в `totalPages`.

### Пакетное изменение

```http
POST /v3/entities/{entityType}/bulkchange/_update
Content-Type: application/json
```

```json
{
  "metaEntities": [
    "655f328da000000000000001",
    "655f328da000000000000002"
  ],
  "values": {
    "fields": {
      "entityStatus": "at_risk",
      "followers": {
        "add": ["username"]
      }
    },
    "comment": "Проекты переведены в зону риска"
  }
}
```

Обязательны `metaEntities` и `values`. Ответ описывает **созданную пакетную операцию**, а не гарантирует завершение обновления:

```json
{
  "id": "6560c6f59b0b1e7600000001",
  "self": "https://api.tracker.yandex.net/v3/bulkchange/6560c6f59b0b1e7600000001",
  "status": "CREATED",
  "statusText": "Операция массового редактирования задач создана.",
  "executionChunkPercent": 0,
  "executionIssuePercent": 0
}
```

После запуска необходимо опрашивать общий ресурс `/v3/bulkchange/{operationId}` до терминального состояния и отдельно обрабатывать ошибки элементов.

Связанные общие методы Tracker:

| Операция | Метод и путь | Успех |
|---|---|---:|
| Получить статус | `GET /v3/bulkchange/{operationId}` | `200` |
| Получить элементы с ошибками | `GET /v3/bulkchange/{operationId}/issues` | `200` |

Документация второго метода описывает ошибки задач. Его фактическую совместимость с пакетными операциями Entities необходимо подтвердить интеграционным тестом до публикации MCP-контракта как гарантированного.

### История событий

```http
GET /v3/entities/{entityType}/{entityId}/events/_relative?perPage=50&from={eventId}
```

```json
{
  "events": [
    {
      "id": "65a26b254dbe621200000001",
      "author": {
        "id": "1000000000000001",
        "display": "Имя Фамилия"
      },
      "date": "2026-08-10T12:30:00.000+0000",
      "transport": "v3",
      "display": "Issue updated",
      "changes": [
        {
          "diff": "<added>username</added>",
          "field": {
            "id": "teamUsers",
            "display": "Participants"
          }
        }
      ]
    }
  ],
  "hasNext": true,
  "hasPrev": false
}
```

`changes` является полиморфным массивом: состав полей зависит от события. MCP не должен отбрасывать неизвестные поля.

### Комментарии

Добавление:

```http
POST /v3/entities/{entityType}/{entityId}/comments?expand=html,attachments
Content-Type: application/json
```

```json
{
  "text": "Текст комментария",
  "attachmentIds": ["30"],
  "summonees": ["username"],
  "maillistSummonees": ["team@example.com"]
}
```

`text` обязателен. `attachmentIds` содержит идентификаторы предварительно загруженных временных файлов.

Чтение поддерживает `expand`:

- `all`;
- `html`;
- `attachments`;
- `reactions`.

Изменение использует тот же набор полей, но URI содержит `commentId`. Удаление возвращает `204`.

### Чек-листы

Чек-листы доступны только для `project` и `portfolio`.

Добавление одного или нескольких пунктов:

```http
POST /v3/entities/{entityType}/{entityId}/checklistItems?fields=checklistItems
Content-Type: application/json
```

```json
[
  {
    "text": "Согласовать план",
    "checked": false,
    "assignee": "username",
    "deadline": {
      "date": "2026-09-30T00:00:00.000+0000",
      "deadlineType": "date"
    }
  }
]
```

У всех специализированных изменяющих методов чек-листа есть параметры:

| Параметр | По умолчанию | Назначение |
|---|---:|---|
| `notify` | `true` | Уведомить автора, ответственного, участников, заказчиков и наблюдателей |
| `notifyAuthor` | `false` | Уведомить автора изменения |
| `fields` | — | Поля сущности в ответе |
| `expand` | — | Дополнительные блоки, включая `attachments` |

Полная замена чек-листа через `PATCH .../checklistItems` требует `id` и `text` каждого пункта. Количество пунктов нельзя менять этим методом. Критичное поведение: неуказанные дополнительные свойства пункта сбрасываются к значениям по умолчанию. Поэтому MCP должен использовать полную замену только после чтения текущего чек-листа.

Перемещение пункта:

```http
POST /v3/entities/{entityType}/{entityId}/checklistItems/{itemId}/_move
Content-Type: application/json
```

```json
{
  "before": "6586d6fee2b9ef7200000001"
}
```

### Ключевые результаты и метрики

Отдельных HTTP endpoint для элементов нет. Используются `GET` и `PATCH` сущности:

```http
GET /v3/entities/goal/{goalId}?fields=keyResultItems
PATCH /v3/entities/goal/{goalId}?fields=keyResultItems
```

```json
{
  "fields": {
    "keyResultItems": {
      "add": {
        "type": "binary",
        "text": "Запустить новую версию",
        "assignee": "username",
        "achieved": false
      }
    }
  }
}
```

Для очистки всего списка передаётся `null`. Для удаления одного элемента оператору `remove` требуется полный объект в формате ответа API, а не только `id`. Перед точечным удалением MCP должен перечитать актуальный список.

Для метрик используется тот же принцип с полем `metricItems`.

### Вложения

Последовательность добавления:

1. Загрузить файл во временное хранилище Tracker через `POST /v3/attachments/`.
2. Получить `temporaryFileId`.
3. Вызвать `POST /v3/entities/{type}/{id}/attachments/{temporaryFileId}`.
4. Перечитать список вложений и проверить появление файла.

Параметры прикрепления:

| Параметр | По умолчанию | Назначение |
|---|---:|---|
| `notify` | `true` | Уведомить участников сущности |
| `notifyAuthor` | `false` | Уведомить автора изменения |
| `fields` | — | Вернуть дополнительные поля сущности |
| `expand` | — | Например, `attachments` |

Метод `GET .../attachments/{fileId}` возвращает метаданные, включая `content` и `thumbnail`. Скачивание содержимого выполняется по URL из `content`; MCP-клиент должен использовать те же заголовки авторизации и организации.

### Связи сущностей

Типы связей зависят от исходной сущности:

| Исходная сущность | `relationship` | Смысл |
|---|---|---|
| project / portfolio | `depends on` | Зависит от связанной сущности |
| project / portfolio | `is dependent by` | Блокирует связанную сущность |
| project | `works towards` | Проект работает на цель |
| goal | `parent entity` | Родительская цель |
| goal | `child entity` | Подцель |
| goal | `depends on` | Цель зависит от связанной |
| goal | `is dependent by` | Цель блокирует связанную |
| goal | `is supported by` | Цель поддерживается проектом |

Создание принимает JSON-массив, даже если добавляется одна связь:

```json
[
  {
    "relationship": "works towards",
    "entity": "65868f3fe2b9ef7400000001"
  }
]
```

Родительский портфель проекта или портфеля следует менять через `parentEntity`, а не через `/links`.

Перед созданием MCP должен получить текущие связи и проверить пару `relationship + entity`. После `200 OK` нужно повторно получить связи, поскольку ответ создания не является достаточным подтверждением состава связей.

### Настройки доступа

Предпочтительный контракт:

```http
GET /v3/entities/{entityType}/{entityId}/extendedPermissions
PATCH /v3/entities/{entityType}/{entityId}/extendedPermissions
```

Тело изменения:

```json
{
  "permissionSources": [],
  "acl": {
    "grant": {
      "READ": {
        "users": ["username"],
        "groups": [2],
        "roles": ["MEMBER"]
      }
    },
    "revoke": {
      "WRITE": {
        "users": ["former-owner"]
      }
    }
  }
}
```

Типы доступа:

| Тип | Назначение |
|---|---|
| `READ` | Просмотр сущности |
| `WRITE` | Редактирование сущности |
| `GRANT` | Управление настройками доступа |

Роли:

| Роль | Поле сущности |
|---|---|
| `AUTHOR` | Автор |
| `OWNER` | Ответственный |
| `CLIENT` | Заказчики |
| `FOLLOWER` | Наблюдатели |
| `MEMBER` | Участники |

Если `permissionSources` непустой:

- текущая сущность наследует права от основного портфеля или родительской цели;
- `acl` нельзя изменить;
- `teamAccess` не действует.

Чтобы изменить собственный ACL, сначала нужно отключить наследование значением `"permissionSources": []`.

## Контракты MCP-инструментов

### Общие правила MCP

1. `entityType` обязателен и валидируется по enum `project | portfolio | goal`.
2. `entityId` всегда строка.
3. Сложные произвольные объекты принимаются JSON-строкой, как параметр `fields` существующих Tracker-инструментов.
4. Read-инструменты регистрируются всегда.
5. Write-инструменты не регистрируются при `YANDEX_READ_ONLY=true` и дополнительно проходят через `WriteGuard`.
6. MCP возвращает JSON, а не человекочитаемую строку, включая подтверждения удаления.
7. Неизвестные поля ответа сохраняются.

### Основные инструменты

#### `tracker_entity_get`

| Параметр | Тип | Обязательный | Описание |
|---|---|---:|---|
| `entityType` | string | да | `project`, `portfolio`, `goal` |
| `entityId` | string | да | `id` или `shortId` |
| `fields` | string | нет | Ключи через запятую |
| `expand` | string | нет | Сейчас поддерживается `attachments` |

Возвращает официальный объект `Entity`.

#### `tracker_entity_search`

| Параметр | Тип | Обязательный | Описание |
|---|---|---:|---|
| `entityType` | string | да | Тип сущности |
| `input` | string | нет | Подстрока названия |
| `filter` | string | нет | JSON-объект фильтра |
| `orderBy` | string | нет | Ключ поля сортировки |
| `orderAsc` | boolean | нет | Направление сортировки |
| `rootOnly` | boolean | нет | Только корневые сущности |
| `fields` | string | нет | Поля ответа |
| `perPage` | integer | нет | Размер страницы; по умолчанию `50` |
| `page` | integer | нет | Начиная с `1` |

Нормализованный ответ:

```json
{
  "items": [],
  "totalCount": 0,
  "totalPages": 0,
  "page": 1,
  "perPage": 50
}
```

#### `tracker_entity_create`

| Параметр | Тип | Обязательный | Описание |
|---|---|---:|---|
| `entityType` | string | да | Тип создаваемой сущности |
| `summary` | string | да | Непустое название |
| `description` | string | нет | Описание |
| `lead` | string | нет | Ответственный |
| `start` | string | нет | Только project / portfolio |
| `end` | string | нет | Дедлайн |
| `entityStatus` | string | нет | Статус из enum соответствующего типа |
| `parentEntity` | string | нет | JSON-объект `primary` / `secondary` |
| `fields` | string | нет | Остальные поля JSON; не может переопределять `summary` конфликтующим значением |
| `links` | string | нет | JSON-массив начальных связей |
| `responseFields` | string | нет | Значение query-параметра `fields` |

Возвращает созданный `Entity`.

#### `tracker_entity_update`

| Параметр | Тип | Обязательный | Описание |
|---|---|---:|---|
| `entityType` | string | да | Тип сущности |
| `entityId` | string | да | Идентификатор |
| `fields` | string | нет | JSON-объект изменений |
| `comment` | string | нет | Комментарий к изменению |
| `links` | string | нет | JSON-массив добавляемых связей |
| `responseFields` | string | нет | Поля ответа |
| `expand` | string | нет | Дополнительные блоки ответа |

Минимум один из `fields`, `comment`, `links` обязателен.

#### `tracker_entity_delete`

| Параметр | Тип | Обязательный | Описание |
|---|---|---:|---|
| `entityType` | string | да | Тип сущности |
| `entityId` | string | да | Идентификатор |
| `withBoard` | boolean | нет | Только для проекта |

Ответ MCP:

```json
{
  "deleted": true,
  "entityType": "project",
  "entityId": "655f328da000000000000001",
  "withBoard": false
}
```

#### `tracker_entity_bulk_update`

| Параметр | Тип | Обязательный | Описание |
|---|---|---:|---|
| `entityType` | string | да | Один тип для всей операции |
| `entityIds` | string | да | Идентификаторы через запятую |
| `fields` | string | нет | JSON-объект изменений |
| `comment` | string | нет | Комментарий |
| `links` | string | нет | JSON-массив связей |

Минимум одно изменение обязательно. Пустой список идентификаторов и повторяющиеся идентификаторы отклоняются.

Пакетная операция требует общих инструментов:

| Инструмент | Параметры | Результат |
|---|---|---|
| `tracker_bulk_operation_get` | `operationId` | Статус и процент выполнения |
| `tracker_bulk_operation_error_list` | `operationId` | Элементы, для которых операция завершилась ошибкой; для Entities включается после интеграционной проверки |

#### `tracker_entity_event_list`

Параметры: `entityType`, `entityId`, `perPage`, `from`, `selected`, `newEventsOnTop`, `direction`. Ответ нормализуется в `items`, `hasNext`, `hasPrev`.

### Комментарии

| Инструмент | Основные параметры | Результат |
|---|---|---|
| `tracker_entity_comment_list` | `entityType`, `entityId`, `expand`, относительная пагинация | Комментарии и курсоры |
| `tracker_entity_comment_get` | `entityType`, `entityId`, `commentId`, `expand` | Один комментарий |
| `tracker_entity_comment_add` | `entityType`, `entityId`, `text`, `attachmentIds`, `summonees`, `maillistSummonees`, `expand` | Созданный комментарий |
| `tracker_entity_comment_update` | те же идентификаторы, изменяемые поля | Обновлённый комментарий |
| `tracker_entity_comment_delete` | идентификаторы | JSON-подтверждение удаления |

Списки идентификаторов предлагается принимать через запятую, кроме случаев, когда API требует объект; сервис преобразует их в JSON-массивы.

### Чек-листы

| Инструмент | Назначение |
|---|---|
| `tracker_entity_checklist_list` | Вызвать `tracker_entity_get` с `fields=checklistItems` и вернуть массив пунктов |
| `tracker_entity_checklist_add` | Добавить один пункт с `text`, `checked`, `assignee`, `deadline` |
| `tracker_entity_checklist_replace` | Полностью заменить состояние существующих пунктов |
| `tracker_entity_checklist_item_update` | Изменить один пункт |
| `tracker_entity_checklist_item_move` | Переместить пункт перед `beforeItemId` |
| `tracker_entity_checklist_item_delete` | Удалить один пункт |
| `tracker_entity_checklist_clear` | Удалить весь чек-лист |

Каждый изменяющий инструмент принимает `notify`, `notifyAuthor`, `responseFields`, `expand`.

`replace` и `clear` должны быть явно помечены как destructive в описании MCP-инструмента. Перед `replace` сервис или Skill обязан получить актуальный чек-лист.

### Вложения

| Инструмент | Назначение |
|---|---|
| `tracker_temporary_attachment_upload` | Общая временная загрузка файла в Tracker |
| `tracker_entity_attachment_list` | Список метаданных |
| `tracker_entity_attachment_get` | Метаданные одного файла |
| `tracker_entity_attachment_attach` | Прикрепить `temporaryFileId` |
| `tracker_entity_attachment_delete` | Удалить прикреплённый файл |

Для будущего скачивания содержимого рекомендуется отдельный `tracker_entity_attachment_download`, который возвращает ресурс или безопасный локальный файл, а не вставляет бинарные данные в JSON MCP-ответа.

### Связи

| Инструмент | Параметры | Особенности |
|---|---|---|
| `tracker_entity_link_list` | `entityType`, `entityId`, `fields` | Поля относятся к связанным сущностям |
| `tracker_entity_link_create` | `entityType`, `entityId`, `relationship`, `rightEntityId` | Проверяет дубликат и перечитывает список |
| `tracker_entity_link_delete` | `entityType`, `entityId`, `rightEntityId` | Удаление адресуется по `right`, не по link id |

### Доступ

| Инструмент | Параметры | Endpoint |
|---|---|---|
| `tracker_entity_access_get` | `entityType`, `entityId`, `extended=true` | `extendedPermissions` по умолчанию |
| `tracker_entity_access_update` | `entityType`, `entityId`, `permissionSources`, `grant`, `revoke`, `extended=true` | `extendedPermissions` по умолчанию |

`grant` и `revoke` передаются JSON-объектами. Одновременное непустое `permissionSources` и изменение ACL должно отклоняться до HTTP-запроса.

### Ключевые результаты и метрики

Низкоуровневое управление доступно через `tracker_entity_get` и `tracker_entity_update`. Для безопасных сценариев Skills, управляющих OKR, также реализованы специализированные инструменты:

| Инструмент | Назначение |
|---|---|
| `tracker_goal_key_result_list` | Получить `keyResultItems` |
| `tracker_goal_key_result_add` | Добавить один элемент оператором `add` |
| `tracker_goal_key_result_update` | Перечитать список и заменить выбранный элемент |
| `tracker_goal_key_result_delete` | Перечитать и удалить полный объект оператором `remove` |
| `tracker_entity_metric_list` | Получить `metricItems` |
| `tracker_entity_metric_replace` | Заменить список метрик |
| `tracker_entity_metric_clear` | Передать `metricItems: null` |

## Безопасные сценарии работы агента

### Изменение сущности

```text
получить сущность с нужными fields
→ проверить entityType и текущие значения
→ сформировать минимальный PATCH
→ выполнить изменение
→ перечитать изменённые fields
→ вернуть подтверждённый результат
```

### Создание связи

```text
получить обе сущности
→ проверить совместимость типов связи
→ получить существующие связи
→ проверить дубликат
→ создать связь
→ перечитать связи
→ вернуть подтверждённую связь
```

### Изменение ACL

```text
получить extendedPermissions
→ определить наличие permissionSources
→ при необходимости отдельно отключить наследование
→ применить grant/revoke
→ повторно получить extendedPermissions
→ проверить фактический ACL
```

### Пакетное изменение

```text
проверить список идентификаторов
→ запустить bulkchange
→ получить operationId
→ опрашивать /v3/bulkchange/{operationId}
→ дождаться терминального состояния
→ проверить ошибки элементов
→ выборочно перечитать изменённые сущности
```

## Ошибки, повторы и конкурентные изменения

| Код | Смысл | Поведение MCP |
|---:|---|---|
| `400` | Некорректные параметры | Не повторять; вернуть детали валидации |
| `401` | Ошибка авторизации | Не повторять автоматически; предложить проверить токен |
| `403` | Недостаточно прав | Не повторять; указать объект и операцию |
| `404` | Сущность или дочерний ресурс не найден | Не повторять без исправления идентификатора |
| `409` | Конфликт при создании или изменении | Перечитать состояние перед повтором |
| `412` | Конфликт изменения | Перечитать сущность; не повторять старое тело вслепую |
| `422` | Ошибка семантической валидации | Не повторять без изменения запроса |
| `423` | Редактирование заблокировано | Не повторять; проверить предел версии |
| `428` | Не выполнены обязательные условия | Не повторять без исправления предусловий |
| `429` | Ограничение частоты | Повторить с задержкой, учитывая `Retry-After`, если он есть |
| `5xx` | Временная ошибка Tracker | Ограниченный retry с exponential backoff и jitter |

Tracker блокирует редактирование сущности, если `version` достигает предела:

- `10100` для роботов;
- `11100` для пользователей.

Повторять автоматически можно только идемпотентные операции чтения. Для `POST`, `PATCH` и `DELETE` после сетевой ошибки с неизвестным результатом сначала нужно перечитать состояние.

## Требования к реализации

### Клиент

- Добавить URI-builder для `/v3/entities/{type}` и дочерних ресурсов.
- Сохранять query-параметры `fields`, `expand`, относительной пагинации и уведомлений.
- Поддержать JSON-массив как корневое тело запросов связей и чек-листов.
- Не терять неизвестные поля ответов: использовать `JsonNode`.
- Добавить multipart-временную загрузку отдельно от JSON-клиента.
- Сохранять бинарные ответы вне JSON и не писать содержимое файлов в лог.

### Сервисы

- Разделить read- и write-сервисы, как в существующем Tracker-модуле.
- Централизовать проверку `entityType` и совместимости полей.
- Использовать существующий `WriteGuard` для каждой изменяющей операции.
- Нормализовать пагинацию в `PagedResult`.
- Для links, ACL и destructive-операций возвращать структурированное подтверждение.

### Логирование и секреты

- Не логировать `Authorization`.
- Не логировать бинарное содержимое файлов.
- Не логировать полный текст приватных комментариев на информационном уровне.
- В ошибке разрешено возвращать метод, путь без токена, HTTP-код и безопасное тело ответа Tracker.

## Критерии приёмки

### Основной CRUD

- Создаются `project`, `portfolio` и `goal` с минимальным полем `summary`.
- `id` и `shortId` одинаково работают при чтении.
- `fields` ограничивает состав `Entity.fields`.
- `expand=attachments` возвращает вложения.
- PATCH с `add` не заменяет существующую коллекцию.
- `tracker_entity_delete` не допускает `withBoard=true` для `portfolio` и `goal`.

### Поиск и история

- Поиск поддерживает `input`, `filter`, сортировку и `rootOnly`.
- Ответ поиска нормализован в `items`, `totalCount`, `totalPages`.
- `from` и `selected` нельзя передать одновременно.
- История сохраняет полиморфный массив `changes` без потери полей.

### Дочерние ресурсы

- Комментарий создаётся, читается, изменяется и удаляется.
- Чек-лист запрещён для `goal`.
- Полная замена чек-листа не выполняется без предварительного чтения в Skill-сценарии.
- Временный файл прикрепляется к сущности и подтверждается повторным чтением.
- Дубликат связи не создаётся.
- Удаление связи использует `rightEntityId`.
- ACL нельзя изменить при активном наследовании без явного отключения наследования.

### Режим read-only

- Все `tracker_entity_*` инструменты чтения присутствуют.
- Все инструменты создания, изменения и удаления отсутствуют в `tools/list`.
- Прямой вызов write-сервиса также блокируется `WriteGuard`.

## Открытые вопросы

| Вопрос | Почему требуется проверка |
|---|---|
| Возвращает ли `GET /v3/bulkchange/{id}/issues` ошибки пакетного изменения Entities, а не только задач? | Общая документация описывает элементы ответа как задачи, хотя Entities возвращает ссылку на тот же ресурс `bulkchange` |
| Какой фактический предел `metaEntities` в одной пакетной операции? | На странице Entities точное максимальное число не указано |
| Поддерживается ли `works towards` для `portfolio` или только для `project`? | Список типов связи объединён для проектов и портфелей, но описание называет связь проекта с целью |
| Должен ли `PATCH` комментария всегда содержать `commentId` в URI? | Пример официальной страницы содержит идентификатор, а её общий шаблон URI в одном месте его опускает |
| Какой формат результата должен использовать будущий инструмент скачивания файла? | Требуется выбрать между MCP resource, локальным временным файлом и отдельным бинарным транспортом |

Эти вопросы не блокируют реализацию базового CRUD, поиска, чтения событий, связей и ACL. Они должны быть закрыты интеграционными тестами перед включением соответствующих спорных возможностей в стабильный MCP-контракт.

## Источники

Основные официальные страницы:

- [Обзор Entities API и дополнительные поля](https://yandex.ru/support/tracker/ru/api/entities/about-entities)
- [Создать сущность](https://yandex.ru/support/tracker/ru/api/entities/create-entity)
- [Получить сущность](https://yandex.ru/support/tracker/ru/api/entities/get-entity)
- [Изменить сущность](https://yandex.ru/support/tracker/ru/api/entities/update-entity)
- [Удалить сущность](https://yandex.ru/support/tracker/ru/api/entities/delete-entity)
- [Найти сущности](https://yandex.ru/support/tracker/ru/api/entities/search-entities)
- [Массовое редактирование](https://yandex.ru/support/tracker/ru/api/entities/bulkchange-entities)
- [Статус пакетной операции и ошибки](https://yandex.ru/support/tracker/ru/api-ref/bulkchange/bulk-move-info)
- [История событий](https://yandex.ru/support/tracker/ru/api/entities/get-events-relative)
- [Комментарии](https://yandex.ru/support/tracker/ru/api/entities/comments/get-all-comments)
- [Чек-листы](https://yandex.ru/support/tracker/ru/api/entities/checklists/add-checklist)
- [Ключевые результаты](https://yandex.ru/support/tracker/ru/api/entities/keyresults)
- [Метрики](https://yandex.ru/support/tracker/ru/api/entities/metric)
- [Вложения](https://yandex.ru/support/tracker/ru/api/entities/attachments/get-all-attachments)
- [Связи](https://yandex.ru/support/tracker/ru/api/entities/links/get-links)
- [Настройки доступа](https://yandex.ru/support/tracker/ru/api/entities/get-access)
- [Изменить настройки доступа](https://yandex.ru/support/tracker/ru/api/entities/patch-access)
