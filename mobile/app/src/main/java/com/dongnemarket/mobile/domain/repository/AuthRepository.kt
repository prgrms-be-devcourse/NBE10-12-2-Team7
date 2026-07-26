package com.dongnemarket.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 로그인·로그아웃과 "지금 로그인 상태인가"를 담당한다.
 *
 * 왜 인터페이스가 Domain 에 있고 구현은 Data 에 있는가(의존성 역전):
 * ViewModel 은 이 인터페이스만 알면 되고, 토큰을 DataStore 에 저장하는지 Retrofit 을 쓰는지는 모른다.
 * Spring 의 `Repository` 인터페이스 ↔ 구현 분리와 같은 자리다.
 *
 * 규약: **예외를 던지지 않는다.** 모든 실패는 `Result.failure(AppError)` 로 돌아온다.
 *
 * ### 토큰은 이 계층 밖으로 나가지 않는다
 * `login()` 이 accessToken 을 반환하지 않고 `Result<Unit>` 을 주는 이유다.
 * 저장은 구현체가 하고, ViewModel 은 성공/실패만 안다. 헤더 부착도 `AuthInterceptor` 가 자동으로 한다.
 *
 * ### accessToken 재발급(reissue)은 Phase 1 범위가 아니다 — 의도적 누락
 * `POST /api/auth/reissue` 는 body 도 Authorization 헤더도 읽지 않고
 * **`refreshToken` HttpOnly 쿠키만** 읽는다. 그런데 OkHttp 에는 기본 CookieJar 가 없어서
 * 지금 상태로 부르면 100% `401 INVALID_REFRESH_TOKEN` 이 온다.
 * 제대로 하려면 영속 CookieJar(또는 `Set-Cookie` 수동 파싱·저장)를 붙여야 하고,
 * 재발급 성공 시 회전된 새 쿠키로 갱신까지 해야 한다 → **Phase 3 과제로 분리했다.**
 * 그래서 Phase 1 의 세션 수명은 accessToken 기본 수명 15분이고, 만료되면 재로그인이다.
 */
interface AuthRepository {

    /**
     * 저장된 accessToken 이 있는지의 흐름. 토큰이 저장/삭제되면 자동으로 새 값이 흘러온다
     * (스플래시·최초 진입 라우팅용).
     *
     * ⚠️ 이것은 "토큰 문자열이 존재한다"는 뜻일 뿐 **토큰이 살아 있다는 보장이 아니다.**
     * 실제 세션 유효성은 `MemberRepository.getMyProfile()` 성공 여부로 확인해야 한다.
     */
    val isLoggedIn: Flow<Boolean>

    /**
     * 이메일/비밀번호 로그인. 성공하면 **구현체가 accessToken 을 기기에 저장한 뒤** 성공을 반환한다.
     *
     * 실패는 `AppError` 로 번역돼 오므로 화면은 `AppError.userMessage` 만 띄우면 된다.
     * (미가입 이메일 404 / 비번 불일치 401 / 5회 실패 후 429 / 정지 403 / 탈퇴 400 — 모두 서버 문구 사용)
     */
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * 로그아웃. **항상 성공한다.**
     *
     * 서버는 저장된 refreshToken 만 지우고 accessToken 을 무효화하지 않는다.
     * 즉 로그아웃의 본질은 "기기에서 토큰을 지우는 것"이므로,
     * 서버 호출이 실패하더라도(예: accessToken 이 이미 만료돼 401) 로컬 토큰은 반드시 지우고 성공으로 끝낸다.
     * 그러지 않으면 사용자에게 "로그아웃이 안 된" 화면이 남는다.
     */
    suspend fun logout(): Result<Unit>
}
