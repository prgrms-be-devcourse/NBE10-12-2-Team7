package com.dongnemarket.mobile.ui.chat

import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatRoomHeader

/**
 * 채팅방 화면의 상태.
 *
 * 목록 화면과 달리 `sealed interface` 가 아니라 **필드를 가진 하나의 data class** 인 이유:
 * 채팅방은 "헤더는 실패했지만 메시지는 보이고, 전송 중이며, 방금 에러가 하나 났다" 같은
 * **조합이 정상인 화면**이다. 로딩/성공/에러 중 하나만 고르게 만들면
 * 헤더 조회 실패 때문에 대화를 못 보는 사고가 난다(그건 절대 안 된다).
 *
 * @param header 상단 상품 카드 + 상대 닉네임. **null 이어도 대화는 그린다** —
 *   방 단건 상세 API 가 없어서(계약 §7-16) 헤더는 우회 조회이고, 실패 확률이 대화보다 높다.
 *   null 이면 헤더 줄을 접고 제목만 "채팅" 으로 둔다.
 * @param messages **오래된 것 → 최신 순**(시간 오름차순). Data 계층이 서버의 `id DESC` 를
 *   이미 뒤집어 주므로 화면에서 다시 `reversed()` 하면 대화가 거꾸로 보인다.
 *   즉 리스트의 **마지막 원소가 가장 최근 메시지**다.
 * @param myMemberId 내 회원 PK. 말풍선 좌/우 판정의 유일한 기준이다
 *   (`message.senderId == myMemberId` → 내 메시지). 로그인 응답에는 memberId 가 없어서
 *   `GET /api/members/me` 로만 얻을 수 있고, **아직 null 인 동안은 좌우를 확정할 수 없으므로
 *   메시지를 그리지 않는다**(틀린 쪽에 그렸다가 튀는 것보다 낫다).
 * @param input 입력창의 현재 글자. 전송 실패 시에도 지우지 않는다(사용자가 다시 쓰지 않게).
 * @param isSending 전송 왕복 중. 같은 문장이 두 번 가지 않게 버튼·입력창을 잠그는 데 쓴다.
 * @param isLoading 첫 진입 로딩. **내 memberId 와 첫 메시지 페이지가 둘 다 도착하면** false 가 된다.
 * @param errorMessage 한 번 보여 주고 사라지는 안내(스낵바). 반드시 `AppError.userMessage` 를 담고,
 *   화면이 표시한 뒤 `onErrorShown()` 으로 지운다. **에러가 대화를 덮지 않는다**는 원칙 때문에
 *   전용 에러 화면 상태를 따로 두지 않았다.
 * @param isPartnerWithdrawn 상대가 탈퇴해서 **전송만** 막힌 상태(읽기는 계속 된다).
 *   판정 근거는 닉네임 문자열("탈퇴한 사용자")이 아니라 전송 실패 시 오는
 *   `CHAT_PARTNER_WITHDRAWN`(400) 에러 코드다(계약 §4-4).
 */
data class ChatRoomUiState(
    val header: ChatRoomHeader? = null,
    val messages: List<ChatMessage> = emptyList(),
    val myMemberId: Long? = null,
    val input: String = "",
    val isSending: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isPartnerWithdrawn: Boolean = false,
) {
    /**
     * 전송 버튼을 누를 수 있는지. 서버 검증(`@NotBlank`)에 걸리기 전에 클라이언트가 먼저 막는다 —
     * 400 응답은 "어느 필드가 틀렸는지" 알려주지 않으므로(계약 §7-21) 미리 막는 편이 낫다.
     */
    val canSend: Boolean
        get() = input.isNotBlank() && !isSending && !isPartnerWithdrawn
}
