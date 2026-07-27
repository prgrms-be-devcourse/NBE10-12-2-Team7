package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.repository.ProductRepositoryImpl
import com.dongnemarket.mobile.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * "`ProductRepository` 를 주입해 달라고 하면 `ProductRepositoryImpl` 을 준다"는 연결 선언.
 *
 * `@Binds` 는 `@Provides` 와 달리 **몸통이 없는 abstract 함수**다. 이미 `@Inject` 생성자가 있는
 * 구현체를 인터페이스에 이어 붙이는 것뿐이라 Dagger 가 코드를 만들 필요가 없다(그만큼 빠르다).
 * 그래서 클래스도 `object` 가 아니라 `abstract class` 여야 한다.
 *
 * 덕분에 ViewModel 은 인터페이스만 알면 되고, 테스트에서는 가짜 구현으로 바꿔 끼울 수 있다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProductRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
}
