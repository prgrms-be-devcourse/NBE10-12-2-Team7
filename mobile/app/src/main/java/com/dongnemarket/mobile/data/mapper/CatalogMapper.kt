package com.dongnemarket.mobile.data.mapper

import com.dongnemarket.mobile.data.remote.dto.CategoryResponse
import com.dongnemarket.mobile.data.remote.dto.RegionResponse
import com.dongnemarket.mobile.domain.model.Category
import com.dongnemarket.mobile.domain.model.Region

/**
 * DTO(서버 계약) → 도메인 모델 변환. 카테고리·지역 담당.
 *
 * 이 경계가 왜 필요한가: DTO 는 **서버 JSON 모양에 종속된 타입**이다.
 * 카테고리 PK 키가 `id`, 지역 PK 키가 `regionId` 로 비대칭인 것도 서버 사정일 뿐이라
 * 그대로 UI 까지 흘리면 화면 코드가 백엔드 명명 실수까지 따라 하게 된다.
 * 여기서 한 번 번역해 두면 서버 키 이름이 바뀌어도 고칠 곳이 이 파일 한 곳이다.
 *
 * (JPA Entity → Response DTO 로 변환하던 Spring 쪽 매퍼와 방향만 반대인 같은 자리다.)
 */

/** 카테고리 DTO 한 건 → 도메인. 서버 키 `id` 를 도메인 [Category.id] 로 옮긴다. */
fun CategoryResponse.toDomain(): Category = Category(
    id = id,
    name = name,
)

/** 카테고리 목록 변환. **서버가 준 순서(id ASC = 시드 순서)를 유지한다** — 재정렬 금지(계약 §8-8). */
fun List<CategoryResponse>.toCategoryDomain(): List<Category> = map { it.toDomain() }

/** 지역 DTO 한 건 → 도메인. 서버 키는 `regionId` 다(카테고리와 다르다). */
fun RegionResponse.toDomain(): Region = Region(
    regionId = regionId,
    name = name,
)

/**
 * 지역 목록 변환. 서버 순서(`name` 가나다 ASC)를 유지한다.
 *
 * [Region.name] 을 `trim()` 하거나 공백을 정규화하지 **않는다.**
 * 이 문자열이 그대로 상품 필터·내 동네 설정 요청에 실려 서버에서 완전 비교되기 때문에,
 * 여기서 한 글자라도 손대면 원인 불명 400 이 난다(계약 §7-18).
 */
fun List<RegionResponse>.toRegionDomain(): List<Region> = map { it.toDomain() }
