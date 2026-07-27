package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.BuildConfig
import com.dongnemarket.mobile.data.remote.AuthInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 네트워크 배관(Json·OkHttp·Retrofit)을 조립해 앱 전역 싱글톤으로 등록하는 Hilt 모듈.
 *
 * Spring 의 `@Configuration` + `@Bean` 과 정확히 같은 자리다.
 *  - `@Module`             ↔ `@Configuration`
 *  - `@Provides`           ↔ `@Bean`
 *  - `@InstallIn(SingletonComponent::class)` ↔ 빈의 스코프를 "앱 전체(싱글톤)"로 지정
 *
 * 함수의 파라미터는 Hilt 가 알아서 채워 준다(생성자 주입과 동일한 원리).
 * 예: providesOkHttpClient(authInterceptor) → AuthInterceptor 는 @Inject 생성자를 갖고 있으므로 자동 생성·주입.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * JSON ↔ Kotlin 객체 변환기.
     *  - ignoreUnknownKeys: 백엔드가 필드를 추가해도 앱이 죽지 않게 한다(가장 흔한 크래시 원인).
     *  - explicitNulls=false: 값이 null 인 필드를 요청 본문에서 아예 빼서 보낸다.
     *  - coerceInputValues: null 이 온 자리에 기본값을 쓴다.
     *  - encodeDefaults=true: **기본값과 같은 값도 반드시 요청 본문에 싣는다.**
     *    kotlinx.serialization 의 기본값(false)이면 `LoginRequestDto.autoLogin = true` 처럼
     *    선언 기본값과 동일한 값이 통째로 생략된다. 서버의 `boolean autoLogin` 은 키가 없으면
     *    false 로 읽으므로, refreshToken 이 Max-Age 없는 세션 쿠키로 내려와 자동 로그인이 끊긴다.
     *    (explicitNulls=false 가 우선하므로 null 필드는 계속 생략된다.)
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    /**
     * 실제로 HTTP를 주고받는 클라이언트.
     * 인터셉터는 등록 순서대로 체인을 이루며, 요청은 위→아래로 통과한다.
     * (Auth 먼저 붙여야 로깅에 Authorization 헤더가 찍힌다.)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // 릴리스 빌드에서 요청/응답 본문을 로그에 남기면 토큰·개인정보가 유출된다.
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    /**
     * 인터페이스에 붙인 애노테이션(@GET/@POST)을 읽어 실제 호출 코드를 만들어 주는 본체.
     * baseUrl 은 buildType 에 따라 갈린다(app/build.gradle.kts):
     *  - debug   → http://10.0.2.2:8080/  (에뮬레이터가 보는 PC의 localhost)
     *  - release → https://marketon.inyeon.io/  (온프렘 배포)
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
