package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.local.TokenDataStore
import com.dongnemarket.mobile.data.repository.ChatRepositoryImpl
import com.dongnemarket.mobile.di.NetworkModule
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.model.TradeStatus
import com.dongnemarket.mobile.domain.repository.ChatRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.math.BigDecimal

/**
 * 채팅 API **계약 테스트**. 목을 쓰지 않고 진짜 Retrofit + kotlinx.serialization 을
 * 가짜 서버([MockWebServer])에 붙여, 계약 문서 §4-2~§4-5·§5.6 의 **실제 JSON** 으로
 * 다음을 검증한다.
 *
 *  - 상품 래퍼와 **키 이름이 다른 지점**(`items` vs `messages`, `opponent` vs `seller`)
 *  - 서버가 **최신순(id DESC)** 으로 주는 메시지를 Data 계층이 시간 오름차순으로 뒤집는지
 *  - 대화 없는 방(`lastMessage: null`), 본문 없는 POST(`read`), `data` 키 없는 성공
 *  - 403/401 이 각각 올바른 [AppError] 로 번역되는지
 *
 * Json·OkHttp 조립은 제품 코드([NetworkModule])를 그대로 호출한다. 테스트가 자기만의 Json 을
 * 만들면 정작 앱이 쓰는 설정(`coerceInputValues` 등)이 검증되지 않은 채 남는다.
 */
class ChatApiContractTest {

    private lateinit var server: MockWebServer

    /** 구현체를 그대로 쓰되 타입은 인터페이스로 둔다(화면이 보는 계약과 같은 시선). */
    private lateinit var repository: ChatRepository

