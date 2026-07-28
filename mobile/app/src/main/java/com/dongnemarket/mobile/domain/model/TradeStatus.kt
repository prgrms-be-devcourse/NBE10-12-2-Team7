package com.dongnemarket.mobile.domain.model

import kotlinx.serialization.Serializable

/**
 * 상품 거래 상태. 백엔드 `product.trade_status` enum 을 그대로 옮긴 것이다.
 *
 * 서버가 실제로 보내는 값은 **3개뿐**(`ON_SALE`/`RESERVED`/`COMPLETED`)이고,
 * [UNKNOWN] 은 서버에 존재하지 않는 **앱 전용 방어값**이다.
 *
 * 왜 UNKNOWN 이 필요한가: 백엔드가 나중에 상태를 하나 추가하면(예: `BLOCKED`)
 * 문자열→enum 변환이 예외를 던지고 목록 화면 전체가 죽는다. [from] 이 모르는 값을
 * UNKNOWN 으로 떨어뜨려 주므로, 그 카드만 배지가 사라질 뿐 앱은 계속 돈다.
 *
 * @Serializable 을 붙여 둔 이유: 채팅·찜 도메인의 DTO 가 이 enum 을 필드 타입으로
 * 직접 선언할 수 있게 하려는 것이다(상품 DTO 자신은 String 으로 받고 [from] 으로 변환한다).
 */
@Serializable
enum class TradeStatus {
    /** 판매중 */
    ON_SALE,

    /** 예약중 */
    RESERVED,

    /**
     * 거래완료.
     * 홈 목록·검색·카테고리 응답에는 절대 오지 않는다(서버가 필터로 제외).
     * 채팅방의 상품 정보에서만 관측된다.
     */
    COMPLETED,

    /** 서버가 준 값을 앱이 모를 때. 배지를 숨기는 신호로 쓴다. */
    UNKNOWN,
    ;

    companion object {
        /**
         * 서버 문자열 → enum. 모르는 값·null·공백은 전부 [UNKNOWN].
         * `valueOf()` 를 직접 쓰면 IllegalArgumentException 이 나므로 반드시 이걸 쓴다.
         */
        fun from(raw: String?): TradeStatus = when (raw) {
            "ON_SALE" -> ON_SALE
            "RESERVED" -> RESERVED
            "COMPLETED" -> COMPLETED
            else -> UNKNOWN
        }
    }
}
