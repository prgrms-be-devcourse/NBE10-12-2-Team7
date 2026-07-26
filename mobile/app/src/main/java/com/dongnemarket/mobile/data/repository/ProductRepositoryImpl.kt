package com.dongnemarket.mobile.data.repository

import com.dongnemarket.mobile.data.mapper.toDomain
import com.dongnemarket.mobile.data.remote.ProductApiService
import com.dongnemarket.mobile.data.remote.apiCall
import com.dongnemarket.mobile.domain.model.Product
import com.dongnemarket.mobile.domain.model.ProductDetail
import com.dongnemarket.mobile.domain.model.ProductPage
import com.dongnemarket.mobile.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * [ProductRepository] 의 실제 구현. HTTP 호출 → 껍데기 벗기기 → 도메인 변환까지 담당한다.
 *
 * 패턴은 세 함수 모두 같다: `apiCall { api.xxx() }.map { it.toDomain() }`
 *  - `apiCall` 이 예외를 [com.dongnemarket.mobile.domain.model.AppError] 로 번역해 `Result` 에 담아 준다
 *    → 이 클래스는 try/catch 를 쓰지 않고, 예외를 밖으로 던지지도 않는다.
 *  - `Result.map` 은 성공일 때만 실행되므로 실패는 그대로 통과한다.
 */
// 싱글톤 스코프는 ProductRepositoryModule 의 @Binds @Singleton 에서 한 번만 지정한다.
class ProductRepositoryImpl @Inject constructor(
    private val api: ProductApiService,
) : ProductRepository {

    override suspend fun getProducts(
        regions: List<String>?,
        cursor: Long?,
        size: Int,
    ): Result<ProductPage> =
        apiCall {
            api.getProducts(
                regions = regions.normalizeRegions(),
                cursor = cursor,
                size = size.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
            )
        }.map { it.toDomain() }

    override suspend fun searchProducts(
        keyword: String?,
        categoryId: Long?,
        regions: List<String>?,
    ): Result<List<Product>> =
        apiCall {
            api.searchProducts(
                // 빈 문자열을 그대로 보내면 "제목에 ''를 포함"이라는 무의미한 조건이 붙는다 → 아예 뺀다.
                keyword = keyword?.trim()?.takeIf { it.isNotEmpty() },
                categoryId = categoryId,
                regions = regions.normalizeRegions(),
            )
        }.map { list -> list.map { it.toDomain() } }

    /**
     * ⚠️ 서버에서 조회수를 올리는 호출이다. 재시도·재구성으로 중복 호출되지 않게
     * 호출자(ViewModel)가 1회 로드를 보장해야 한다. 자세한 주의사항은 인터페이스 KDoc 참고.
     */
    override suspend fun getProductDetail(productId: Long): Result<ProductDetail> =
        apiCall { api.getProductDetail(productId) }.map { it.toDomain() }

    private companion object {
        /** 서버 지역 필터 상한. 3개 이상 보내면 400 이 떨어진다. */
        const val MAX_REGION_FILTER = 2
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 100
    }

    /**
     * 지역 필터 방어 처리. 서버가 400/500 을 내기 전에 클라이언트에서 정리한다:
     * 공백 원소 제거 → 중복 제거 → **앞의 2개만** 사용 → 남은 게 없으면 null(파라미터 생략).
     *
     * 조용히 잘라내는 편을 택한 이유: 지역명은 사용자가 직접 타이핑하는 값이 아니라
     * '내 동네'(서버가 최대 2개만 저장) 에서 온 값이므로 3개가 들어오는 건 앱 버그이고,
     * 그때 화면 전체를 에러로 덮는 것보다 1·2번 동네 결과를 보여 주는 편이 낫다.
     */
    private fun List<String>?.normalizeRegions(): List<String>? =
        this?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.take(MAX_REGION_FILTER)
            ?.takeIf { it.isNotEmpty() }
}
