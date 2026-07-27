package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.CategoryResponse
import retrofit2.http.GET

/**
 * 카테고리 API 창구. Retrofit 이 이 인터페이스의 구현체를 런타임에 만들어 준다
 * (Spring Data JPA 리포지토리 인터페이스가 구현 없이 동작하는 것과 같은 원리다).
 *
 * 규칙 두 가지:
 *  - 경로에 **선행 `/` 를 붙이지 않는다.** `@GET("/api/...")` 로 쓰면 baseUrl 의 path 가 잘린다.
 *  - 반환 타입은 껍데기째 [ApiEnvelope] 로 받는다. 껍데기를 벗기는 일은
 *    Repository 의 `apiCall { }` 이 담당한다(`Response<...>` 는 쓰지 않는다).
 *
 * `Authorization` 헤더는 [AuthInterceptor] 가 자동으로 붙이므로 `@Header` 를 쓰지 않는다.
 * (카테고리는 permitAll 이라 토큰이 없어도, 만료돼도 200 이 온다 — 계약 §0.13)
 */
interface CategoryApiService {

    /**
     * `GET /api/categories` — 카테고리 전량 조회.
     *
     * 쿼리 파라미터가 없다. 페이징·검색·정렬 옵션이 **존재하지 않고**
     * 시드 8종이 `id ASC` 로 한 번에 온다.
     */
    @GET("api/categories")
    suspend fun getCategories(): ApiEnvelope<List<CategoryResponse>>
}
