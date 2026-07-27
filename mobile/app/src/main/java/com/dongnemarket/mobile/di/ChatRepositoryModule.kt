package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.repository.ChatRepositoryImpl
import com.dongnemarket.mobile.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * "[ChatRepository] 를 달라고 하면 [ChatRepositoryImpl] 을 주라"는 연결 선언.
 *
 * ViewModel 은 인터페이스([ChatRepository])만 주입받는데, Hilt 는 인터페이스로
 * 인스턴스를 만들 수 없으니 어느 구현체를 쓸지 알려 줘야 한다. 그 역할이 `@Binds` 다.
 *
 * `@Provides` 대신 `@Binds` 를 쓰는 이유: `ChatRepositoryImpl` 은 `@Inject` 생성자를 갖고 있어
 * Hilt 가 스스로 만들 수 있다. 그러니 "만드는 법"이 아니라 "타입 연결"만 알려 주면 되고,
 * `@Binds` 는 몸통 없는 추상 함수라 코드도 생성물도 더 작다.
 * 그래서 이 클래스는 `abstract` 다(인스턴스를 만들 필요가 없다).
 *
 * ⚠ 같은 타입을 두 모듈이 제공하면 중복 바인딩 컴파일 에러가 난다
 * → 이 모듈은 채팅 소유 타입만 다룬다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