    /** 가짜 서버를 띄우고 실제 Retrofit 배관을 연결한다. 채팅 API 는 전부 인증 필수라 토큰도 심는다. */
    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        // DataStore 는 Android 의존이라 JVM 에서 못 쓴다 → 토큰 흐름만 목으로 대체한다.
        val tokenDataStore = mockk<TokenDataStore>()
        every { tokenDataStore.accessToken } returns flowOf("test-access-token")

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(NetworkModule.provideOkHttpClient(AuthInterceptor(tokenDataStore)))
            .addConverterFactory(
                NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()),
            )
            .build()

        repository = ChatRepositoryImpl(retrofit.create(ChatApiService::class.java))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ────────────────────────────────────────────────────────────────
    // 메시지 조회 — 래퍼 키(messages)와 정렬 뒤집기
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `메시지 페이지의 아이템 키는 items 가 아니라 messages 다`() = runTest {
        // Given: 메시지 3건이 messages 키로 온다
        server.enqueue(성공응답(메시지_첫페이지_JSON))

        // When
        val 페이지 = repository.getMessages(roomId = 12L).getOrThrow()

        // Then
        assertEquals(3, 페이지.messages.size)
    }

    @Test
    fun `상품 래퍼 키 items 로 온 메시지 응답은 조용히 비지 않고 실패한다`() = runTest {
        // Given: 상품 페이지 래퍼와 모양만 같고 키가 items 인 응답
        server.enqueue(성공응답(잘못된_래퍼_메시지_JSON))

        // When
        val 결과 = repository.getMessages(roomId = 12L)

        // Then: 두 래퍼를 공용 제네릭으로 묶으면 여기서 빈 목록이 되어 조용히 넘어간다.
        //       분리 선언 덕분에 파싱이 실패하고 원인이 드러난다.
        assertTrue(결과.exceptionOrNull() is AppError.Unknown)
    }

    @Test
    fun `id DESC 로 온 메시지는 오래된 것부터 최신 순으로 뒤집혀 담긴다`() = runTest {
        // Given: 서버는 messages[0] 이 가장 최근(103, 102, 101)
        server.enqueue(성공응답(메시지_첫페이지_JSON))

        // When
        val 페이지 = repository.getMessages(roomId = 12L).getOrThrow()

        // Then: 채팅 화면은 위에서 아래로 시간이 흐른다 → 101, 102, 103
        assertEquals(listOf(101L, 102L, 103L), 페이지.messages.map { it.messageId })
    }

    @Test
    fun `nextCursor 는 뒤집기와 무관하게 이 페이지에서 가장 오래된 messageId 다`() = runTest {
        // Given
        server.enqueue(성공응답(메시지_첫페이지_JSON))

        // When
        val 페이지 = repository.getMessages(roomId = 12L).getOrThrow()

        // Then: 커서는 여전히 '과거 방향'을 가리킨다
        assertEquals(101L, 페이지.nextCursor)
    }

    @Test
    fun `메시지 첫 페이지 요청에는 cursor 파라미터가 아예 붙지 않는다`() = runTest {
        // Given
        server.enqueue(성공응답(메시지_첫페이지_JSON))

        // When: 폴링은 항상 커서 없이 최신 첫 페이지를 다시 받는다
        repository.getMessages(roomId = 12L, cursor = null)

        // Then: 숫자가 아닌 값이 실려 나가면 400 이 아니라 500 이 온다(계약 §4-3)
        val 요청 = server.takeRequest()
        assertEquals(setOf("size"), 요청.requestUrl!!.queryParameterNames)
    }

    @Test
    fun `과거 메시지를 더 볼 때는 cursor 가 붙는다`() = runTest {
        // Given
        server.enqueue(성공응답(메시지_첫페이지_JSON))

        // When
        repository.getMessages(roomId = 12L, cursor = 101L)

        // Then: 서버는 id < 101 인 더 오래된 메시지를 준다
        assertEquals("101", server.takeRequest().requestUrl!!.queryParameter("cursor"))
    }

    @Test
    fun `메시지 조회 요청 경로에 roomId 가 박혀 나간다`() = runTest {
        // Given
        server.enqueue(성공응답(메시지_첫페이지_JSON))

        // When
        repository.getMessages(roomId = 12L)

        // Then
        val 요청 = server.takeRequest()
        assertEquals("/api/chat-rooms/12/messages", 요청.requestUrl!!.encodedPath)
    }

    // ────────────────────────────────────────────────────────────────
    // 채팅방 목록 — 대화 없는 방 / 중첩 객체 / 알 수 없는 enum
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `대화가 없는 채팅방은 lastMessage 가 null 로 와도 크래시하지 않는다`() = runTest {
        // Given: 갓 만든 빈 방은 키가 남은 채 값만 null 이다
        server.enqueue(성공응답(채팅방_목록_JSON))

        // When
        val 방목록 = repository.getRooms().getOrThrow()

        // Then
        assertNull(방목록[1].lastMessage)
    }

    @Test
    fun `대화가 없는 방의 표시 시각은 방 생성 시각으로 대체된다`() = runTest {
        // Given
        server.enqueue(성공응답(채팅방_목록_JSON))

        // When
        val 빈방 = repository.getRooms().getOrThrow()[1]

        // Then: 시간 칸이 비지 않는다(서버 정렬 기준과 같은 규칙)
        assertEquals("2026-07-26T09:00:00", 빈방.displayTimeRaw)
    }

    @Test
    fun `중첩된 opponent 객체는 도메인에서 평평하게 펼쳐진다`() = runTest {
        // Given: 목록 응답의 상대방 키는 seller 가 아니라 opponent 다
        server.enqueue(성공응답(채팅방_목록_JSON))

        // When
        val 첫방 = repository.getRooms().getOrThrow().first()

        // Then: 화면이 room.opponent.nickname 같은 점 사슬을 타지 않는다
        assertEquals("동네주민", 첫방.opponentNickname)
    }

    @Test
    fun `중첩된 product 의 가격도 BigDecimal 로 파싱되어 펼쳐진다`() = runTest {
        // Given: price 800000.00
        server.enqueue(성공응답(채팅방_목록_JSON))

        // When
        val 첫방 = repository.getRooms().getOrThrow().first()

        // Then
        assertEquals(0, BigDecimal("800000").compareTo(첫방.productPrice))
    }

    @Test
    fun `앱이 모르는 tradeStatus 가 와도 채팅 목록은 UNKNOWN 으로 강등되어 살아남는다`() = runTest {
        // Given: 채팅 DTO 는 문자열이 아니라 enum 으로 직접 받는다
        //        → Json 의 coerceInputValues 가 모르는 값을 default(UNKNOWN)로 떨어뜨려야 한다
        server.enqueue(성공응답(모르는_상태값_채팅방_목록_JSON))

        // When
        val 첫방 = repository.getRooms().getOrThrow().first()

        // Then
        assertEquals(TradeStatus.UNKNOWN, 첫방.productTradeStatus)
    }

    // ────────────────────────────────────────────────────────────────
    // 방 생성 / 전송 — 요청 본문과 응답 키 비대칭
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `방 생성 요청 본문은 productId 한 필드다`() = runTest {
        // Given
        server.enqueue(성공응답(방_생성_JSON))

        // When
        repository.createRoom(productId = 128L)

        // Then: 상대 memberId 는 서버가 상품에서 파생한다 → 보내지 않는다
        assertEquals("""{"productId":128}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `방 생성 응답의 상대방 키가 seller 여도 정상 파싱되어 roomId 를 준다`() = runTest {
        // Given: 목록은 opponent, 생성 응답은 seller 다(한 DTO 로 받으면 조용히 null 이 된다)
        server.enqueue(성공응답(방_생성_JSON))

        // When
        val roomId = repository.createRoom(productId = 128L).getOrThrow()

        // Then
        assertEquals(7L, roomId)
    }

    @Test
    fun `메시지 전송 요청 본문은 content 한 필드다`() = runTest {
        // Given
        server.enqueue(성공응답(전송_결과_JSON, code = 201))

        // When
        repository.sendMessage(roomId = 12L, content = "안녕하세요")

        // Then: 이미지·메시지 타입 개념이 없다
        assertEquals("""{"content":"안녕하세요"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `전송 201 응답의 저장된 메시지가 재조회 없이 그대로 도메인으로 온다`() = runTest {
        // Given: 성공 코드가 200 이 아니라 201 이다
        server.enqueue(성공응답(전송_결과_JSON, code = 201))

        // When
        val 메시지 = repository.sendMessage(roomId = 12L, content = "안녕하세요").getOrThrow()

        // Then: 폴링 중복 제거 기준이 되는 messageId 가 실려 온다
        assertEquals(304L, 메시지.messageId)
    }

    // ────────────────────────────────────────────────────────────────
    // 읽음 처리 — 본문 없는 POST, data 키 없는 성공
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `읽음 처리 요청은 본문이 비어 있다`() = runTest {
        // Given: messageId 를 보내지 않는다. 서버가 최신 메시지까지 읽음 지점을 전진시킨다
        server.enqueue(성공응답(읽음처리_JSON))

        // When
        repository.markAsRead(roomId = 12L)

        // Then
        assertEquals(0L, server.takeRequest().body.size)
    }

    @Test
    fun `읽음 처리 응답에 data 키가 아예 없어도 성공으로 처리된다`() = runTest {
        // Given: {"status":200,"message":"..."} — data 키 자체가 사라진다(계약 §7-3)
        server.enqueue(성공응답(읽음처리_JSON))

        // When
        val 결과 = repository.markAsRead(roomId = 12L)

        // Then: apiCall 로 감쌌다면 EmptyBody 실패로 오판했을 자리다
        assertTrue(결과.isSuccess)
    }

    // ────────────────────────────────────────────────────────────────
    // 인증 헤더 / 에러 번역
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `채팅 요청에는 저장된 토큰이 Authorization 헤더로 붙는다`() = runTest {
        // Given: 채팅 API 는 permitAll 경로가 하나도 없다
        server.enqueue(성공응답(채팅방_목록_JSON))

        // When
        repository.getRooms()

        // Then
        assertEquals("Bearer test-access-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `참여자가 아닌 방은 403 CHAT_ACCESS_DENIED 로 번역된다`() = runTest {
        // Given: 404 가 아니라 403 이다(계약 §7-17)
        server.enqueue(에러응답(403, 접근거부_에러_JSON))

        // When
        val 결과 = repository.getMessages(roomId = 99L)

        // Then
        val 오류 = 결과.exceptionOrNull() as AppError.Api
        assertEquals("CHAT_ACCESS_DENIED", 오류.code)
    }

    @Test
    fun `403 응답의 서버 문구가 사용자 문구로 그대로 전달된다`() = runTest {
        // Given
        server.enqueue(에러응답(403, 접근거부_에러_JSON))

        // When
        val 결과 = repository.getMessages(roomId = 99L)

        // Then: UI 는 error 코드가 아니라 이 문장만 읽어서 띄운다
        assertEquals(
            "채팅방에 접근할 권한이 없습니다.",
            (결과.exceptionOrNull() as AppError).userMessage,
        )
    }

    @Test
    fun `토큰이 만료된 401 은 로그인 화면 신호인 Unauthorized 로 번역된다`() = runTest {
        // Given: 채팅은 익명 통과가 없어 만료 토큰이면 정직하게 401 이 온다
        server.enqueue(에러응답(401, 토큰만료_에러_JSON))

        // When
        val 결과 = repository.getRooms()

        // Then: 403 과 달리 Api 가 아니라 Unauthorized 로 갈라져야 화면이 로그인으로 보낼 수 있다
        assertTrue(결과.exceptionOrNull() is AppError.Unauthorized)
    }

    @Test
    fun `없는 방을 열면 404 CHAT_ROOM_NOT_FOUND 로 번역된다`() = runTest {
        // Given
        server.enqueue(에러응답(404, 방없음_에러_JSON))

        // When
        val 결과 = repository.markAsRead(roomId = 99L)

        // Then: markAsRead 도 apiCallForUnit 을 통해 같은 번역을 거친다
        assertEquals("CHAT_ROOM_NOT_FOUND", (결과.exceptionOrNull() as AppError.Api).code)
    }

    // ────────────────────────────────────────────────────────────────
    // 응답 픽스처 — 계약 문서 §4-2~§4-5·§5.6 의 실제 JSON
    // ────────────────────────────────────────────────────────────────

    private fun 성공응답(body: String, code: Int = 200): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)

    private fun 에러응답(code: Int, body: String): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)

    /** 최신순(id DESC). `messages[0]` 이 가장 최근이다. */
    private val 메시지_첫페이지_JSON = """
        {
          "status": 200,
          "message": "요청이 성공적으로 처리되었습니다.",
          "data": {
            "messages": [
              { "messageId": 103, "senderId": 7, "content": "네 좋아요", "createdAt": "2026-07-26T13:47:10" },
              { "messageId": 102, "senderId": 4, "content": "3시에 뵐까요?", "createdAt": "2026-07-26T13:46:02.5" },
              { "messageId": 101, "senderId": 7, "content": "안녕하세요", "createdAt": "2026-07-26T13:45:30.123456" }
            ],
            "nextCursor": 101,
            "hasNext": true
          }
        }
    """.trimIndent()

    /** 상품 페이지 래퍼(`items`)를 그대로 흉내 낸 응답. 채팅 DTO 로는 파싱되면 안 된다. */
    private val 잘못된_래퍼_메시지_JSON = """
        {
          "status": 200,
          "message": "요청이 성공적으로 처리되었습니다.",
          "data": {
            "items": [
              { "messageId": 101, "senderId": 7, "content": "안녕하세요", "createdAt": "2026-07-26T13:45:30" }
            ],
            "nextCursor": null,
            "hasNext": false
          }
        }
    """.trimIndent()

    private val 채팅방_목록_JSON = """
        {
          "status": 200,
          "message": "요청이 성공적으로 처리되었습니다.",
          "data": [
            {
              "roomId": 12,
              "product": {
                "productId": 128,
                "title": "아이폰 15 프로 256GB",
                "price": 800000.00,
                "tradeStatus": "ON_SALE",
                "thumbnailUrl": null
              },
              "opponent": { "memberId": 7, "nickname": "동네주민" },
              "createdAt": "2026-07-26T10:00:00",
              "lastMessage": {
                "messageId": 301,
                "senderId": 7,
                "content": "네고 가능할까요?",
                "createdAt": "2026-07-26T13:45:30.123456"
              },
              "unreadCount": 2
            },
            {
              "roomId": 13,
              "product": {
                "productId": 126,
                "title": "아이폰 케이스 나눔",
                "price": 0,
                "tradeStatus": "ON_SALE",
                "thumbnailUrl": null
              },
              "opponent": { "memberId": 9, "nickname": "탈퇴한 사용자" },
              "createdAt": "2026-07-26T09:00:00",
              "lastMessage": null,
              "unreadCount": 0
            }
          ]
        }
    """.trimIndent()

    private val 모르는_상태값_채팅방_목록_JSON = """
        {
          "status": 200,
          "message": "요청이 성공적으로 처리되었습니다.",
          "data": [
            {
              "roomId": 14,
              "product": {
                "productId": 200,
                "title": "신고 누적 상품",
                "price": 10000.00,
                "tradeStatus": "BLOCKED",
                "thumbnailUrl": null
              },
              "opponent": { "memberId": 7, "nickname": "동네주민" },
              "createdAt": "2026-07-26T08:00:00",
              "lastMessage": null,
              "unreadCount": 0
            }
          ]
        }
    """.trimIndent()

    private val 방_생성_JSON = """
        {
          "status": 200,
          "message": "요청이 성공적으로 처리되었습니다.",
          "data": {
            "roomId": 7,
            "product": {
              "productId": 128,
              "title": "아이폰 15 프로 256GB",
              "description": "1년 사용했고 상태 좋습니다.",
              "price": 800000.00,
              "tradeStatus": "ON_SALE",
              "region": "서울 강남구",
              "thumbnailUrl": null
            },
            "seller": { "memberId": 7, "nickname": "동네주민" }
          }
        }
    """.trimIndent()

    private val 전송_결과_JSON = """
        {
          "status": 201,
          "message": "메시지가 전송되었습니다.",
          "data": {
            "messageId": 304,
            "senderId": 4,
            "content": "안녕하세요",
            "createdAt": "2026-07-26T13:50:00"
          }
        }
    """.trimIndent()

    /** `data` 키가 통째로 없다(`ApiResponse` 에 `@JsonInclude(NON_NULL)`). */
    private val 읽음처리_JSON = """
        { "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
    """.trimIndent()

    private val 접근거부_에러_JSON = """
        {
          "status": 403,
          "error": "CHAT_ACCESS_DENIED",
          "message": "채팅방에 접근할 권한이 없습니다.",
          "timestamp": "2026-07-26T13:45:30.123456"
        }
    """.trimIndent()

    private val 방없음_에러_JSON = """
        {
          "status": 404,
          "error": "CHAT_ROOM_NOT_FOUND",
          "message": "채팅방을 찾을 수 없습니다.",
          "timestamp": "2026-07-26T13:45:30.123456"
        }
    """.trimIndent()

    private val 토큰만료_에러_JSON = """
        {
          "status": 401,
          "error": "INVALID_TOKEN",
          "message": "유효하지 않은 토큰입니다.",
          "timestamp": "2026-07-26T13:45:30.123456"
        }
    """.trimIndent()
}
