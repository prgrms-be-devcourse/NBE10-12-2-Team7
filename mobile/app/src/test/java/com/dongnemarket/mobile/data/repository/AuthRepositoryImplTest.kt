package com.dongnemarket.mobile.data.repository

import com.dongnemarket.mobile.data.local.TokenDataStore
import com.dongnemarket.mobile.data.remote.AuthApiService
import com.dongnemarket.mobile.data.remote.dto.AccessTokenDto
import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.LoginRequestDto
import com.dongnemarket.mobile.domain.model.AppError
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * ## AuthRepositoryImpl 명세
 *
 * 이 Repository 의 존재 이유는 단 하나다 —
 * **"로그인 = 서버에서 토큰을 받아 기기에 저장하는 것" 이라는 한 덩어리의 일을 여기서 끝내는 것.**
 * 그래서 이 파일이 검증하는 것도 "무엇을 반환했나"보다 **"토큰이 실제로 저장/삭제됐나"** 쪽에 무게가 있다.
 *
 * 협력자 두 개를 목으로 세운다.
 *  - [AuthApiService] : 서버. 실패는 **진짜 [HttpException] 을 던져서** 재현한다
 *    (그래야 `ApiCall.kt` 의 에러 본문 파싱 → [AppError] 번역까지 함께 검증된다).
 *  - [TokenDataStore] : 기기 저장소. DataStore 는 Android Context 가 필요해 단위 테스트에서 실물을 쓸 수 없다.
 */
class AuthRepositoryImplTest {

    private val api: AuthApiService = mockk()
    private val tokenDataStore: TokenDataStore = mockk()

    /*
     * 주의: [AuthRepositoryImpl] 은 **생성자에서** `tokenDataStore.accessToken` 을 구독한다
     * (isLoggedIn 을 map 으로 만든다). 그래서 각 테스트는 인스턴스를 만들기 **전에**
     * `every { tokenDataStore.accessToken } returns ...` 스텁을 먼저 건다.
     */

    // ── 1. 로그인 성공: 토큰 저장이 Repository 안에서 끝난다 ──────────────────────────

