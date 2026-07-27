package com.dongnemarket.mobile.domain.repository

import com.dongnemarket.mobile.domain.model.ChatMessage
import com.dongnemarket.mobile.domain.model.ChatMessagePage
import com.dongnemarket.mobile.domain.model.ChatRoom
import com.dongnemarket.mobile.domain.model.ChatRoomHeader

/**
 * 채팅 기능이 서버에 요구하는 능력의 목록.
 *
 * 인터페이스를 **Domain 이 소유**하고 구현체를 Data 에 두는 것이 의존성 역전이다.
 * Spring 에 빗대면 `XxxRepository` 인터페이스는 도메인 쪽에 두고
 * JPA/HTTP 구현 세부는 어댑터로 밀어내는 것과 같은 배치다.
 * ViewModel 은 이 인터페이스만 알고, Retrofit·DTO·JSON 은 전혀 모른다.
 *
 * ## 계약 두 가지
 * 1. **예외를 던지지 않는다.** 실패는 항상 `Result.failure(AppError)` 로 온다
 *    → 화면은 `onFailure { it.userMessage }` 만 보면 된다.
 * 2. **DTO 를 내보내지 않는다.** 반환 타입은 전부 순수 도메인 모델이다.
 *
 * ## 실시간이 없다
 * 이 브랜치의 백엔드에 WebSocket/STOMP/SSE 가 **0건**이다.
 * 그래서 이 인터페이스에 Flow 구독이 없고, 새 메시지는 화면이 보이는 동안
 * [getMessages] / [getRooms] 를 주기적으로 다시 호출하는 **폴링**으로만 얻는다.
 * 폴링 루프(주기·취소·중복 제거)는 화면 생명주기를 아는 ViewModel 의 책임이므로
 * 여기서는 "한 번 조회" 함수만 제공한다.
 */
interface ChatRepository {

    /**
     * 상품에 대한 채팅방을 확보하고 `roomId` 를 돌려준다. (`POST /api/chat-rooms`)
     *
     * **get-or-create 멱등**이다 — 이미 방이 있으면 서버가 기존 방의 같은 `roomId` 를
     * 그대로 200 으로 준다. 신규/기존을 구분하는 플래그도 상태코드 차이도 없으니
     * 호출 전에 "방이 있는지" 확인할 필요가 없다. **조건 없이 이 함수 하나만 부르고 이동**하라.
     *
     * ⚠ **판매자는 자기 상품으로 방을 만들 수 없다.**
     * `AppError.Api(status = 400, code = "CANNOT_CHAT_WITH_SELF")` 가 온다.
     * 내 상품이면 애초에 '채팅하기' 버튼을 숨기고, 그래도 눌린 경우엔 이 코드를 보고
     * "내 상품에는 채팅을 걸 수 없어요" 처럼 안내하라(서버 message 를 그대로 써도 된다).
     *
     * 그 밖의 실패: 404 `PRODUCT_NOT_FOUND`(삭제·숨김 상품), 401 계열.
     */
    suspend fun createRoom(productId: Long): Result<Long>

    /**
     * 내가 참여한 모든 채팅방. (`GET /api/chat-rooms`)
     *
     * **페이징이 없고 전량 반환**이다 → 무한스크롤을 붙일 수 없고, 붙일 필요도 없다.
     * 정렬(마지막 메시지 시각 DESC)은 서버가 이미 해 뒀으니 재정렬하지 마라.
     * 방이 없으면 `Result.success(emptyList())` 다(실패가 아니다).
     *
     * 실패: 401 계열만.
     */
    suspend fun getRooms(): Result<List<ChatRoom>>

    /**
     * 채팅방 헤더(상품 카드 + 상대 닉네임)를 `roomId` 로 조회한다.
     *
     * ⚠ **이 함수는 우회 구현이다.** 백엔드에 `GET /api/chat-rooms/{roomId}` 가
     * 아예 없어서(계약 §7-16), 내부에서 [getRooms] 를 호출해 목록에서 `roomId` 로 찾아 만든다.
     * 목록이 전량 반환이라 참여 중인 방이면 1회 호출로 항상 찾을 수 있다.
     *
     * 그래서 비용이 "방 하나"가 아니라 "내 방 전체"다.
     * 목록 화면에서 방으로 들어갈 때처럼 이미 [ChatRoom] 을 손에 들고 있다면
     * 이 함수를 부르지 말고 `ChatRoomHeader.from(room)` 을 써라(호출 0회).
     * 이 함수는 **상품 상세에서 방을 새로 만든 직후**와
     * **프로세스 재시작으로 roomId 만 복원된 경우**를 위한 것이다.
     *
     * 목록에 없는 `roomId`(참여자가 아니거나 없는 방)면
     * `AppError.Api(status = 404, code = "CHAT_ROOM_NOT_FOUND")` 로 실패한다.
     */
    suspend fun getRoomHeader(roomId: Long): Result<ChatRoomHeader>

