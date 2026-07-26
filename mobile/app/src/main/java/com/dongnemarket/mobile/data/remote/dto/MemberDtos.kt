package com.dongnemarket.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `GET /api/members/me` 응답의 `data`. 백엔드 원본: `member/dto/MemberResponse`.
 */
@Serializable
data class MemberResponseDto(
    /** JWT 의 `sub` 와 같은 값. 채팅에서 내 메시지를 판별하는 기준이 된다. */
    val memberId: Long,
    val email: String,
    val nickname: String,
    /**
     * `"ROLE_USER"` / `"ROLE_ADMIN"`.
     *
     * 왜 Kotlin enum 이 아니라 String 인가: 백엔드에 권한이 하나 추가되면
     * enum 파싱이 실패해 내 정보 조회 전체가 깨진다(= 세션 확인이 죽는다).
     * 문자열로 받아 두고 매퍼에서 `MemberRole.UNKNOWN` 으로 강등시키는 쪽이 안전하다.
     */
    val role: String? = null,
    /** `"ACTIVE"` / `"SUSPENDED"` / `"DELETED"`. role 과 같은 이유로 String 이다. */
    val status: String? = null,
    /**
     * 오프셋(Z) 없고 소수부 자릿수가 가변인 ISO local 문자열. `Instant`/`OffsetDateTime` 파싱은 반드시 실패한다.
     * 기본값 `""` 를 둔 이유는 서버가 이 키를 빼더라도 파싱 자체가 깨지지 않게 하기 위한 방어다.
     */
    val createdAt: String = "",
)

/**
 * `GET/PUT /api/members/me/locations` 응답 원소. 백엔드 원본: `member/dto/MemberLocationResponse`.
 *
 * ⚠️ **regionId 가 없다.** `region` 문자열이 유일한 식별자다.
 * `active` 는 Java 의 `isActive()` getter 라서 JSON 키가 `"active"` 다(`isActive` 아님).
 */
@Serializable
data class MemberLocationResponseDto(
    val region: String,
    val sortOrder: Int = 0,
    val active: Boolean = false,
)

/**
 * `PUT /api/members/me/locations` 요청 본문. 백엔드 원본: `member/dto/MemberLocationUpdateRequest`.
 *
 * `regions` 는 `GET /api/regions` 가 준 `name` **원문**이어야 한다(서버가 문자열 완전 일치로 검증).
 * 1~2개만 허용되고, 리스트 0번이 대표 동네가 된다.
 */
@Serializable
data class MemberLocationUpdateRequestDto(
    val regions: List<String>,
)
