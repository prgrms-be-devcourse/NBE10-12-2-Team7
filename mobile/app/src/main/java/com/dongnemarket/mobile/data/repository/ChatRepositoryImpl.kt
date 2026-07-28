package com.dongnemarket.mobile.data.repository

import com.dongnemarket.mobile.data.mapper.toDomain
import com.dongnemarket.mobile.data.remote.ChatApiService
import com.dongnemarket.mobile.data.remote.apiCall
import com.dongnemarket.mobile.data.remote.apiCallForUnit
import com.dongnemarket.mobile.data.remote.dto.ChatMessageCreateRequest
import com.dongnemarket.mobile.data.remote.dto.ChatRoomCreateRequest
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatMessagePage
import com.dongnemarket.mobile.domain.model.ChatRoom
import com.dongnemarket.mobile.domain.model.ChatRoomHeader
import com.dongnemarket.mobile.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ChatRepository] 의 HTTP 구현체.
 *
 * 모든 함수가 같은 3단 구조다:
 *  1. `apiCall { api.xxx() }` — 껍데기를 벗기고 예외를 [AppError] 로 번역해 `Result` 로 만든다
 *  2. `.map { ... }` — DTO → 도메인 모델 변환(성공일 때만 실행된다)
 *  3. 그대로 반환 — **예외를 던지지 않는다**
 *
 * `map` 을 쓰는 이유: 실패 케이스를 if 로 분기하지 않아도 실패가 자동으로 흘러내려간다.
 * 실패 종류를 여기서 바꿔 담을 필요가 없으므로 함수 몸통이 한 줄로 끝난다.
 *
 * 상태(캐시)를 갖지 않는다 — 채팅은 폴링으로 계속 새로 받으므로 캐시가 오히려 방해가 된다.
 * `@Singleton` 인 것은 인스턴스를 여러 개 만들 이유가 없기 때문이다.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApiService,
) : ChatRepository {

    /**
     * 서버가 방 전체(`roomId` + 상품 + 판매자)를 주지만 `roomId` 만 꺼내 버린다.
     * 화면 이동에 필요한 것은 `roomId` 하나이고, 헤더는 [getRoomHeader] 가 책임지기 때문에
     * 반환 타입을 DTO 가 새어 나가지 않는 `Long` 으로 좁혔다.
     */
    override suspend fun createRoom(productId: Long): Result<Long> =
        apiCall { api.createRoom(ChatRoomCreateRequest(productId)) }
            .map { it.roomId }

    override suspend fun getRooms(): Result<List<ChatRoom>> =
        apiCall { api.getRooms() }
            .map { rooms -> rooms.map { it.toDomain() } }

    /**
     * ⚠ **우회 구현**: `GET /api/chat-rooms/{roomId}` 라는 API 가 백엔드에 없다.
     * 그래서 방 목록을 통째로 받아 `roomId` 로 찾는다. 목록이 전량 반환(페이징 없음)이라
     * 내가 참여한 방이면 이 1회 호출로 반드시 찾을 수 있다.
     *
     * 못 찾은 경우를 `null` 이나 빈 헤더로 뭉개지 않고 실패로 만드는 이유:
     * "참여자가 아닌 방"이거나 "없는 방"이라는 뜻이므로 화면이 방을 열어서는 안 된다.
     * 실패에 서버가 그 상황에서 쓰는 코드(`CHAT_ROOM_NOT_FOUND`)를 그대로 달아 주므로,
     * UI 는 서버가 낸 404 와 이 우회 실패를 구분하지 않아도 된다.
     */
    override suspend fun getRoomHeader(roomId: Long): Result<ChatRoomHeader> {
        // 네트워크 실패(401·오프라인 등)는 그대로 위로 전달한다. 여기서 삼키면 원인이 사라진다.
        val rooms = getRooms().getOrElse { return Result.failure(it) }

        val room = rooms.firstOrNull { it.roomId == roomId }
            ?: return Result.failure(
                AppError.Api(
                    status = 404,
                    code = "CHAT_ROOM_NOT_FOUND",
                    message = "채팅방을 찾을 수 없습니다.",
                ),
            )
        return Result.success(ChatRoomHeader.from(room))
    }

    override suspend fun getMessages(
        roomId: Long,
        cursor: Long?,
        size: Int,
    ): Result<ChatMessagePage> =
        apiCall { api.getMessages(roomId = roomId, cursor = cursor, size = size) }
            .map { it.toDomain() }   // 여기서 최신순 → 시간순으로 뒤집힌다 (ChatMapper 참고)

    override suspend fun sendMessage(roomId: Long, content: String): Result<ChatMessage> =
        apiCall { api.sendMessage(roomId, ChatMessageCreateRequest(content)) }
            .map { it.toDomain() }

    /**
     * 성공 응답에 `data` 키 자체가 없으므로 [apiCallForUnit] 을 쓴다.
     * [apiCall] 로 감싸면 "data 가 null" → `AppError.EmptyBody` 실패로 오판한다.
     */
    override suspend fun markAsRead(roomId: Long): Result<Unit> =
        apiCallForUnit { api.markAsRead(roomId) }
}
