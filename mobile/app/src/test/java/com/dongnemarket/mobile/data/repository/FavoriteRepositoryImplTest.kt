package com.dongnemarket.mobile.data.repository

import app.cash.turbine.test
import com.dongnemarket.mobile.data.remote.FavoriteApiService
import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.FavoriteProductRefDto
import com.dongnemarket.mobile.data.remote.dto.MyFavoriteResponseDto
import com.dongnemarket.mobile.domain.model.AppError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * ## FavoriteRepositoryImpl 명세
 *
 * 이 Repository 는 "요청을 대신 보내주는 창구" 가 아니라 **상태(찜 id 집합)를 들고 있는 창구** 다.
 * 상품 상세 응답에 "내가 찜했는지" 가 구조적으로 없어서, 하트 on/off 의 유일한 근거가 이 캐시다.
 * → 그래서 이 파일의 모든 테스트는 **호출 후 [FavoriteRepositoryImpl.favoriteProductIds] 가
 * 어떤 집합이 되는가** 를 본다. "API 가 불렸는가" 는 관심사가 아니다.
 *
 * 서버 실패는 진짜 [HttpException] 으로 재현한다. 409/404 를 성공으로 흡수하는 규칙이
 * **HTTP 상태 + 에러 코드** 두 가지를 함께 보기 때문에, 에러 본문 파싱까지 통과시켜야 의미가 있다.
 */
class FavoriteRepositoryImplTest {

    private val api: FavoriteApiService = mockk()
    private val repository = FavoriteRepositoryImpl(api)

    // ── 1. refreshFavorites: 서버 목록으로 캐시를 통째로 채운다 ────────────────────

    @Test
    fun `찜 목록을 새로고침하면 productId 집합이 캐시에 담긴다`() = runTest {
        // Given: 서버가 찜 2건을 준다(우리가 쓰는 값은 product.productId 뿐이다)
        coEvery { api.getMyFavorites() } returns ApiEnvelope(
            status = 200,
            data = listOf(favoriteOf(favoriteId = 7, productId = 11), favoriteOf(8, 12)),
        )

        // When
        repository.refreshFavorites()

        // Then
        assertEquals(setOf(11L, 12L), repository.favoriteProductIds.value)
    }

    @Test
    fun `새로고침은 캐시를 병합하지 않고 통째로 교체한다`() = runTest {
        // Given: 이미 11, 12 를 찜한 상태로 캐시가 채워져 있다
        coEvery { api.getMyFavorites() } returns ApiEnvelope(
            status = 200,
            data = listOf(favoriteOf(7, 11), favoriteOf(8, 12)),
        )
        repository.refreshFavorites()

        // When: 다른 기기에서 11 번 찜을 취소한 뒤 다시 새로고침한다
        coEvery { api.getMyFavorites() } returns ApiEnvelope(
            status = 200,
            data = listOf(favoriteOf(8, 12), favoriteOf(9, 13)),
        )
        repository.refreshFavorites()

        // Then: 서버가 정답이므로 11 은 사라져야 한다(병합이면 남는다)
        assertEquals(setOf(12L, 13L), repository.favoriteProductIds.value)
    }

    @Test
    fun `새로고침이 401 로 실패하면 캐시를 비운다`() = runTest {
        // Given: 로그인 상태에서 찜을 받아 둔 뒤 토큰이 만료됐다
        coEvery { api.getMyFavorites() } returns ApiEnvelope(
            status = 200,
            data = listOf(favoriteOf(7, 11)),
        )
        repository.refreshFavorites()
        coEvery { api.getMyFavorites() } throws serverError(401, "INVALID_TOKEN", "다시 로그인해 주세요.")

        // When
        repository.refreshFavorites()

        // Then: 익명 사용자에게 찜이 있을 수 없다
        assertEquals(emptySet<Long>(), repository.favoriteProductIds.value)
    }

