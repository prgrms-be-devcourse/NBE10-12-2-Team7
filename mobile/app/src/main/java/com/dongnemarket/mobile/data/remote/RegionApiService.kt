package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.RegionResponse
import retrofit2.http.GET

/**
 * 지역(동네) API 창구. 사용 규칙은 [CategoryApiService] 와 같다.
 */
interface RegionApiService {

    /**
     * `GET /api/regions` — 전국 지역 **229건 전량** 조회.
     *
     * 검색어·페이징 파라미터가 **없다**(계약 §8-10). 리포지토리에 `like` 조회 메서드조차 없어서
     * 서버에서 걸러 받는 방법이 아예 없다 → 한 번 받아 두고 **클라이언트가 메모리에서 필터링**한다.
     * 정렬은 `name` 가나다 ASC 고정.
     */
    @GET("api/regions")
    suspend fun getRegions(): ApiEnvelope<List<RegionResponse>>
}
