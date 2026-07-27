package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.ChatMessageCreateRequest
import com.dongnemarket.mobile.data.remote.dto.ChatMessagePageResponse
import com.dongnemarket.mobile.data.remote.dto.ChatMessageResponse
import com.dongnemarket.mobile.data.remote.dto.ChatRoomCreateRequest
import com.dongnemarket.mobile.data.remote.dto.ChatRoomDetailResponse
import com.dongnemarket.mobile.data.remote.dto.ChatRoomListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 채팅 관련 HTTP 호출 선언부. Retrofit 이 이 인터페이스의 구현체를 런타임에 만들어 준다.
 *
 * Spring 의 `@FeignClient` 나 서버 `@RestController` 와 모양이 비슷한데 방향이 반대다.
 * 여기 붙은 애노테이션은 "이런 요청을 **보낸다**"는 선언이다.
 *
 * 규약 몇 가지:
 *  - 경로는 선행 `/` 없이 `api/...` 로 쓴다. baseUrl 이 `/` 로 끝나므로 이어붙고,
 *    선행 `/` 를 쓰면 baseUrl 의 path 가 잘리는 사고가 난다(습관화).
 *  - `Authorization` 헤더는 [AuthInterceptor] 가 전 요청에 자동으로 붙인다
 *    → 여기에 `@Header("Authorization")` 를 절대 쓰지 않는다.
 *  - 반환 타입은 `Response<...>` 가 아니라 껍데기째 `ApiEnvelope<T>` 다.
 *    껍데기를 벗기고 예외를 [com.dongnemarket.mobile.domain.model.AppError] 로 번역하는 일은
 *    Repository 가 `apiCall { }` 로 처리한다.
 *
 * 채팅 API 는 **전부 인증 필수**다(permitAll 경로가 하나도 없다)
 * → 토큰이 만료되면 여기서는 정직하게 401 이 온다(상품 조회와 달리 익명 통과가 없다).
 */
interface ChatApiService {

    /**
     * 채팅방 확보(get-or-create). 성공 **200**(201 아니다).
     * 이미 방이 있으면 기존 방의 roomId 를 그대로 준다.
     * 판매자가 자기 상품으로 호출하면 400 `CANNOT_CHAT_WITH_SELF`.
     */
    @POST("api/chat-rooms")
    suspend fun createRoom(
        @Body body: ChatRoomCreateRequest,
    ): ApiEnvelope<ChatRoomDetailResponse>

    /**
     * 내 채팅방 전체. 페이징 파라미터가 없다 — `data` 가 배열 그대로 온다.
     * 정렬(마지막 메시지 시각 DESC → roomId DESC)은 서버가 메모리에서 처리해 준다.
     */
    @GET("api/chat-rooms")
    suspend fun getRooms(): ApiEnvelope<List<ChatRoomListResponse>>

    /**
     * 메시지 커서 페이징.
     *
     * @param cursor null 이면 Retrofit 이 쿼리에서 아예 빼 준다 → 최신 첫 페이지.
     *   값이 있으면 `id < cursor` 인 **더 오래된** 메시지를 준다(과거 방향 전용).
     *   ⚠ 숫자가 아닌 값이 들어가면 400 이 아니라 **500** 이 온다.
     * @param size 서버가 1~100 으로 조용히 클램프한다.
     */
    @GET("api/chat-rooms/{roomId}/messages")
    suspend fun getMessages(
        @Path("roomId") roomId: Long,
        @Query("cursor") cursor: Long? = null,
        @Query("size") size: Int = 30,
    ): ApiEnvelope<ChatMessagePageResponse>

    /** 메시지 전송. 성공 **201**, 응답 data 에 저장된 메시지가 그대로 온다. */
    @POST("api/chat-rooms/{roomId}/messages")
    suspend fun sendMessage(
        @Path("roomId") roomId: Long,
        @Body body: ChatMessageCreateRequest,
    ): ApiEnvelope<ChatMessageResponse>

    /**
     * 읽음 처리. **요청 body 가 없다** → `@Body` 파라미터를 선언하지 않는다
     * (Retrofit 은 body 없는 POST 를 Content-Length: 0 으로 보내며, 서버도 Content-Type 을 요구하지 않는다).
     *
     * 성공 응답에 `data` 키 자체가 없으므로 타입 인자는 `Unit` 이고,
     * Repository 에서 `apiCall` 이 아니라 **`apiCallForUnit`** 으로 감싸야 한다
     * (`apiCall` 은 data 가 null 이면 실패로 판정한다).
     */
    @POST("api/chat-rooms/{roomId}/read")
    suspend fun markAsRead(
        @Path("roomId") roomId: Long,
    ): ApiEnvelope<Unit>
}
