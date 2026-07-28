package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.remote.dto.AccessTokenDto
import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.LoginRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 인증 엔드포인트 선언부. Retrofit 이 이 인터페이스를 읽어 구현체를 만들어 준다
 * (Spring Data JPA 가 리포지토리 인터페이스를 구현해 주는 것과 같은 원리).
 *
 * 규칙 세 가지:
 *  - 경로에 **선행 `/` 를 쓰지 않는다.** `"/api/..."` 로 쓰면 baseUrl 의 path 가 잘려 나간다.
 *  - 반환 타입은 `Response<...>` 가 아니라 껍데기 그대로인 `ApiEnvelope<...>` 다.
 *    벗기고 예외를 번역하는 일은 Repository 에서 `apiCall { }` 이 한다.
 *  - `@Header("Authorization")` 을 쓰지 않는다. [AuthInterceptor] 가 자동으로 붙인다.
 */
interface AuthApiService {

    /**
     * 로그인. 성공 시 `data.accessToken` 과 함께 `Set-Cookie: refreshToken=...` 이 온다.
     * (쿠키를 실제로 보관하는 일은 Phase 3 — 지금은 accessToken 만 쓴다.)
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): ApiEnvelope<AccessTokenDto>

    /**
     * 로그아웃. 인증이 필요하다(permitAll 아님).
     *
     * ⚠️ 반환 타입이 `ApiEnvelope<Unit>` 인 이유: 이 응답에는 **`data` 키 자체가 없다**
     * (서버 `ApiResponse` 에 `@JsonInclude(NON_NULL)` 이 걸려 있어 null 인 data 는 키까지 사라진다).
     * 그래서 Repository 에서 `apiCall` 이 아니라 **`apiCallForUnit`** 으로 감싸야 한다
     * — `apiCall` 은 data 가 없으면 실패로 판정한다.
     */
    @POST("api/auth/logout")
    suspend fun logout(): ApiEnvelope<Unit>
}
