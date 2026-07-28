package com.dongnemarket.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `GET /api/categories` 응답 `data` 배열의 원소.
 * 원본: backend `com.dongnemarket.category.dto.CategoryResponse`
 *
 * ```json
 * { "id": 1, "name": "디지털기기" }
 * ```
 *
 * ⚠ **PK 키 이름이 `id` 다.** 지역은 `regionId` 인데 카테고리는 `id` 로 비대칭이다.
 * `categoryId` 로 선언하면 예외 없이 조용히 파싱 실패(값 없음)로 이어진다.
 *
 * 필드는 이 두 개가 전부다. `iconUrl`/`sortOrder`/`parentId` 는 서버에 존재하지 않는다.
 */
@Serializable
data class CategoryResponse(
    val id: Long,
    val name: String,
)

/**
 * `GET /api/regions` 응답 `data` 배열의 원소.
 * 원본: backend `com.dongnemarket.region.dto.RegionResponse`
 *
 * ```json
 * { "regionId": 1, "name": "서울 강남구" }
 * ```
 *
 * ⚠ 카테고리(`id`)와 달리 지역은 **`regionId`** 다.
 *
 * `name` 은 `"시도 시군구"` 가 공백 하나로 붙은 **한 문자열**이다. 계층 테이블도, 좌표도 없다.
 * 그리고 상품 필터·내 동네 설정은 `regionId` 가 아니라 이 **`name` 원문**으로 통신한다(계약 §7-18).
 */
@Serializable
data class RegionResponse(
    val regionId: Long,
    val name: String,
)
