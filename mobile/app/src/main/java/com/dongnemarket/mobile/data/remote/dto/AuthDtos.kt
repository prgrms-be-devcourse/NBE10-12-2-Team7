package com.dongnemarket.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `POST /api/auth/login` 요청 본문.
 *
 * 백엔드 원본: `auth/dto/LoginRequest`. 필드명이 JSON 키와 같으므로 `@SerialName` 이 필요 없다.
 * (서버에 `@JsonNaming` 설정이 없어 camelCase 그대로 주고받는다.)
 */
@Serializable
data class LoginRequestDto(
    /** 로그인 식별자는 loginId 가 아니라 **이메일**이다. */
    val email: String,
    val password: String,
    /**
     * **모바일은 항상 true 로 고정한다.**
     *
     * 이 값이 false 면 서버가 refreshToken 쿠키를 `Max-Age` 없이 내려서
     * 세션 쿠키(= 프로세스 종료 시 소멸)가 된다. 그러면 나중에 재발급(reissue)을 붙여도
     * 앱을 재시작한 순간부터 자동 로그인이 불가능해진다. 기본값을 true 로 박아 실수를 막는다.
     */
    val autoLogin: Boolean = true,
)

/**
 * 로그인·재발급 응답의 `data`. 백엔드 원본: `auth/dto/AccessTokenResponse`.
 *
 * ⚠️ **필드가 이것 하나뿐이다.** tokenType·expiresIn·memberId·nickname·role 이 전부 없다.
 * 그래서 로그인 직후 내 정보가 필요하면 `GET /api/members/me` 를 반드시 이어서 호출해야 한다.
 * refreshToken 은 body 에 절대 오지 않는다(HttpOnly 쿠키 전용).
 */
@Serializable
data class AccessTokenDto(
    val accessToken: String,
)
