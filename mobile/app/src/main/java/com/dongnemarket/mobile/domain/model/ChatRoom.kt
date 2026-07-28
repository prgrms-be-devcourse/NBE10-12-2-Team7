package com.dongnemarket.mobile.domain.model

import java.math.BigDecimal

/**
 * 채팅 목록 화면의 한 줄. `GET /api/chat-rooms` 응답 원소에 대응한다.
 *
 * 서버 응답은 `{roomId, product, opponent, createdAt, lastMessage, unreadCount}` 처럼
 * 중첩 객체지만, 목록 한 줄을 그리는 데 필요한 값은 몇 개뿐이라 **평평하게 펼쳐** 담는다.
 * 화면이 `room.product.thumbnailUrl` 같은 점 사슬을 타지 않아도 되게 하는 것이 목적이다.
 *
 * 서버가 주는 순서는 **마지막 메시지 시각 DESC → roomId DESC** 이고,
 * 메시지가 없는 방은 방 생성 시각을 기준으로 낀다(갓 만든 빈 방이 맨 위).
 * 클라이언트에서 재정렬하지 말고 받은 순서를 그대로 신뢰하라.
 *
 * @param opponentId 상대방 memberId. 서버 응답 키가 `opponent`(요청자 기준 상대방)라서
 *   내가 구매자면 판매자, 내가 판매자면 구매자가 들어온다.
 * @param opponentNickname 탈퇴한 회원이면 `"탈퇴한 사용자"` 고정 문구가 온다.
 *   단 **입력창 비활성 판정을 이 문자열로 하지 마라** — 전송 시 오는
 *   `CHAT_PARTNER_WITHDRAWN`(400) 에러 코드로 판단해야 한다.
 * @param productPrice `BigDecimal` 인 이유: 서버가 GET 에서는 `800000.00`,
 *   POST 응답에서는 `800000` 으로 **스케일이 다른 숫자**를 준다. Int/Long 파싱은 깨진다.
 * @param productThumbnailUrl 이미 절대 URL 로 변환된 값(Data 계층이 baseUrl 을 붙였다).
 *   데모 데이터에는 이미지가 없어 대부분 null 이므로 **플레이스홀더가 필수**다.
 * @param createdAt **방이 만들어진 시각**(마지막 메시지 시각이 아니다). 오프셋 없는 원문 문자열.
 * @param lastMessage 아직 아무 말도 없는 방이면 null.
 * @param unreadCount 내가 안 읽은 **상대가 보낸** 메시지 수(내 메시지는 제외). 배지에 쓴다.
 *   방에 들어가서 `markAsRead(roomId)` 를 부르면 다음 폴링에서 0으로 내려온다.
 */
data class ChatRoom(
    val roomId: Long,
    val opponentId: Long,
    val opponentNickname: String,
    val productId: Long,
    val productTitle: String,
    val productPrice: BigDecimal,
    val productTradeStatus: TradeStatus,
    val productThumbnailUrl: String?,
    val createdAt: String,
    val lastMessage: ChatMessage?,
    val unreadCount: Long,
) {
    /**
     * 목록 한 줄에 찍을 시각의 원문 문자열.
     * 서버 정렬 기준과 같은 규칙(`lastMessage?.createdAt ?: createdAt`)이라
     * 빈 방도 시간 칸이 비지 않는다.
     */
    val displayTimeRaw: String
        get() = lastMessage?.createdAt ?: createdAt

    /** 배지를 그릴지 여부. `unreadCount` 가 Long 이라 비교 실수를 막기 위해 둔다. */
    val hasUnread: Boolean
        get() = unreadCount > 0L
}
