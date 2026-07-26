package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.remote.AuthApiService
import com.dongnemarket.mobile.data.remote.MemberApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 인증·회원 도메인의 ApiService 구현체를 앱 전역 싱글톤으로 등록한다.
 *
 * `retrofit.create(...)` 가 인터페이스의 애노테이션을 읽어 실제 HTTP 호출 코드를 만들어 준다.
 * `Retrofit` 자체는 [NetworkModule] 이 이미 제공하므로 파라미터로 받기만 하면 Hilt 가 주입해 준다.
 *
 * ⚠️ **`@Provides`(object) 와 `@Binds`(abstract) 를 한 파일/한 클래스에 섞으면 컴파일이 안 된다.**
 * 그래서 인터페이스 구현 바인딩은 [AuthRepositoryModule] 로 분리했다.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthApiModule {

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideMemberApiService(retrofit: Retrofit): MemberApiService =
        retrofit.create(MemberApiService::class.java)
}
