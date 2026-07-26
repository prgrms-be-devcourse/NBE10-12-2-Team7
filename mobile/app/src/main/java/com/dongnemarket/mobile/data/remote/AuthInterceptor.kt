package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.local.TokenDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 나가는 모든 HTTP 요청에 `Authorization: Bearer <token>` 헤더를 붙인다.
 *
 * Spring 에 비유하면 서버의 `HandlerInterceptor`/필터와 같은 자리인데, 방향이 반대다.
 * 서버 필터는 들어오는 요청에서 토큰을 *꺼내 검증*하고,
 * 이 클라이언트 인터셉터는 나가는 요청에 토큰을 *끼워 넣는다*.
 *
 * 덕분에 각 ApiService 함수마다 `@Header("Authorization")` 를 반복하지 않아도 된다.
 *
 * runBlocking 을 쓰는 이유: OkHttp 인터셉터는 suspend 함수가 아닌 동기 API 이고,
 * 이미 OkHttp 의 백그라운드 스레드에서 실행되므로 여기서 잠깐 기다려도 메인 스레드를 막지 않는다.
 * 읽는 값이 로컬 파일 한 줄이라 비용도 작다.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // 로그인·회원가입처럼 토큰이 필요 없는(그리고 있으면 안 되는) 요청은 건드리지 않는다.
        if (original.url.encodedPath in NO_AUTH_PATHS) {
            return chain.proceed(original)
        }

        val token = runBlocking { tokenDataStore.accessToken.first() }
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }

    private companion object {
        val NO_AUTH_PATHS = setOf(
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/reissue",
        )
    }
}
