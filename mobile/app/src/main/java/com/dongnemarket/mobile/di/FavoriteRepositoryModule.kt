package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.repository.FavoriteRepositoryImpl
import com.dongnemarket.mobile.domain.repository.FavoriteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * "FavoriteRepository 를 주입해 달라"는 요청에 [FavoriteRepositoryImpl] 을 꽂아 주는 연결표.
 *
 * `@Binds` 는 `@Provides` 와 달리 몸통이 없다 — 새로 만드는 방법을 알려주는 게 아니라
 * "이 인터페이스 자리에 저 구현을 쓴다"는 매핑만 선언하기 때문이다. 그래서 `abstract` 여야 한다.
 * 덕분에 ViewModel 은 인터페이스만 알고, 구현을 바꿔도 ViewModel 코드는 그대로다.
 *
 * `@Singleton` 이 붙은 이유: 구현체가 찜 id 캐시(StateFlow)를 들고 있어서
 * 인스턴스가 두 개 이상 생기면 화면 간 하트 상태가 어긋난다.
 * (바인딩과 구현 클래스 양쪽 모두에 `@Singleton` 이 필요하다.)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FavoriteRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository
}