    @Test
    fun `로그인에 성공하면 accessToken 이 TokenDataStore 에 저장된다`() = runTest {
        // Given: 서버가 accessToken 을 내려준다
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { tokenDataStore.saveAccessToken(any()) } just Runs
        coEvery { api.login(any()) } returns ApiEnvelope(
            status = 200,
            message = "요청이 성공적으로 처리되었습니다.",
            data = AccessTokenDto(accessToken = "eyJhbGciOiJIUzI1NiJ9.buyer.signature"),
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When: 사용자가 로그인한다
        repository.login(email = "buyer@marketon.com", password = "Password1!")

        // Then: ViewModel 이 아니라 Repository 가 토큰을 저장한다.
        //       이게 깨지면 로그인은 성공했는데 이후 모든 인증 요청이 401 이 된다.
        coVerify(exactly = 1) {
            tokenDataStore.saveAccessToken("eyJhbGciOiJIUzI1NiJ9.buyer.signature")
        }
    }

    // ── 2. 로그인 요청 본문: 무엇을 보내는가 ────────────────────────────────────────

    @Test
    fun `로그인 요청은 autoLogin 을 true 로 보낸다`() = runTest {
        // Given: 서버로 나가는 요청 본문을 잡아 둘 슬롯
        val sentRequest = slot<LoginRequestDto>()
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { tokenDataStore.saveAccessToken(any()) } just Runs
        coEvery { api.login(capture(sentRequest)) } returns ApiEnvelope(
            status = 200,
            data = AccessTokenDto("token"),
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        repository.login(email = "buyer@marketon.com", password = "Password1!")

        // Then: false 로 나가면 서버가 refreshToken 을 Max-Age 없는 세션 쿠키로 내려서
        //       앱을 재시작하는 순간 자동 로그인이 영구히 불가능해진다.
        assertTrue(
            "모바일은 autoLogin 을 항상 true 로 보내야 한다",
            sentRequest.captured.autoLogin,
        )
    }

    @Test
    fun `로그인 요청의 이메일은 앞뒤 공백을 제거해서 보낸다`() = runTest {
        // Given: 키보드 자동완성이 뒤에 공백을 붙인 상황
        val sentRequest = slot<LoginRequestDto>()
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { tokenDataStore.saveAccessToken(any()) } just Runs
        coEvery { api.login(capture(sentRequest)) } returns ApiEnvelope(
            status = 200,
            data = AccessTokenDto("token"),
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        repository.login(email = "  buyer@marketon.com ", password = "Password1!")

        // Then: 공백이 남아 있으면 서버 @Email 검증에서 400 이 난다
        assertEquals("buyer@marketon.com", sentRequest.captured.email)
    }

    @Test
    fun `로그인 요청의 비밀번호는 공백까지 원문 그대로 보낸다`() = runTest {
        // Given: 비밀번호 앞뒤에 공백이 있는 사용자(공백도 유효한 비밀번호 문자다)
        val sentRequest = slot<LoginRequestDto>()
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { tokenDataStore.saveAccessToken(any()) } just Runs
        coEvery { api.login(capture(sentRequest)) } returns ApiEnvelope(
            status = 200,
            data = AccessTokenDto("token"),
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        repository.login(email = "buyer@marketon.com", password = " Password1! ")

        // Then: 여기서 trim 하면 정상 비밀번호가 영원히 틀린 비밀번호가 된다
        assertEquals(" Password1! ", sentRequest.captured.password)
    }

    // ── 3. 로그인 실패: 토큰을 저장하지 않는다 ──────────────────────────────────────

    @Test
    fun `비밀번호가 틀려 401 이 오면 토큰을 저장하지 않는다`() = runTest {
        // Given: 서버가 401 을 준다
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { tokenDataStore.saveAccessToken(any()) } just Runs
        coEvery { api.login(any()) } throws serverError(
            status = 401,
            errorCode = "INVALID_PASSWORD",
            message = "비밀번호가 일치하지 않습니다.",
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        repository.login(email = "buyer@marketon.com", password = "틀린비밀번호")

        // Then
        coVerify(exactly = 0) { tokenDataStore.saveAccessToken(any()) }
    }

    @Test
    fun `로그인 401 실패는 AppError Unauthorized 로 번역된다`() = runTest {
        // Given
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { api.login(any()) } throws serverError(
            status = 401,
            errorCode = "INVALID_PASSWORD",
            message = "비밀번호가 일치하지 않습니다.",
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        val result = repository.login("buyer@marketon.com", "틀린비밀번호")

        // Then: 화면이 "로그인 화면으로 보내기" 를 판단하는 신호다
        assertTrue(result.exceptionOrNull() is AppError.Unauthorized)
    }

    @Test
    fun `로그인 실패 문구는 서버가 준 message 를 그대로 쓴다`() = runTest {
        // Given: 미가입 이메일
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { api.login(any()) } throws serverError(
            status = 404,
            errorCode = "MEMBER_NOT_FOUND",
            message = "가입되지 않은 이메일입니다.",
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        val result = repository.login("nobody@marketon.com", "Password1!")

        // Then: UI 는 userMessage 만 읽으면 되도록 Data 계층에서 번역을 끝낸다
        assertEquals(
            "가입되지 않은 이메일입니다.",
            (result.exceptionOrNull() as AppError).userMessage,
        )
    }

    @Test
    fun `서버가 빈 accessToken 을 주면 저장하지 않는다`() = runTest {
        // Given: 계약상 올 수 없지만, 저장되면 "로그인된 것처럼 보이는데 전부 401" 이 된다
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { tokenDataStore.saveAccessToken(any()) } just Runs
        coEvery { api.login(any()) } returns ApiEnvelope(
            status = 200,
            data = AccessTokenDto(accessToken = "   "),
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        repository.login("buyer@marketon.com", "Password1!")

        // Then
        coVerify(exactly = 0) { tokenDataStore.saveAccessToken(any()) }
    }

    @Test
    fun `서버가 빈 accessToken 을 주면 EmptyBody 실패로 끝낸다`() = runTest {
        // Given
        every { tokenDataStore.accessToken } returns flowOf(null)
        coEvery { api.login(any()) } returns ApiEnvelope(
            status = 200,
            data = AccessTokenDto(accessToken = ""),
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        val result = repository.login("buyer@marketon.com", "Password1!")

        // Then
        assertTrue(result.exceptionOrNull() is AppError.EmptyBody)
    }

    // ── 4. 로그아웃: 서버가 실패해도 로컬 토큰은 반드시 지운다 ──────────────────────

    @Test
    fun `로그아웃은 서버 호출이 401 로 실패해도 로컬 토큰을 지운다`() = runTest {
        // Given: accessToken 이 이미 만료돼 로그아웃 호출 자체가 401 로 튕기는 흔한 상황
        every { tokenDataStore.accessToken } returns flowOf("만료된토큰")
        coEvery { tokenDataStore.clear() } just Runs
        coEvery { api.logout() } throws serverError(
            status = 401,
            errorCode = "INVALID_TOKEN",
            message = "다시 로그인해 주세요.",
        )
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        repository.logout()

        // Then: 로그아웃의 실체는 "기기에서 토큰을 지우는 것" 이다.
        //       안 지우면 사용자는 로그아웃 버튼을 눌러도 로그인 상태에 갇힌다.
        coVerify(exactly = 1) { tokenDataStore.clear() }
    }

    @Test
    fun `로그아웃은 서버 호출이 실패해도 성공을 반환한다`() = runTest {
        // Given: 비행기 모드에서 로그아웃
        every { tokenDataStore.accessToken } returns flowOf("만료된토큰")
        coEvery { tokenDataStore.clear() } just Runs
        coEvery { api.logout() } throws IOException("네트워크 연결 없음")
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        val result = repository.logout()

        // Then: 화면에 실패 토스트를 띄울 이유가 없다 — 이미 로그아웃됐기 때문이다
        assertTrue(result.isSuccess)
    }

    @Test
    fun `로그아웃에 성공하면 로컬 토큰을 지운다`() = runTest {
        // Given
        every { tokenDataStore.accessToken } returns flowOf("살아있는토큰")
        coEvery { tokenDataStore.clear() } just Runs
        coEvery { api.logout() } returns ApiEnvelope(status = 200, message = "로그아웃되었습니다.")
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        repository.logout()

        // Then
        coVerify(exactly = 1) { tokenDataStore.clear() }
    }

    // ── 5. isLoggedIn: 저장된 토큰 유무를 그대로 흘려보낸다 ────────────────────────

    @Test
    fun `저장된 토큰이 있으면 isLoggedIn 이 true 다`() = runTest {
        // Given
        every { tokenDataStore.accessToken } returns flowOf("eyJhbGciOiJIUzI1NiJ9.buyer.signature")
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        val loggedIn = repository.isLoggedIn.first()

        // Then
        assertTrue(loggedIn)
    }

    @Test
    fun `저장된 토큰이 없으면 isLoggedIn 이 false 다`() = runTest {
        // Given: 최초 실행(또는 로그아웃 직후)
        every { tokenDataStore.accessToken } returns flowOf(null)
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        val loggedIn = repository.isLoggedIn.first()

        // Then
        assertFalse(loggedIn)
    }

    @Test
    fun `저장된 토큰이 공백뿐이면 isLoggedIn 이 false 다`() = runTest {
        // Given: 어쩌다 공백이 저장된 경우까지 로그인 상태로 보면 스플래시가 홈으로 보내 버린다
        every { tokenDataStore.accessToken } returns flowOf("   ")
        val repository = AuthRepositoryImpl(api, tokenDataStore)

        // When
        val loggedIn = repository.isLoggedIn.first()

        // Then
        assertFalse(loggedIn)
    }

    // ── 테스트 지원 ────────────────────────────────────────────────────────────────

    /**
     * 백엔드 `ErrorResponse` 본문을 그대로 실은 진짜 [HttpException] 을 만든다.
     * (Retrofit 이 4xx/5xx 에서 던지는 것과 같은 모양 → `ApiCall.kt` 의 번역 로직까지 검증된다.)
     */
    private fun serverError(status: Int, errorCode: String?, message: String): HttpException {
        val errorField = errorCode?.let { "\"$it\"" } ?: "null"
        val body = """
            {"status":$status,"error":$errorField,"message":"$message","timestamp":"2026-07-27T10:00:00"}
        """.trimIndent().toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(status, body))
    }
}
