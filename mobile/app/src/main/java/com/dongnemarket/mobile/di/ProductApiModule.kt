package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.remote.ProductApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 상품 API 인터페이스의 Retrofit 구현체를 앱 전역 싱글톤으로 등록한다.
 *
 * `Retrofit` 자체는 [NetworkModule] 이 이미 제공하므로 여기서 다시 만들지 않는다
 * (같은 타입을 두 모듈이 제공하면 Dagger 가 중복 바인딩 에러를 낸다).
 *
 * `@Provides`(구현을 코드로 만들어 주는 경우)와 `@Binds`(인터페이스↔구현 연결)를 한 파일에
 * 섞으면 컴파일 에러라서, Repository 바인딩은 `ProductRepositoryModule` 로 분리했다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProductApiModule {

    @Provides
    @Singleton
    fun provideProductApiService(retrofit: Retrofit): ProductApiService =
        retrofit.create(ProductApiService::class.java)
}
