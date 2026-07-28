package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.local.TokenDataStore
import com.dongnemarket.mobile.data.repository.FavoriteRepositoryImpl
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.di.NetworkModule
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * favorite(찜) 계약 통합 테스트 — 목 없이 **실제 Retrofit + OkHttp + kotlinx.serialization** 을
 * 가짜 서버에 붙여 "요청이 계약대로 나가고, 응답이 캐시 상태로 올바르게 번역되는가"를 확인한다.
 *
 * 찜 기능의 핵심 난점은 서버가 아니라 **로컬 캐시([FavoriteRepositoryImpl.favoriteProductIds])** 다.
 * 상품 상세 응답에 '내가 찜했는지' 필드가 없어서, 하트 on/off 는 오직 이 집합이 결정한다.
 * 그래서 이 테스트의 절반은 "어떤 응답이 왔을 때 집합이 어떻게 변하는가"를 검증한다.
 *
 * TokenDataStore 만 목이다(실제 구현은 Android DataStore 라 JVM 에서 돌지 않는다).
 * Json 은 NetworkModule.provideJson() 을 직접 호출해 쓴다(사본을 두면 조용히 어긋난다).
 */
class FavoriteApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var favoriteApi: FavoriteApiService
    private lateinit var repository: FavoriteRepositoryImpl

    /** **제품 코드의 Json 을 그대로 쓴다** (사본은 조용히 어긋난다). */
    private val json = NetworkModule.provideJson()

    @Before
    fun `가짜 서버와 실제 네트워크 배관을 세운다`() {
        server = MockWebServer()
        server.start()

        val tokenDataStore = mockk<TokenDataStore>(relaxed = true)
        every { tokenDataStore.accessToken } returns flowOf("test-token")

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor(tokenDataStore))
                    .build(),
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        favoriteApi = retrofit.create(FavoriteApiService::class.java)
        repository = FavoriteRepositoryImpl(favoriteApi)
    }

    @After
    fun `가짜 서버를 내린다`() {
        runCatching { server.shutdown() }
    }

    // ────────────────────────────────────────────────────────────────
    // 1. 나가는 요청 — 경로·메서드·헤더
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `내 찜 목록 조회는 GET 으로 api 슬래시 members 슬래시 me 슬래시 favorites 에 보내진다`() = runTest {
        // Given
        server.enqueue(jsonResponse(200, MY_FAVORITES_SUCCESS_BODY))

        // When
        repository.refreshFavorites()

        // Then: 계약 §3-3 — 쿼리 파라미터 없음(페이징 없이 서버가 최근 200건 하드캡)
        val recorded = server.takeRequest()
        assertEquals("GET /api/members/me/favorites", "${recorded.method} ${recorded.path}")
    }

    @Test
    fun `찜 등록은 POST 로 상품 경로에 보내진다`() = runTest {
        // Given
        server.enqueue(jsonResponse(201, ADD_FAVORITE_SUCCESS_BODY))

        // When
        repository.addFavorite(productId = 12)

        // Then: 계약 §3-4 — POST /api/products/{productId}/favorites
        val recorded = server.takeRequest()
        assertEquals("POST /api/products/12/favorites", "${recorded.method} ${recorded.path}")
    }

    @Test
    fun `찜 등록 요청에는 본문이 없다`() = runTest {
        // Given: 계약 §3-4 — 요청 body 없음(@Body 생략)
        server.enqueue(jsonResponse(201, ADD_FAVORITE_SUCCESS_BODY))

        // When
        repository.addFavorite(productId = 12)

        // Then
        assertEquals("", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `찜 취소는 DELETE 로 같은 경로에 보내진다`() = runTest {
        // Given
        server.enqueue(jsonResponse(200, NO_DATA_SUCCESS_BODY))

        // When
        repository.removeFavorite(productId = 12)

        // Then: 계약 §3-5 — 토글이 아니라 메서드로 등록/취소가 갈린다
        val recorded = server.takeRequest()
        assertEquals("DELETE /api/products/12/favorites", "${recorded.method} ${recorded.path}")
    }

    @Test
    fun `찜 API 요청에는 Authorization Bearer 헤더가 붙는다`() = runTest {
        // Given: 찜 3종은 전부 인증 필수다(§3-3·§3-4·§3-5)
        server.enqueue(jsonResponse(200, MY_FAVORITES_SUCCESS_BODY))

        // When
        repository.refreshFavorites()

        // Then
        assertEquals("Bearer test-token", server.takeRequest().getHeader("Authorization"))
    }

    // ────────────────────────────────────────────────────────────────
    // 2. 응답 파싱 → 캐시 채우기
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `찜 목록 응답이 productId 집합으로 캐시된다`() = runTest {
        // Given: 서버가 찜 2건(상품 12, 34)을 준다
        server.enqueue(jsonResponse(200, MY_FAVORITES_SUCCESS_BODY))

        // When
        repository.refreshFavorites()

        // Then: 하트 on/off 판정에 쓰는 것은 favoriteId 가 아니라 product.productId 다
        assertEquals(setOf(12L, 34L), repository.favoriteProductIds.value)
    }

    @Test
    fun `찜 목록에 앱이 선언하지 않은 필드가 잔뜩 있어도 파싱이 깨지지 않는다`() = runTest {
        // Given: 서버는 title·price·region·tradeStatus·thumbnailUrl·newField 까지 보내지만
        //        앱 DTO 는 favoriteId·createdAt·product.productId 만 선언했다
        server.enqueue(jsonResponse(200, MY_FAVORITES_SUCCESS_BODY))

        // When
        val result = repository.refreshFavorites()

        // Then: ignoreUnknownKeys 가 없으면 여기서 앱 전체 하트가 죽는다
        assertTrue("실제: ${result.exceptionOrNull()}", result.isSuccess)
    }

    @Test
    fun `찜한 상품이 하나도 없으면 캐시가 빈 집합이 된다`() = runTest {
        // Given
        server.enqueue(jsonResponse(200, EMPTY_FAVORITES_BODY))

        // When
        repository.refreshFavorites()

        // Then
        assertEquals(emptySet<Long>(), repository.favoriteProductIds.value)
    }

    @Test
    fun `새로고침은 캐시를 병합하지 않고 서버 목록으로 통째로 교체한다`() = runTest {
        // Given: 이전 새로고침으로 12·34 가 캐시에 있다
        server.enqueue(jsonResponse(200, MY_FAVORITES_SUCCESS_BODY))
        repository.refreshFavorites()
        server.takeRequest()

        // When: 다른 기기에서 34 를 취소해 서버 목록이 12 하나로 줄었다
        server.enqueue(jsonResponse(200, SINGLE_FAVORITE_BODY))
        repository.refreshFavorites()

        // Then: 서버가 정답이므로 34 는 캐시에서 사라져야 한다
        assertEquals(setOf(12L), repository.favoriteProductIds.value)
    }

    @Test
    fun `찜 목록 응답이 깨진 JSON 이면 예외가 아니라 Result 실패로 온다`() = runTest {
        // Given: 앞단 프록시가 HTML 오류 페이지를 끼워 넣은 상황
        server.enqueue(jsonResponse(200, "<html>502 Bad Gateway</html>"))

        // When
        val result = repository.refreshFavorites()

        // Then
        assertTrue(
            "실제: ${result.exceptionOrNull()}",
            result.exceptionOrNull() is AppError.Unknown,
        )
    }

    // ────────────────────────────────────────────────────────────────
    // 3. 캐시 상태 전이 — 등록 / 취소
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `찜 등록에 성공하면 캐시에 productId 가 추가된다`() = runTest {
        // Given
        server.enqueue(jsonResponse(201, ADD_FAVORITE_SUCCESS_BODY))

        // When
        repository.addFavorite(productId = 12)

        // Then
        assertEquals(setOf(12L), repository.favoriteProductIds.value)
    }

    @Test
    fun `찜 등록 성공 응답의 data 는 앱이 쓰지 않으므로 무시하고 성공으로 처리한다`() = runTest {
        // Given: 서버는 201 과 함께 {id, productId, createdAt} 을 주지만 앱이 쓸 값이 없다
        server.enqueue(jsonResponse(201, ADD_FAVORITE_SUCCESS_BODY))

        // When
        val result = repository.addFavorite(productId = 12)

        // Then: ApiEnvelope<Unit> 로 받아 본문 모양이 바뀌어도 성공은 성공으로 남는다
        assertTrue("실제: ${result.exceptionOrNull()}", result.isSuccess)
    }

    @Test
    fun `찜 취소 응답에 data 키가 아예 없어도 성공으로 처리한다`() = runTest {
        // Given: 계약 §3-5 — 성공 200 이고 {"status":200,"message":"..."} 뿐이다
        server.enqueue(jsonResponse(200, NO_DATA_SUCCESS_BODY))

        // When
        val result = repository.removeFavorite(productId = 12)

        // Then: data 없음을 실패로 판정하면 성공한 취소가 실패가 된다
        assertTrue("실제: ${result.exceptionOrNull()}", result.isSuccess)
    }

    @Test
    fun `찜 취소에 성공하면 캐시에서 productId 가 빠진다`() = runTest {
        // Given: 12·34 가 찜된 상태
        server.enqueue(jsonResponse(200, MY_FAVORITES_SUCCESS_BODY))
        repository.refreshFavorites()

        // When: 12 를 취소한다
        server.enqueue(jsonResponse(200, NO_DATA_SUCCESS_BODY))
        repository.removeFavorite(productId = 12)

        // Then: 34 는 남고 12 만 빠진다
        assertEquals(setOf(34L), repository.favoriteProductIds.value)
    }

    // ────────────────────────────────────────────────────────────────
    // 4. "원하는 상태에 이미 도달" 한 에러는 성공으로 흡수한다
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `이미 찜한 상품을 다시 등록하면 409 를 성공으로 흡수한다`() = runTest {
        // Given: 서버 목록이 최근 200건뿐이라 캐시에 없던 찜을 다시 누르면 409 가 온다
        server.enqueue(jsonResponse(409, FAVORITE_ALREADY_EXISTS_BODY))

        // When
        val result = repository.addFavorite(productId = 12)

        // Then: 실패로 올리면 낙관적 UI 의 하트가 켜졌다 꺼지며 깜빡인다
        assertTrue("실제: ${result.exceptionOrNull()}", result.isSuccess)
    }

    @Test
    fun `409 를 흡수한 뒤 캐시에는 찜한 것으로 반영된다`() = runTest {
        // Given
        server.enqueue(jsonResponse(409, FAVORITE_ALREADY_EXISTS_BODY))

        // When
        repository.addFavorite(productId = 12)

        // Then: 서버 기준으로도 찜된 상태이므로 화면이 스스로 치유된다
        assertEquals(setOf(12L), repository.favoriteProductIds.value)
    }

    @Test
    fun `찜하지 않은 상품을 취소하면 404 FAVORITE_NOT_FOUND 를 성공으로 흡수한다`() = runTest {
        // Given
        server.enqueue(jsonResponse(404, FAVORITE_NOT_FOUND_BODY))

        // When
        val result = repository.removeFavorite(productId = 12)

        // Then: 이미 '찜 없음' 이라는 원하는 상태에 도달해 있다
        assertTrue("실제: ${result.exceptionOrNull()}", result.isSuccess)
    }

    @Test
    fun `경로 오타로 난 404 Not Found 는 삼키지 않고 실패로 남긴다`() = runTest {
        // Given: 매핑 없는 URL 의 404 는 스프링 기본 바디라 error 가 "Not Found" 다.
        //        이건 서버 상태가 아니라 우리 버그이므로 성공으로 흡수하면 안 된다.
        server.enqueue(jsonResponse(404, SPRING_DEFAULT_404_BODY))

        // When
        val result = repository.removeFavorite(productId = 12)

        // Then
        assertEquals(404, (result.exceptionOrNull() as? AppError.Api)?.status)
    }

    @Test
    fun `삭제된 상품에 찜을 등록하면 404 는 실패로 남는다`() = runTest {
        // Given: add 가 흡수하는 것은 409 뿐이다
        server.enqueue(jsonResponse(404, PRODUCT_NOT_FOUND_BODY))

        // When
        val result = repository.addFavorite(productId = 999)

        // Then
        assertEquals("PRODUCT_NOT_FOUND", (result.exceptionOrNull() as? AppError.Api)?.code)
    }

    @Test
    fun `등록이 404 로 실패하면 캐시에 productId 를 넣지 않는다`() = runTest {
        // Given
        server.enqueue(jsonResponse(404, PRODUCT_NOT_FOUND_BODY))

        // When
        repository.addFavorite(productId = 999)

        // Then: 실패한 찜이 하트로 켜져 있으면 안 된다
        assertEquals(emptySet<Long>(), repository.favoriteProductIds.value)
    }

    // ────────────────────────────────────────────────────────────────
    // 5. 401 / 네트워크 실패 — 캐시를 비울 때와 지킬 때
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `찜 목록 조회가 401 이면 Unauthorized 로 번역된다`() = runTest {
        // Given
        server.enqueue(jsonResponse(401, UNAUTHORIZED_BODY))

        // When
        val result = repository.refreshFavorites()

        // Then
        assertTrue(
            "실제: ${result.exceptionOrNull()}",
            result.exceptionOrNull() is AppError.Unauthorized,
        )
    }

    @Test
    fun `찜 목록 조회가 401 이면 캐시를 비운다`() = runTest {
        // Given: 12·34 가 캐시에 있는데 토큰이 만료됐다
        server.enqueue(jsonResponse(200, MY_FAVORITES_SUCCESS_BODY))
        repository.refreshFavorites()

        // When
        server.enqueue(jsonResponse(401, UNAUTHORIZED_BODY))
        repository.refreshFavorites()

        // Then: 익명 사용자에게 찜이 있을 수 없다
        assertEquals(emptySet<Long>(), repository.favoriteProductIds.value)
    }

    @Test
    fun `서버에 닿지 못하면 네트워크 실패로 번역된다`() = runTest {
        // Given: 지하철에서 잠깐 끊긴 상황
        server.shutdown()

        // When
        val result = repository.refreshFavorites()

        // Then
        assertTrue(
            "실제: ${result.exceptionOrNull()}",
            result.exceptionOrNull() is AppError.Network,
        )
    }

    @Test
    fun `네트워크 실패에는 찜 캐시를 지우지 않는다`() = runTest {
        // Given: 12·34 가 캐시에 있다
        server.enqueue(jsonResponse(200, MY_FAVORITES_SUCCESS_BODY))
        repository.refreshFavorites()

        // When: 연결이 끊긴 채 새로고침한다
        server.shutdown()
        repository.refreshFavorites()

        // Then: 잠깐 끊겼다고 화면의 하트가 전부 꺼지면 안 된다
        assertEquals(setOf(12L, 34L), repository.favoriteProductIds.value)
    }

    @Test
    fun `찜 취소가 401 이면 캐시에서 지우지 않는다`() = runTest {
        // Given: 12 가 찜된 상태
        server.enqueue(jsonResponse(200, SINGLE_FAVORITE_BODY))
        repository.refreshFavorites()

        // When: 토큰 만료로 취소가 거부된다
        server.enqueue(jsonResponse(401, UNAUTHORIZED_BODY))
        repository.removeFavorite(productId = 12)

        // Then: 서버에는 여전히 찜이 남아 있으므로 하트도 켜져 있어야 한다
        assertEquals(setOf(12L), repository.favoriteProductIds.value)
    }

    // ────────────────────────────────────────────────────────────────
    // 실제 응답 본문 (계약 문서 §0.2·§0.3·§3-3·§3-4·§3-5)
    // ────────────────────────────────────────────────────────────────

    private fun jsonResponse(code: Int, body: String): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setBody(body)

    private companion object {

        /** 서버가 실제로 보내는 전체 필드. 앱 DTO 는 이 중 일부만 선언한다. */
        const val MY_FAVORITES_SUCCESS_BODY = """
            {"status":200,"message":"요청이 성공적으로 처리되었습니다.","data":[
              {"favoriteId":7,"createdAt":"2026-07-26T13:45:30.123",
               "product":{"productId":12,"categoryId":1,"title":"아이패드 프로 11",
                          "price":800000.00,"region":"서울 강남구","tradeStatus":"ON_SALE",
                          "thumbnailUrl":null,"newField":1}},
              {"favoriteId":9,"createdAt":"2026-07-25T09:00:00",
               "product":{"productId":34,"categoryId":2,"title":"에어프라이어",
                          "price":45000.00,"region":"서울 마포구","tradeStatus":"RESERVED",
                          "thumbnailUrl":"/api/products/images/9f3.png","newField":1}}
            ]}
        """

        const val SINGLE_FAVORITE_BODY = """
            {"status":200,"message":"요청이 성공적으로 처리되었습니다.","data":[
              {"favoriteId":7,"createdAt":"2026-07-26T13:45:30.123",
               "product":{"productId":12,"categoryId":1,"title":"아이패드 프로 11",
                          "price":800000.00,"region":"서울 강남구","tradeStatus":"ON_SALE",
                          "thumbnailUrl":null}}
            ]}
        """

        const val EMPTY_FAVORITES_BODY = """
            {"status":200,"message":"요청이 성공적으로 처리되었습니다.","data":[]}
        """

        /** 찜 등록 201 — data 는 오지만 앱이 쓸 값이 하나도 없다. */
        const val ADD_FAVORITE_SUCCESS_BODY = """
            {"status":201,"message":"찜 등록이 완료되었습니다.",
             "data":{"id":15,"productId":12,"createdAt":"2026-07-26T13:45:30.123"}}
        """

        /** 찜 취소 200 — data 키가 통째로 없다(@JsonInclude(NON_NULL)). */
        const val NO_DATA_SUCCESS_BODY = """
            {"status":200,"message":"찜이 취소되었습니다."}
        """

        const val FAVORITE_ALREADY_EXISTS_BODY = """
            {"status":409,"error":"FAVORITE_ALREADY_EXISTS","message":"이미 찜한 상품입니다.",
             "timestamp":"2026-07-26T13:45:30.123456"}
        """

        const val FAVORITE_NOT_FOUND_BODY = """
            {"status":404,"error":"FAVORITE_NOT_FOUND","message":"찜 내역을 찾을 수 없습니다.",
             "timestamp":"2026-07-26T13:45:30.123456"}
        """

        const val PRODUCT_NOT_FOUND_BODY = """
            {"status":404,"error":"PRODUCT_NOT_FOUND","message":"존재하지 않는 상품입니다.",
             "timestamp":"2026-07-26T13:45:30.123456"}
        """

        const val UNAUTHORIZED_BODY = """
            {"status":401,"error":"UNAUTHORIZED","message":"인증이 필요합니다.",
             "timestamp":"2026-07-26T13:45:30.123456"}
        """

        /** 매핑 없는 URL 의 404 — 스프링 부트 기본 바디. error 가 reason phrase 다. */
        const val SPRING_DEFAULT_404_BODY = """
            {"timestamp":"2026-07-26T13:45:30.123+00:00","status":404,"error":"Not Found",
             "path":"/api/products/12/favorites"}
        """
    }
}
