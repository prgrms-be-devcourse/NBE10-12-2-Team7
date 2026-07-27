package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.MyFavoriteResponseDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 찜(favorite) 관련 백엔드 엔드포인트 3개의 선언.
 *
 * Spring 의 `@RestController` 와 짝을 이루는 반대편이다. 서버는 애노테이션을 보고
 * "이 URL 요청을 받겠다"고 선언하고, Retrofit 은 같은 정보를 보고
 * "이 URL 로 요청을 보내겠다"는 구현체를 런타임에 만들어 준다.
 * 그래서 이 파일에는 몸통이 없는 함수 선언만 있다.
 *
 * 규칙 두 가지:
 *  - 경로에 선행 `/` 를 쓰지 않는다(`api/...`). baseUrl 의 path 가 잘리는 사고를 예방한다.
 *  - `Authorization` 헤더는 [AuthInterceptor] 가 자동으로 붙인다 → `@Header` 를 쓰지 않는다.
 */
interface FavoriteApiService {

    /**
     * 내가 찜한 상품 목록. **하트 초기 상태를 알아낼 수 있는 유일한 경로다.**
     *
     * 상품 상세 응답에는 '내가 찜했는지' 필드가 없고(상세 API 는 permitAll 이라 서버가 요청자를 모른다),
     * 찜 여부 단건 조회 API 도 없다. 그래서 이 목록을 받아 productId 집합으로 만들어 캐시한다.
     *
     * ⚠ 페이징 파라미터가 없고 **서버가 최근 200건으로 하드캡**한다(`createdAt DESC, id DESC`).
     * ⚠ 삭제·숨김된 상품의 찜은 목록에서 조용히 빠진다 → "3개 찜했는데 2개만 온다"는 정상 동작이다.
     */
    @GET("api/members/me/favorites")
    suspend fun getMyFavorites(): ApiEnvelope<List<MyFavoriteResponseDto>>

    /**
     * 찜 등록. **토글이 아니다** — 취소는 아래 DELETE 로 분리돼 있고,
     * 이미 찜한 상품에 또 POST 하면 409 `FAVORITE_ALREADY_EXISTS` 가 온다.
     *
     * 요청 본문이 없어서 `@Body` 파라미터를 두지 않는다(Retrofit 이 Content-Length: 0 으로 보낸다).
     *
     * 반환을 `ApiEnvelope<Unit>` 으로 둔 이유: 성공 시 서버는 201 과 함께
     * `data: {id, productId, createdAt}` 를 주지만 **앱이 쓸 값이 하나도 없다**
     * (갱신된 favoriteCount 조차 오지 않는다). 필요 없는 필드를 선언해 두면
     * 서버 응답 모양이 조금 바뀔 때 '성공한 요청이 파싱 실패로 실패 처리'되는 최악의 사고가 난다.
     * `Unit` + `ignoreUnknownKeys` 조합이면 본문이 무엇이든 성공은 성공으로 판정된다.
     */
    @POST("api/products/{productId}/favorites")
    suspend fun addFavorite(@Path("productId") productId: Long): ApiEnvelope<Unit>

    /**
     * 찜 취소. 성공 시 200 이고 **응답에 `data` 키 자체가 없다**
     * (백엔드 `ApiResponse` 에만 `@JsonInclude(NON_NULL)` 이 걸려 있다).
     * 찜하지 않은 상품을 취소하면 404 `FAVORITE_NOT_FOUND` 다.
     */
    @DELETE("api/products/{productId}/favorites")
    suspend fun removeFavorite(@Path("productId") productId: Long): ApiEnvelope<Unit>
}
