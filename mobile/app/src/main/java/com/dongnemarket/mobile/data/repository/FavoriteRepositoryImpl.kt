package com.dongnemarket.mobile.data.repository

import com.dongnemarket.mobile.data.remote.FavoriteApiService
import com.dongnemarket.mobile.data.remote.apiCall
import com.dongnemarket.mobile.data.remote.apiCallForUnit
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FavoriteRepository] 의 실제 구현. 서버 호출 + **찜 id 캐시 보관**을 담당한다.
 *
 * ⚠ `@Singleton` 이 이 클래스의 핵심이다.
 * 캐시([_favoriteProductIds])를 인스턴스 필드로 들고 있으므로 인스턴스가 여러 개 생기면
 * 화면마다 서로 다른 집합을 보게 되고, 목록에서 찜한 상품이 상세에서 꺼져 있는 식으로 하트가 어긋난다.
 * `@Binds` 로 인터페이스에 연결할 때 **바인딩 쪽뿐 아니라 구현 클래스 자체에도** 이 애노테이션이
 * 있어야 Hilt 가 인스턴스를 하나만 만든다(Spring 의 기본 싱글톤 빈과 같은 상태를 손으로 지정하는 셈).
 *
 * 상태를 들고 있는 Repository 라서 Data 계층에 있는 것이 어색해 보일 수 있는데,
 * '서버 응답을 앱이 쓰기 좋은 형태로 보관하는 일'은 Data 계층의 책임이다.
 * Domain·UI 는 [favoriteProductIds] 라는 규격만 알고 캐시의 존재를 몰라도 된다.
 */
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val api: FavoriteApiService,
) : FavoriteRepository {

    /**
     * 쓰기 가능한 원본. 외부에는 읽기 전용([asStateFlow])으로만 노출해
     * "값을 바꾸는 경로는 이 클래스의 세 함수뿐"이라는 규칙을 타입으로 강제한다.
     */
    private val _favoriteProductIds = MutableStateFlow<Set<Long>>(emptySet())

    override val favoriteProductIds: StateFlow<Set<Long>> = _favoriteProductIds.asStateFlow()

    /**
     * 서버 목록으로 캐시를 **통째로 교체**한다(부분 병합이 아니다).
     * 서버가 정답이므로, 다른 기기에서 취소한 찜이 이 기기에 남아 있는 상태도 이때 정리된다.
     */
    override suspend fun refreshFavorites(): Result<Unit> {
        val result = apiCall { api.getMyFavorites() }

        result
            .onSuccess { favorites ->
                _favoriteProductIds.value = favorites.mapTo(mutableSetOf()) { it.product.productId }
            }
            .onFailure { error ->
                // 401 = 로그인 안 된 상태(또는 토큰 만료). 익명 사용자에게 찜이 있을 수 없으니 캐시를 비운다.
                // 반대로 네트워크 실패는 캐시를 유지한다 — 지하철에서 잠깐 끊겼다고 하트가 전부 꺼지면 안 된다.
                if (error is AppError.Unauthorized) {
                    _favoriteProductIds.value = emptySet()
                }
            }

        // 목록 자체는 화면이 쓰지 않는다(Phase 1 에 '내 찜 목록' 화면이 없다) → 성공/실패만 돌려준다.
        return result.map { }
    }

    override suspend fun addFavorite(productId: Long): Result<Unit> {
        val result = apiCallForUnit { api.addFavorite(productId) }
            // 409 = 이미 찜한 상품. 서버 목록이 최근 200건뿐이어서 캐시에 없던 찜을 다시 누르면 여기로 온다.
            // 우리가 원하는 최종 상태(찜됨)에 이미 도달했으므로 에러로 올리지 않고 성공으로 흡수한다.
            // (낙관적 갱신 UI 에서 이걸 실패로 올리면 하트가 켜졌다 꺼지며 깜빡인다.)
            .recoverIf { it.status == 409 }

        result.onSuccess { _favoriteProductIds.update { current -> current + productId } }
        return result
    }

    override suspend fun removeFavorite(productId: Long): Result<Unit> {
        val result = apiCallForUnit { api.removeFavorite(productId) }
            // 404 FAVORITE_NOT_FOUND = 이미 찜이 없는 상태 → 이것도 '원하는 상태 도달'이라 성공 처리.
            // status 만 보지 않고 error 코드까지 확인하는 이유: 경로 오타로 나는 404 는
            // 스프링 기본 바디라 error 가 "Not Found" 인데, 그건 우리 버그이므로 삼키면 안 된다.
            .recoverIf { it.status == 404 && it.code == ERROR_FAVORITE_NOT_FOUND }

        result.onSuccess { _favoriteProductIds.update { current -> current - productId } }
        return result
    }

    /**
     * 서버가 준 실패 중 [predicate] 에 맞는 것만 성공으로 되돌린다.
     * 조건에 맞지 않으면 원래 예외를 그대로 다시 던져 실패로 남긴다.
     * (`AppError.Api` 가 아닌 실패 — 네트워크 끊김·401 — 은 절대 흡수하지 않는다.)
     */
    private fun Result<Unit>.recoverIf(predicate: (AppError.Api) -> Boolean): Result<Unit> =
        recoverCatching { error ->
            if (error is AppError.Api && predicate(error)) Unit else throw error
        }

    private companion object {
        const val ERROR_FAVORITE_NOT_FOUND = "FAVORITE_NOT_FOUND"
    }
}
