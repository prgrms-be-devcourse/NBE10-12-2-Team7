package com.dongnemarket.mobile.domain.model

/**
 * 내가 설정한 "내 동네" 한 건. `GET/PUT /api/members/me/locations` 의 원소다.
 *
 * 알아야 할 규칙 세 가지:
 *  - **식별자가 없다.** 서버 응답에 regionId 가 없고 [region] 문자열이 곧 식별자다.
 *    (지역 목록 API는 `regionId` 를 주지만 내 동네 API는 이름만 주고받는다 — 이 비대칭이 계약이다.)
 *  - [region] 은 `"서울 강남구"` 처럼 **"시도 시군구" 한 문자열**이다. 서버가 이 문자열을
 *    완전 일치로 검증하므로 사용자 자유입력을 보내면 400이 난다. 반드시 지역 목록에서 받은 원문을 쓴다.
 *  - [active] 는 클라이언트가 정하지 못한다. `PUT` 으로 보낸 **리스트의 0번 원소**를
 *    서버가 `sortOrder = 0, active = true` 로 만든다. 대표 동네를 바꾸려면 순서를 바꿔 전체를 다시 보낸다.
 */
data class MemberLocation(
    /** 지역 이름 원문. 예: `"서울 강남구"`. 이 값이 이 모델의 식별자다. */
    val region: String,
    /** 0-base 우선순위. 0번이 대표 동네다. */
    val sortOrder: Int,
    /** 대표 동네 여부(= `sortOrder == 0`). 홈 헤더에 찍는 동네가 이것이다. */
    val active: Boolean,
)