    @Test
    fun `새로고침이 네트워크 오류로 실패하면 캐시를 그대로 유지한다`() = runTest {
        // Given: 찜을 받아 둔 뒤 지하철에서 연결이 끊겼다
        coEvery { api.getMyFavorites() } returns ApiEnvelope(
            status = 200,
            data = listOf(favoriteOf(7, 11)),
        )
        repository.refreshFavorites()
        coEvery { api.getMyFavorites() } throws IOException("연결 실패")

        // When
        repository.refreshFavorites()

        // Then: 잠깐 끊겼다고 하트가 전부 꺼지면 안 된다
        assertEquals(setOf(11L), repository.favoriteProductIds.value)
    }

    // ── 2. addFavorite / removeFavorite: 낙관적 하트의 근거가 되는 상태 전이 ───────

    @Test
    fun `찜에 성공하면 집합에 productId 가 추가된다`() = runTest {
        // Given: 아무것도 찜하지 않은 상태
        coEvery { api.addFavorite(42L) } returns ApiEnvelope(status = 201)

        // When / Then: 빈 집합 → {42} 로 한 번 전이한다
        repository.favoriteProductIds.test {
            assertEquals(emptySet<Long>(), awaitItem())

            repository.addFavorite(productId = 42L)

            assertEquals(setOf(42L), awaitItem())
        }
    }

    @Test
    fun `찜 취소에 성공하면 집합에서 productId 가 제거된다`() = runTest {
        // Given: 42 번을 찜한 상태
        coEvery { api.getMyFavorites() } returns ApiEnvelope(
            status = 200,
            data = listOf(favoriteOf(favoriteId = 7, productId = 42)),
        )
        repository.refreshFavorites()
        coEvery { api.removeFavorite(42L) } returns ApiEnvelope(status = 200)

        // When / Then: {42} → 빈 집합
        repository.favoriteProductIds.test {
            assertEquals(setOf(42L), awaitItem())

            repository.removeFavorite(productId = 42L)

            assertEquals(emptySet<Long>(), awaitItem())
        }
    }

    @Test
    fun `찜 등록이 네트워크 오류로 실패하면 집합에 넣지 않는다`() = runTest {
        // Given
        coEvery { api.addFavorite(42L) } throws IOException("연결 실패")

        // When
        repository.addFavorite(productId = 42L)

        // Then: 실패한 찜을 켜 두면 새로고침 때 하트가 꺼지며 깜빡인다
        assertEquals(emptySet<Long>(), repository.favoriteProductIds.value)
    }

    @Test
    fun `찜 등록이 네트워크 오류로 실패하면 실패를 그대로 돌려준다`() = runTest {
        // Given
        coEvery { api.addFavorite(42L) } throws IOException("연결 실패")

        // When
        val result = repository.addFavorite(productId = 42L)

        // Then: AppError_Api 가 아닌 실패는 절대 흡수하지 않는다
        assertTrue(result.exceptionOrNull() is AppError.Network)
    }

    // ── 3. 자기치유: 이미 도달한 상태를 알리는 실패는 성공으로 흡수한다 ────────────

    @Test
    fun `이미 찜한 상품이라 409 가 와도 성공으로 처리한다`() = runTest {
        // Given: 서버 목록이 최근 200건뿐이라 캐시에 없던 찜을 다시 누른 상황
        coEvery { api.addFavorite(42L) } throws serverError(
            status = 409,
            errorCode = "FAVORITE_ALREADY_EXISTS",
            message = "이미 찜한 상품입니다.",
        )

        // When
        val result = repository.addFavorite(productId = 42L)

        // Then: 우리가 원한 최종 상태(찜됨)에 이미 도달했으므로 실패가 아니다
        assertTrue(result.isSuccess)
    }

    @Test
    fun `이미 찜한 상품이라 409 가 와도 집합에는 남는다`() = runTest {
        // Given
        coEvery { api.addFavorite(42L) } throws serverError(
            status = 409,
            errorCode = "FAVORITE_ALREADY_EXISTS",
            message = "이미 찜한 상품입니다.",
        )

        // When
        repository.addFavorite(productId = 42L)

        // Then: 캐시가 스스로 치유돼 하트가 켜진 채로 유지된다
        assertEquals(setOf(42L), repository.favoriteProductIds.value)
    }

