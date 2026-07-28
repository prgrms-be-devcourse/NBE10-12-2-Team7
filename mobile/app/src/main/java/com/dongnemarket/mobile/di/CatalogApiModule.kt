package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.remote.CategoryApiService
import com.dongnemarket.mobile.data.remote.RegionApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 카탈로그(카테고리·지역) API 창구를 앱 전역 싱글톤으로 등록하는 Hilt 모듈.
 *
 * `Retrofit` 자체는 [NetworkModule] 이 이미 제공하므로 여기서 다시 만들지 않는다
 * (같은 타입을 두 모듈이 제공하면 중복 바인딩 컴파일 에러).
 * 이 모듈은 "Retrofit 을 받아 인터페이스 구현체를 찍어내는" 일만 한다.
 *
 * 구현체를 `@Binds` 로 연결하는 Repository 쪽은
 * `abstract class` 가 필요해서 [CatalogRepositoryModule] 로 파일을 분리했다
 * (`@Provides` 를 담는 `object` 와 `@Binds` 를 담는 `abstract class` 는 한 모듈에 섞을 수 없다).
 */
@Module
@InstallIn(SingletonComponent::class)
object CatalogApiModule {

    @Provides
    @Singleton
    fun provideCategoryApiService(retrofit: Retrofit): CategoryApiService =
        retrofit.create(CategoryApiService::class.java)

    @Provides
    @Singleton
    fun provideRegionApiService(retrofit: Retrofit): RegionApiService =
        retrofit.create(RegionApiService::class.java)
}
