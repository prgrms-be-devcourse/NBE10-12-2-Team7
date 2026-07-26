package com.dongnemarket.mobile.data.repository

import com.dongnemarket.mobile.data.mapper.toRegionDomain
import com.dongnemarket.mobile.data.remote.RegionApiService
import com.dongnemarket.mobile.data.remote.apiCall
import com.dongnemarket.mobile.domain.model.Region
import com.dongnemarket.mobile.domain.repository.RegionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [RegionRepository] 의 실제 구현.
 *
 * 캐시 이유가 카테고리보다 강하다: 한 번의 호출이 **229건**을 끌고 오는데
 * 검색·페이징 파라미터가 없어서(계약 §8-10) 줄여 받을 수단이 없다.
 * 동네 선택 화면에 재진입할 때마다 229건을 다시 받는 건 낭비이므로 세션 내 1회로 고정한다.
 * (지역 마스터 데이터는 앱에서 바꿀 수 없고 서버에 관리 API 도 없다.)
 */
@Singleton
class RegionRepositoryImpl @Inject constructor(
    private val api: RegionApiService,
) : RegionRepository {

    @Volatile
    private var cached: List<Region>? = null

    private val mutex = Mutex()

    override suspend fun getRegions(): Result<List<Region>> {
        cached?.let { return Result.success(it) }

        return mutex.withLock {
            // 락을 기다리는 사이 앞선 호출이 채웠는지 다시 확인(229건 중복 수신 방지).
            cached?.let { return@withLock Result.success(it) }

            apiCall { api.getRegions() }
                .map { it.toRegionDomain() }
                .onSuccess { cached = it }
        }
    }
}
