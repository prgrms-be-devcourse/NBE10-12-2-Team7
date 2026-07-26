package com.dongnemarket.mobile.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * 찜 상태의 **단일 진실 공급원(single source of truth)**.
 *
 * 왜 이런 모양인가 (이 인터페이스에서 가장 중요한 이야기):
 * 상품 상세 응답에 "내가 이 상품을 찜했는지"가 **구조적으로 없다.**
 * 상세 API 는 permitAll 이고 `@AuthenticationPrincipal` 을 아예 받지 않아 서버가 요청자를 모르며,
 * 찜 여부를 묻는 단건 API 도 없다. 유일한 방법은 내 찜 목록을 한 번 받아
 * **productId 집합을 앱 메모리에 캐시하고 화면들이 그것을 구독**하는 것이다.
 * 그래서 이 Repository 는 "요청을 대신 보내주는 창구"가 아니라 **상태를 들고 있는 창구**다.
 *
 * 인터페이스가 Domain 에 있는 이유(의존성 역전): 상위(ViewModel)가 하위(Retrofit 구현)를
 * 직접 알지 않도록 Domain 이 규격을 소유하고 Data 가 그 규격을 구현한다.
 * Spring 의 `interface XxxRepository` + 구현체 주입과 같은 구도다.
 *
 * 실패는 예외로 던지지 않고 항상 [Result] 로 돌려준다.
 */
interface FavoriteRepository {

    /**
     * 찜한 상품 id 집합. 여러 화면(홈 카드·상세 하트)이 같은 값을 보도록 앱 전역 단일 소스로 둔다.
     *
     * ⚠ 알려진 한계: 이 집합은 `GET /api/members/me/favorites` 로 채우는데
     * 서버가 **최근 200건만** 준다 → 찜이 200개를 넘는 사용자는 오래된 찜의 하트가 꺼져 보인다.
     * 그 상태에서 다시 찜하면 서버가 409 를 주는데, [addFavorite] 이 이를
     * "이미 찜한 상태"로 흡수해 집합에 넣으므로 화면은 스스로 치유된다.
     */
    val favoriteProductIds: StateFlow<Set<Long>>

    /**
     * 서버에서 내 찜 목록을 받아 캐시를 채운다(집합 전체 교체).
     * 로그인 직후 1회, 그리고 화면 재진입·pull-to-refresh 때 호출한다.
     */
    suspend fun refreshFavorites(): Result<Unit>

    /** 찜 등록. 이미 찜한 상태(409)는 실패가 아니라 성공으로 취급한다. */
    suspend fun addFavorite(productId: Long): Result<Unit>

    /** 찜 취소. 이미 찜이 없는 상태(404)는 실패가 아니라 성공으로 취급한다. */
    suspend fun removeFavorite(productId: Long): Result<Unit>
}