    @Test
    fun `찜이 이미 없어 404 FAVORITE_NOT_FOUND 가 와도 성공으로 처리한다`() = runTest {
        // Given
        coEvery { api.removeFavorite(42L) } throws serverError(
            status = 404,
            errorCode = "FAVORITE_NOT_FOUND",
            message = "찜하지 않은 상품입니다.",
        )

        // When
        val result = repository.removeFavorite(productId = 42L)

        // Then: 원한 최종 상태(찜 없음)에 이미 도달했다
        assertTrue(result.isSuccess)
    }

    // ── 4. 흡수해서는 안 되는 404: 경로 오타(스프링 기본 에러 바디) ────────────────

    @Test
    fun `경로 오타로 나는 404 는 흡수하지 않고 실패로 남긴다`() = runTest {
        // Given: 우리가 URL 을 틀렸을 때 스프링이 주는 기본 바디(error 가 "Not Found" 다)
        coEvery { api.removeFavorite(42L) } throws springDefaultNotFound(
            path = "api/products/42/favorite",
        )

        // When
        val result = repository.removeFavorite(productId = 42L)

        // Then: 상태 코드만 보고 흡수했다면 우리 버그가 "정상 동작" 으로 조용히 묻힌다
        assertTrue(result.isFailure)
    }

    @Test
    fun `경로 오타로 나는 404 는 캐시를 건드리지 않는다`() = runTest {
        // Given: 42 번이 찜된 상태에서 잘못된 경로로 취소를 시도한다
        coEvery { api.getMyFavorites() } returns ApiEnvelope(
            status = 200,
            data = listOf(favoriteOf(favoriteId = 7, productId = 42)),
        )
        repository.refreshFavorites()
        coEvery { api.removeFavorite(42L) } throws springDefaultNotFound("api/products/42/favorite")

        // When
        repository.removeFavorite(productId = 42L)

        // Then: 서버에는 찜이 그대로 남아 있으므로 캐시도 남아 있어야 한다
        assertEquals(setOf(42L), repository.favoriteProductIds.value)
    }

    @Test
    fun `찜 취소가 403 으로 실패하면 흡수하지 않는다`() = runTest {
        // Given: 404 만 흡수 대상이라는 사실을 못 박아 둔다
        coEvery { api.removeFavorite(42L) } throws serverError(403, "ACCESS_DENIED", "권한이 없습니다.")

        // When
        val result = repository.removeFavorite(productId = 42L)

        // Then
        assertTrue(result.isFailure)
    }

    // ── 테스트 지원 ────────────────────────────────────────────────────────────────

    /** `GET /api/members/me/favorites` 응답 원소 하나. */
    private fun favoriteOf(favoriteId: Long, productId: Long) = MyFavoriteResponseDto(
        favoriteId = favoriteId,
        createdAt = "2026-07-26T13:45:30.123",
        product = FavoriteProductRefDto(productId = productId),
    )

    /** 백엔드 `ErrorResponse` 본문을 실은 진짜 [HttpException]. */
    private fun serverError(status: Int, errorCode: String?, message: String): HttpException {
        val errorField = errorCode?.let { "\"$it\"" } ?: "null"
        val body = """
            {"status":$status,"error":$errorField,"message":"$message","timestamp":"2026-07-27T10:00:00"}
        """.trimIndent().toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(status, body))
    }

    /**
     * 경로를 틀렸을 때 스프링이 돌려주는 **기본** 404 바디.
     * 우리 ErrorCode 가 아니라 `"Not Found"` 가 `error` 에 들어오고 `message` 키가 없다.
     */
    private fun springDefaultNotFound(path: String): HttpException {
        val body = """
            {"timestamp":"2026-07-27T10:00:00.000+00:00","status":404,"error":"Not Found","path":"/$path"}
        """.trimIndent().toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(404, body))
    }
}
