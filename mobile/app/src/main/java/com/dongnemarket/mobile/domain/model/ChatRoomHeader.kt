package com.dongnemarket.mobile.domain.model

import java.math.BigDecimal

/**
 * 채팅방 화면 상단에 붙는 요약(상품 카드 + 상대 닉네임).
 *
 * ## 이 모델이 존재하는 이유 — 서버 응답 키 비대칭 흡수
 * 채팅방 헤더에 필요한 정보를 주는 응답이 **두 종류인데 키 이름이 다르다**.
 *
 * | 같은 의미 | 방 생성 `POST /api/chat-rooms` | 방 목록 `GET /api/chat-rooms` |
 * |---|---|---|
 * | 상대방 | `seller` (ChatMemberSummary) | `opponent` (ChatMemberSummary) |
 * | 상품 | `product` (ChatProductDetail — description·region **있음**) | `product` (ChatProductSummary — **없음**) |
 *
 * 타입이 사실상 같은데 키가 달라서 DTO 한 개로 매핑하면 **예외 없이 조용히 null** 이 된다.
 * 그래서 두 응답의 **교집합**만 뽑은 이 도메인 모델을 두고, 어느 쪽에서 왔든
 * 화면은 이 한 가지 타입만 알면 되게 했다.
 * (`description`·`region` 은 헤더에서 쓰지 않으므로 일부러 뺐다.)
 *
 * ## 왜 서버에서 한 번에 못 받나
 * `GET /api/chat-rooms/{roomId}` — 즉 **방 단건 상세 API 가 존재하지 않는다**(계약 §7-16).
 * 그래서 진입 경로에 따라 채우는 방법이 다르다:
 *  1. 목록 → 방: 이미 갖고 있는 [ChatRoom] 을 [from] 으로 변환(추가 호출 0회)
 *  2. 상품 상세 → 방: `createRoom()` 으로 roomId 만 받고 `getRoomHeader(roomId)` 호출
 *  3. 프로세스 재시작·복원(roomId 만 남은 경우): `getRoomHeader(roomId)` 호출
 *
 * @param productPrice `BigDecimal` — 스케일이 엔드포인트마다 달라 Int/Long 파싱이 깨진다.
 * @param productThumbnailUrl baseUrl 이 이미 붙은 절대 URL. 없으면 null → 플레이스홀더.
 * @param opponentNickname 탈퇴 회원이면 `"탈퇴한 사용자"`. 이 문자열로 입력창을 잠그지 말고
 *   전송 실패 시 오는 `CHAT_PARTNER_WITHDRAWN` 코드로 판단하라.
 */
data class ChatRoomHeader(
    val roomId: Long,
    val opponentId: Long,
    val opponentNickname: String,
    val productId: Long,
    val productTitle: String,
    val productPrice: BigDecimal,
    val productTradeStatus: TradeStatus,
    val productThumbnailUrl: String?,
) {
    companion object {
        /**
         * 채팅 목록에서 이미 받아 둔 방 정보로 헤더를 만든다(네트워크 호출 없음).
         * 목록 → 방 이동 시 헤더가 한 프레임도 비지 않게 하는 용도다.
         */
        fun from(room: ChatRoom): ChatRoomHeader = ChatRoomHeader(
            roomId = room.roomId,
            opponentId = room.opponentId,
            opponentNickname = room.opponentNickname,
            productId = room.productId,
            productTitle = room.productTitle,
            productPrice = room.productPrice,
            productTradeStatus = room.productTradeStatus,
            productThumbnailUrl = room.productThumbnailUrl,
        )
    }
}
