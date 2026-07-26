package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.remote.FavoriteApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * `FavoriteApiService` 구현체를 만들어 앱 전역 싱글톤으로 등록한다.
 *
 * `retrofit.create(...)` 가 인터페이스를 읽어 실제 HTTP 호출 코드를 동적으로 생성한다
 * (Spring Data JPA 가 리포지토리 인터페이스 구현체를 만들어 주는 것과 같은 방식).
 * [NetworkModule] 이 제공하는 `Retrofit` 싱글톤을 파라미터로 받아 쓴다.
 *
 * 왜 Repository 바인딩과 파일을 나눴는가: Dagger 는 `@Provides`(object 모듈)와
 * `@Binds`(abstract 모듈)를 한 모듈에 섞으면 컴파일 에러를 낸다 → 도메인마다 모듈 2개가 정석이다.
 */
@Module
@InstallIn(SingletonComponent::class)
object FavoriteApiModule {

    @Provides
    @Singleton
    fun provideFavoriteApiService(retrofit: Retrofit): FavoriteApiService =
        retrofit.create(FavoriteApiService::class.java)
}
