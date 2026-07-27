package com.dongnemarket.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `GET /api/members/me/favorites` 응답 배열의 원소.
 * 원본: backend `MyFavoriteResponse`
 *
 * ```json
 * { "favoriteId": 7, "createdAt": "2026-07-26T13:45:30.123",
 *   "product": { "productId": 12, "title": "...", "price": 800000.00, ... } }
 * ```
 *
 * ⚠ 찜 PK 키 이름이 등록 응답(`id`)과 목록(`favoriteId`)에서 서로 다르다.
 * 우리는 목록만 쓰므로 `favoriteId` 로 받는다(`id` 로 적으면 조용히 null 이 된다).
 *
 * ⚠ **서버가 보내는 필드 중 일부만 선언했다.** Phase 1 에 '내 찜 목록' 화면이 없어서
 * 이 응답에서 실제로 필요한 값은 `product.productId` 뿐이다(하트 on/off 판정용 id 집합).
 * `Json { ignoreUnknownKeys = true }`(NetworkModule) 덕분에 선언하지 않은
 * title·price·region·tradeStatus·thumbnailUrl 은 그냥 버려진다.
 * 목록 화면을 만들 때 필드를 추가하면 되고, 그때 `price` 는 반드시 BigDecimal 로 받아야 한다
 * (서버가 `800000.00` 처럼 소수부를 붙여 보내므로 Int/Long 파싱은 깨진다).
 */
@Serializable
data class MyFavoriteResponseDto(
    val favoriteId: Long,
    /** 찜한 시각. 오프셋(Z) 없는 ISO local 문자열이라 String 으로 받는다. */
    val createdAt: String? = null,
    val product: FavoriteProductRefDto,
)

/**
 * 찜 목록 원소가 품고 있는 상품 조각.
 *
 * 상품 요약 DTO(`ProductSummaryResponse`)의 진부분집합이고 필드 구성도 다르다
 * (memberId·viewCount·favoriteCount·hidden 이 없다) → **상품 DTO 와 합치지 말 것.**
 * 여기서는 위 KDoc 의 이유로 `productId` 만 선언한다.
 */
@Serializable
data class FavoriteProductRefDto(
    val productId: Long,
)
