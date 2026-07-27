package com.dongnemarket.mobile.data.mapper

import com.dongnemarket.mobile.data.remote.dto.ChatMessagePageResponse
import com.dongnemarket.mobile.data.remote.dto.ChatMessageResponse
import com.dongnemarket.mobile.data.remote.dto.ChatRoomListResponse
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatMessagePage
import com.dongnemarket.mobile.domain.model.ChatRoom

/*
 * DTO(서버가 준 JSON) → 도메인 모델(앱이 쓰기 편한 형태) 변환을 모아 둔 곳.
 *
 * 왜 한 파일에 모으나: 서버 응답의 기괴한 점들(키 비대칭, 역순 정렬, 상대경로 이미지)을
 * **여기서 전부 흡수**해서 그 밖의 코드가 정상적인 세계에 살게 하려는 것이다.
 * 이 파일 밖에서 같은 보정을 또 하면 이중 보정이 되어 버그가 된다.
 *
 * 확장 함수로 쓴 이유는 `dto.toDomain()` 처럼 읽히게 하려는 것뿐이고,
 * DTO 클래스 자체에 매핑 로직을 넣지 않는 것이 핵심이다(DTO 는 JSON 모양만 책임진다).
 *
 * 상대경로 이미지 → 절대 URL 변환은 상품 쪽과 규칙이 완전히 같아서 여기서 다시 만들지 않고
 * 같은 패키지의 `ImageUrlMapper.kt` 에 있는 `String?.toAbsoluteImageUrl()` 을 그대로 쓴다
 * (같은 패키지라 import 가 필요 없다). 같은 보정을 두 곳에 두면 규칙이 갈라진다.
 */

/** 메시지 한 건. `createdAt` 은 파싱하지 않고 원문을 넘긴다(표시 시점에 파싱). */
internal fun ChatMessageResponse.toDomain(): ChatMessage = ChatMessage(
    messageId = messageId,
    senderId = senderId,
    content = content,
    createdAt = createdAt,
)

/**
 * 메시지 페이지.
 *
 * ⚠ **여기서 `reversed()` 하는 것이 이 함수의 핵심**이다.
 * 서버는 `id DESC`(최신이 0번)로 주는데, 채팅 화면은 위에서 아래로 시간이 흐른다.
 * 뒤집지 않으면 대화가 거꾸로 보인다.
 *
 * 뒤집기를 UI 가 아니라 여기서 하는 이유: 폴링·과거더보기·낙관적추가 등
 * 리스트를 만지는 지점이 여러 곳이라, 각자 뒤집으면 한 군데만 빼먹어도 순서가 깨진다.
 * "Repository 를 벗어난 메시지 리스트는 언제나 시간 오름차순"이라는 불변식을 여기서 고정한다.
 *
 * `nextCursor`/`hasNext` 는 뒤집기와 무관하다 — 커서는 여전히 **과거 방향**을 가리킨다.
 */
internal fun ChatMessagePageResponse.toDomain(): ChatMessagePage = ChatMessagePage(
    messages = messages.map { it.toDomain() }.reversed(),
    nextCursor = nextCursor,
    hasNext = hasNext,
)

/**
 * 채팅 목록의 한 줄.
 *
 * 중첩된 `product`/`opponent` 를 평평하게 펼치고, 썸네일은 절대 URL 로 바꿔 둔다
 * → 화면은 점 사슬을 타지 않고 이미지 URL 보정도 신경 쓰지 않는다.
 */
internal fun ChatRoomListResponse.toDomain(): ChatRoom = ChatRoom(
    roomId = roomId,
    opponentId = opponent.memberId,
    opponentNickname = opponent.nickname,
    productId = product.productId,
    productTitle = product.title,
    productPrice = product.price,
    productTradeStatus = product.tradeStatus,
    productThumbnailUrl = product.thumbnailUrl.toAbsoluteImageUrl(),
    createdAt = createdAt,
    lastMessage = lastMessage?.toDomain(),
    unreadCount = unreadCount,
)

/*
 * 참고: `ChatRoomDetailResponse`(방 생성 응답) → [ChatRoomHeader] 매퍼는 **일부러 두지 않았다.**
 *
 * 방 생성 응답에도 헤더에 필요한 정보가 다 들어 있지만(상대방 키가 `opponent` 가 아니라
 * **`seller`** 라는 점만 다르다), Repository 의 `createRoom()` 은 `roomId` 만 반환하도록
 * 계약이 정해져 있어서 이 매퍼를 만들면 호출자가 없는 죽은 코드가 된다.
 * 방 생성 직후의 헤더는 `getRoomHeader(roomId)` 가 채운다(갓 만든 빈 방도 목록에 뜬다).
 *
 * `seller`/`opponent` 비대칭을 흡수하는 책임 자체는 [ChatRoomHeader] 라는 타입이 지고 있다.
 * 나중에 방 생성 직후의 추가 호출 1회를 없애고 싶어지면, `createRoom` 의 반환 타입을
 * `Result<ChatRoomHeader>` 로 바꾸고 그때 이 매퍼를 추가하는 것이 정석이다.
 */
