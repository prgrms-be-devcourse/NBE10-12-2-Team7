package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.remote.ChatApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * [ChatApiService] 구현체를 앱 전역 싱글톤으로 등록한다.
 *
 * `ChatApiService` 는 인터페이스라 `new` 할 수 없고, Retrofit 이 `create()` 로
 * 구현체를 만들어 준다. 그 한 줄을 Hilt 에 알려 주는 것이 이 모듈의 전부다.
 * Spring 의 `@Configuration` + `@Bean` 자리와 정확히 같다.
 *
 * `Retrofit` 파라미터는 NetworkModule 이 이미 제공하므로 Hilt 가 알아서 채운다.
 *
 * ## 왜 파일을 API/Repository 두 개로 쪼갰나
 * `@Provides`(구체 구현을 직접 만들어 준다)는 `object` 에,
 * `@Binds`(인터페이스 ↔ 구현체를 연결만 한다)는 `abstract class` 에 있어야 한다.
 * 한 파일·한 클래스에 섞으면 Dagger 컴파일 에러가 난다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ChatApiModule {

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService =
        retrofit.create(ChatApiService::class.java)
}
