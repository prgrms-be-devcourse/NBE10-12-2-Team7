package com.dongnemarket.mobile.domain.repository

import com.dongnemarket.mobile.domain.model.Region

/**
 * 지역(동네) 조회 창구. 설계 원칙은 [CategoryRepository] 와 같다.
 *
 * ### 범위 주의
 * 이 Repository 는 **"전국에 어떤 동네가 있는가"(마스터 데이터)만** 다룬다.
 * "내 동네가 무엇인가"(`GET·PUT /api/members/me/locations`)는 회원 도메인의 책임이며
 * `MemberRepository` 가 제공한다 → 여기에 중복 구현하지 마라.
 */
interface RegionRepository {

    /**
     * 전국 지역 목록을 조회한다. 동네 선택 화면에서 쓴다.
     *
     * - **229건이 한 번에 전량** 온다. 검색·페이징 API 가 없으므로(계약 §8-10)
     *   검색 UX 는 이 결과를 메모리에 들고 `name.contains(query)` 로 **UI(ViewModel)에서 필터링**한다.
     * - 시/도 섹션 헤더가 필요하면 `groupBy { it.sido }` 를 쓴다(`"세종"` 예외 처리 포함됨).
     * - 사용자가 고른 항목을 서버로 보낼 때는 `regionId` 가 아니라 **`name`** 을 보낸다.
     */
    suspend fun getRegions(): Result<List<Region>>
}
