package com.dongnemarket.mobile.data.repository

import com.dongnemarket.mobile.data.mapper.toCategoryDomain
import com.dongnemarket.mobile.data.remote.CategoryApiService
import com.dongnemarket.mobile.data.remote.apiCall
import com.dongnemarket.mobile.domain.model.Category
import com.dongnemarket.mobile.domain.repository.CategoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CategoryRepository] 의 실제 구현. Data 계층에 있고, DTO↔도메인 변환을 여기서 끝낸다.
 *
 * `@Singleton` 을 클래스에도 붙인 이유: 아래 세션 캐시가 **인스턴스 하나에만** 담기므로
 * 화면마다 새 인스턴스가 생기면 캐시가 무의미해진다.
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val api: CategoryApiService,
) : CategoryRepository {

    /**
     * 앱 실행 중 유지되는 메모리 캐시.
     *
     * 카테고리는 시드로 고정된 **8건 마스터 데이터**이고 앱에서 추가·수정할 방법이 없다
     * (관리 API 자체가 없다). 홈에 들어올 때마다 다시 받을 이유가 없어서 한 번만 받는다.
     * 실패는 캐시하지 않으므로 재시도는 정상적으로 네트워크를 다시 탄다.
     *
     * `@Volatile`: 다른 스레드가 쓴 값을 즉시 보도록 보장한다(캐시 히트 체크는 락 밖에서 하므로).
     */
    @Volatile
    private var cached: List<Category>? = null

    /** 홈 진입 시 여러 곳에서 동시에 호출돼도 네트워크 요청이 중복되지 않게 막는 잠금. */
    private val mutex = Mutex()

    override suspend fun getCategories(): Result<List<Category>> {
        // 1차 확인: 이미 받아 뒀으면 락도 잡지 않고 바로 돌려준다.
        cached?.let { return Result.success(it) }

        return mutex.withLock {
            // 2차 확인: 락을 기다리는 동안 앞선 호출이 이미 채웠을 수 있다(double-checked locking).
            cached?.let { return@withLock Result.success(it) }

            apiCall { api.getCategories() }
                .map { it.toCategoryDomain() }
                .onSuccess { cached = it }
        }
    }
}
