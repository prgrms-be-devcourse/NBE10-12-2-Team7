package com.dongnemarket.mobile.di

import com.dongnemarket.mobile.data.repository.CategoryRepositoryImpl
import com.dongnemarket.mobile.data.repository.RegionRepositoryImpl
import com.dongnemarket.mobile.domain.repository.CategoryRepository
import com.dongnemarket.mobile.domain.repository.RegionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * "이 인터페이스를 요구하면 저 구현체를 넣어라"를 선언하는 모듈.
 *
 * `@Binds` 는 `@Provides` 와 달리 **본문이 없는 추상 함수**다.
 * 새로 만들 객체가 아니라 이미 만들 수 있는 구현체(`@Inject` 생성자를 가진)를
 * 인터페이스 자리에 그대로 꽂아 주는 것뿐이라 Dagger 가 코드를 덜 생성한다.
 * 그래서 이 모듈은 `object` 가 아니라 `abstract class` 여야 한다.
 *
 * ViewModel 이 `CategoryRepository` 를 주입받으면 여기 선언 덕분에
 * `CategoryRepositoryImpl` 인스턴스가 들어온다 → UI 는 구현체를 몰라도 된다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindRegionRepository(impl: RegionRepositoryImpl): RegionRepository
}
