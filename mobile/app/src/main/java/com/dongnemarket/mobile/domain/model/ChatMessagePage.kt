package com.dongnemarket.mobile.domain.model

/**
 * 메시지 목록 한 페이지(커서 페이징).
 *
 * ## ⚠ [messages] 의 정렬은 서버와 반대다
 * 서버는 `id DESC`(**최신이 0번**)로 주지만, 이 모델은 Data 계층에서 뒤집어
 * **오래된 것 → 최신 순(시간 오름차순)** 으로 담는다.
 * 채팅 UI는 위에서 아래로 시간이 흐르므로 그대로 `LazyColumn` 에 넣으면 되고,
 * 화면 쪽에서 다시 `reversed()` 하면 대화가 거꾸로 보인다.
 *
 * ## 왜 상품 페이지와 같은 제네릭 클래스로 안 묶었나
 * 서버의 아이템 필드명이 다르다 — 상품은 `items`, 채팅은 `messages`.
 * 공용 제네릭으로 묶으면 `@SerialName` 별칭 곡예가 필요하고, 한쪽이 조용히 null 이 된다.
 *
 * @param nextCursor 이 페이지에서 **가장 오래된** messageId. 더 과거를 불러올 때
 *   `getMessages(roomId, cursor = nextCursor)` 로 넘긴다(서버는 `id < cursor`).
 *   마지막 페이지면 null.
 * @param hasNext 더 **과거** 메시지가 남아 있는지. "더 최신"이 아니다 —
 *   커서는 과거 방향 전용이고 최신 수신은 폴링(커서 없이 첫 페이지 재조회)으로 얻는다.
 */
data class ChatMessagePage(
    val messages: List<ChatMessage>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)
