package com.sorface.mcp.yandex.tracker.domain

import com.fasterxml.jackson.databind.JsonNode

/**
 * Результат постраничного запроса к API Tracker.
 *
 * Tracker возвращает данные страницы в теле ответа, а сведения о пагинации — в заголовках
 * `X-Total-Count` и `X-Total-Pages`. Эта модель объединяет тело и метаданные пагинации,
 * чтобы инструменты могли вернуть агенту и сами объекты, и навигацию по страницам.
 *
 * @property items тело ответа со списком объектов (как правило, JSON-массив)
 * @property totalCount общее число объектов, удовлетворяющих запросу, если API его сообщил
 * @property totalPages общее число страниц, если API его сообщил
 * @property nextPageId курсор следующей страницы из ссылки `rel="next"`, если API его сообщил
 * @property nextPageUrl полная ссылка следующей страницы из заголовка `Link`
 * @property scrollId идентификатор прокрутки для больших результатов поиска
 *
 * @author Sorface Developer
 */
data class PagedResult(
    val items: JsonNode,
    val totalCount: Long?,
    val totalPages: Long?,
    val nextPageId: String? = null,
    val nextPageUrl: String? = null,
    val scrollId: String? = null,
)