    /**
     * 메시지 조회. (`GET /api/chat-rooms/{roomId}/messages`)
     *
     * 두 가지 용도를 한 함수로 겸한다.
     *  - **최신 폴링**: `cursor = null` → 최신 첫 페이지. 서버에 `since`/`after` 같은
     *    증분 파라미터가 없어서(계약 §7-13) 폴링은 늘 첫 페이지를 통째로 다시 받고,
     *    로컬이 가진 최대 `messageId` 보다 큰 것만 골라 append 해야 한다.
     *  - **과거 더보기**: `cursor = 직전 페이지의 nextCursor` → 그보다 **오래된** 메시지.
     *
     * ⚠ 반환되는 [ChatMessagePage.messages] 는 서버 순서(최신 먼저)를 뒤집어
     * **오래된 것 → 최신 순**으로 담겨 온다. 화면에서 다시 뒤집지 마라.
     *
     * @param size 기본 30. 서버가 1~100 으로 **조용히 클램프**하니 잘못된 값에 에러를 기대하지 마라.
     *
     * 실패: 403 `CHAT_ACCESS_DENIED`(참여자 아님 — **404 가 아니다**),
     * 404 `CHAT_ROOM_NOT_FOUND`, 401 계열.
     */
    suspend fun getMessages(
        roomId: Long,
        cursor: Long? = null,
        size: Int = 30,
    ): Result<ChatMessagePage>

    /**
     * 메시지 전송. (`POST /api/chat-rooms/{roomId}/messages`, 201)
     *
     * 응답에 **저장된 메시지가 그대로 담겨 오므로 재조회 없이 목록에 붙일 수 있다.**
     * 낙관적 UI(먼저 그려 두기)를 쓰면 폴링이 같은 메시지를 또 가져오므로
     * `messageId` 기준 중복 제거가 필수다.
     *
     * `content` 는 서버 검증이 `@NotBlank @Size(max = 1000)` 이다.
     * 빈 문자열·1000자 초과는 400 `INVALID_INPUT_VALUE` 가 되지만
     * **어느 필드가 틀렸는지 알려주지 않으니** 전송 버튼 활성 조건으로 미리 막아라.
     *
     * 실패: 400 `CHAT_PARTNER_WITHDRAWN`(상대 탈퇴 — **읽기는 계속 200 이고 전송만 막힌다**.
     * 입력창 비활성 판정은 닉네임 문자열이 아니라 이 코드로 하라),
     * 403 `CHAT_ACCESS_DENIED`, 404 `CHAT_ROOM_NOT_FOUND`, 401 계열.
     */
    suspend fun sendMessage(roomId: Long, content: String): Result<ChatMessage>

    /**
     * 이 방을 '여기까지 읽음'으로 표시한다. (`POST /api/chat-rooms/{roomId}/read`)
     *
     * 보낼 것도 받을 것도 없다 — 요청 body 가 없고(messageId 를 지정하지 않는다),
     * 성공 응답에도 `data` 키가 없다. 서버가 내 읽음 지점을 그 방의 **최신 메시지까지
     * 단조 전진**시킨다(부분 읽음 개념 없음). 메시지가 없는 방이면 no-op.
     *
     * 효과는 [getRooms] 의 `unreadCount` 가 0 이 되는 것뿐이므로,
     * 방 진입 시 1회 + 새 메시지를 받아 화면에 그린 뒤 1회 호출하면 충분하다.
     *
     * 실패: 404 `CHAT_ROOM_NOT_FOUND`, 403 `CHAT_ACCESS_DENIED`, 401 계열.
     * 배지 정리는 부가 기능이므로 **실패해도 화면을 막지 말고 조용히 무시**해도 된다.
     */
    suspend fun markAsRead(roomId: Long): Result<Unit>
}
