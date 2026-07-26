package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.MemberLocationResponseDto
import com.dongnemarket.mobile.data.remote.dto.MemberLocationUpdateRequestDto
import com.dongnemarket.mobile.data.remote.dto.MemberResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * 회원(내 정보·내 동네) 엔드포인트 선언부.
 * 세 엔드포인트 모두 **인증 필수**이며 토큰은 [AuthInterceptor] 가 자동으로 붙인다.
 */
interface MemberApiService {

    /** 내 정보. 앱의 세션 확인 수단이며 `memberId` 의 유일한 출처다. */
    @GET("api/members/me")
    suspend fun getMyProfile(): ApiEnvelope<MemberResponseDto>

    /**
     * 내 동네 목록. 페이징 없이 전량(최대 2건) 반환한다.
     * 미설정 회원은 `data` 가 `[]` 로 온다(null 이 아니므로 `apiCall` 이 정상 성공으로 처리한다).
     */
    @GET("api/members/me/locations")
    suspend fun getMyLocations(): ApiEnvelope<List<MemberLocationResponseDto>>

    /** 내 동네 **전체 교체**. 응답은 갱신된 전체 목록. */
    @PUT("api/members/me/locations")
    suspend fun updateMyLocations(
        @Body request: MemberLocationUpdateRequestDto,
    ): ApiEnvelope<List<MemberLocationResponseDto>>
}
