package com.dongnemarket.mobile.domain.model

/**
 * 채팅 메시지 한 건. 말풍선 하나에 대응한다.
 *
 * ## 왜 `isMine` 이 없는가
 * "내가 보낸 메시지인가"는 `senderId == 내 memberId` 로 정해지는데,
 * **내 memberId 를 아는 것은 Data 계층이 아니라 ViewModel 의 책임**이다
 * (`MemberRepository.getMe()` 로 얻는다). Data 계층이 미리 계산해 넣으면
 * 로그인 사용자가 바뀔 때 캐시된 값이 거짓말을 하게 된다.
 * → 화면에서 `message.senderId == myMemberId` 로 좌/우를 판단하라.
 *
 * ## 왜 `isRead` 가 없는가
 * 서버는 읽음 지점을 DB 컬럼으로만 관리하고 **어떤 응답에도 노출하지 않는다**(계약 §8-5).
 * '상대가 읽음' 표시는 이 계약으로 구현 불가능하므로 필드를 만들지 않았다.
 * 안읽음 정보는 [ChatRoom.unreadCount] 배지뿐이다.
 *
 * @param messageId 삽입순 단조증가 PK. **정렬키 겸 페이징 커서**이고,
 *   폴링이 같은 메시지를 다시 가져올 때 중복 제거 기준으로도 쓴다.
 * @param senderId 보낸 사람의 memberId. 메시지에 닉네임·프로필은 오지 않는다.
 * @param content 본문(최대 1000자). 이미지·메시지 타입 개념 없음.
 * @param createdAt 서버가 준 **원문 문자열**. 오프셋(Z)이 없고 소수부 자릿수가 가변이라
 *   (`2026-07-26T13:45:30` / `...30.123456`) Instant·OffsetDateTime 파싱은 반드시 실패한다.
 *   그래서 파싱하지 않고 원문을 들고 다니며, 표시 시점에
 *   `DateTimeFormatter.ISO_LOCAL_DATE_TIME` 으로 파싱(KST 가정)하고 실패하면 원문을 그대로 보여준다.
 */
data class ChatMessage(
    val messageId: Long,
    val senderId: Long,
    val content: String,
    val createdAt: String,
)
