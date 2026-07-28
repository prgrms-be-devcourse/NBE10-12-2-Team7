package com.dongnemarket.mobile.domain.model

/**
 * 로그인한 "나"를 표현하는 도메인 모델. `GET /api/members/me` 응답을 옮겨 담은 것이다.
 *
 * 이 모델이 앱에서 갖는 두 가지 특별한 역할:
 *  1. **세션 확인 수단.** 홈·상품목록 같은 무인증(permitAll) 경로는 토큰이 만료돼도 401이 아니라
 *     200 + 익명 취급으로 응답한다. 그래서 "지금 로그인 상태가 살아 있는가"는
 *     이 API 호출이 성공하는지로만 판정할 수 있다.
 *  2. **`memberId` 의 유일한 출처.** 로그인 응답에는 accessToken 한 필드밖에 없어서
 *     내 memberId 를 알 방법이 이 API뿐이다. 채팅에서 "내가 보낸 메시지"(좌/우 말풍선)를
 *     가르는 기준도 `ChatMessage.senderId == member.memberId` 비교다.
 */
data class Member(
    val memberId: Long,
    val email: String,
    val nickname: String,
    val role: MemberRole,
    val status: MemberStatus,
    /**
     * 가입 시각의 **원문 문자열**. 예: `"2026-07-26T13:45:30.123456"`
     *
     * 왜 LocalDateTime 이 아닌가: 서버가 오프셋(Z)도 없고 소수부 자릿수도 가변인
     * ISO local 문자열을 준다. Data 계층에서 파싱하면 형식이 하나만 어긋나도 화면 전체가
     * 실패하므로, 원문을 그대로 들고 와서 **표시할 때만** 파싱하고 실패 시 원문을 노출한다.
     * (`DateTimeFormatter.ISO_LOCAL_DATE_TIME`, 타임존은 KST 가정)
     */
    val createdAt: String,
)

/**
 * 회원 권한. 서버는 `"ROLE_USER"` / `"ROLE_ADMIN"` 처럼 **`ROLE_` 접두가 붙은 문자열**로 주는데,
 * 도메인에서는 접두를 떼고 쓴다(매핑은 MemberMapper 담당).
 *
 * [UNKNOWN] 이 있는 이유: 백엔드에 권한이 추가되면 앱이 파싱 단계에서 죽는다.
 * 모르는 값은 UNKNOWN 으로 강등시켜 "일반 사용자보다 특별하지 않게" 다룬다.
 */
enum class MemberRole { USER, ADMIN, UNKNOWN }

/**
 * 회원 상태. 실제로 `GET /api/members/me` 가 성공하면 [ACTIVE] 만 관측된다
 * (정지·탈퇴 계정은 API가 403/400으로 먼저 막는다). 그래도 값을 보존해 두는 편이
 * 나중에 서버 정책이 바뀌었을 때 원인을 추적하기 쉽다.
 */
enum class MemberStatus { ACTIVE, SUSPENDED, DELETED, UNKNOWN }
