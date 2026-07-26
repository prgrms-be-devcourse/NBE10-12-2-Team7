package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.repository.AuthRepositoryImpl
import com.dongnemarket.mobile.data.repository.MemberRepositoryImpl
import com.dongnemarket.mobile.domain.repository.AuthRepository
import com.dongnemarket.mobile.domain.repository.MemberRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * "이 인터페이스를 요청하면 이 구현체를 주겠다"를 알려 주는 모듈.
 *
 * `@Binds` 는 `@Provides` 와 목적이 같지만 **몸통이 없다.**
 * 구현체가 이미 `@Inject constructor` 를 갖고 있어 Dagger 가 스스로 만들 수 있으므로,
 * "인터페이스 ← 구현체" 연결만 선언하면 되고 그래서 `abstract` 로 쓴다(코드가 생성되지 않아 더 가볍다).
 *
 * 덕분에 ViewModel 은 `AuthRepository` 만 알면 되고, 테스트에서는 이 모듈만 교체해
 * 가짜 구현으로 바꿔 끼울 수 있다.
 *
 * ⚠️ `@Binds`(abstract class) 와 `@Provides`(object) 를 한 클래스에 섞으면 컴파일 에러다
 * → ApiService 제공은 [AuthApiModule] 에 있다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMemberRepository(impl: MemberRepositoryImpl): MemberRepository
}
