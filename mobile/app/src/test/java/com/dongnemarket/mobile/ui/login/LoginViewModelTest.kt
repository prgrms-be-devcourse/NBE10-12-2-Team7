package com.dongnemarket.mobile.ui.login

import app.cash.turbine.test
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope` 는 `Dispatchers.Main` 위에서 돈다. JVM 단위 테스트에는 안드로이드 메인 루퍼가
 * 없으므로 갈아 끼우지 않으면 `Module with the Main dispatcher had failed to initialize` 가 난다.
 *
 * `UnconfinedTestDispatcher` 를 쓰는 이유: `launch` 한 코루틴이 **호출 지점에서 즉시** 실행돼
 * "onSubmit() 을 부르면 그 다음 줄에서 결과 상태를 읽을 수 있다"가 성립한다.
 * (공용 파일로 빼면 병렬 작업 중인 다른 테스트와 충돌하므로 이 파일 안에 둔다.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
private class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * 로그인 화면 ViewModel 의 명세.
 *
 * 이 화면이 지켜야 하는 약속은 네 가지다.
 *  1. 사용자가 입력한 값은 그대로 상태에 남는다.
 *  2. 성공하면 `isLoading` 이 켜졌다 꺼지고 `isLoggedIn` 이 켜진다(= 화면이 홈으로 이동한다).
 *  3. 실패하면 `AppError.userMessage`(서버 한국어 문구)만 보여 주고 로그인 상태는 그대로 false 다.
 *  4. **요청은 절대 두 번 나가지 않는다.** 이 API 는 5회 실패 시 10분 잠긴다(429 TOO_MANY_LOGIN_ATTEMPTS).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule: TestWatcher = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    /** 실제로 서버에 보내는 모양의 값. 테스트를 읽는 사람이 "무슨 요청인지" 바로 알 수 있어야 한다. */
    private val 이메일 = "buyer@dongne.com"
    private val 비밀번호 = "Passw0rd!!"

    @Before
    fun setUp() {
        authRepository = mockk()
        viewModel = LoginViewModel(authRepository)
    }

    // ─────────────────────────────────────────────────────────────
    // 1. 입력
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `이메일을 입력하면 그 값이 상태에 반영된다`() {
        // Given: 아무것도 입력하지 않은 초기 화면
        assertEquals("", viewModel.uiState.value.email)

        // When
        viewModel.onEmailChange(이메일)

        // Then
        assertEquals(이메일, viewModel.uiState.value.email)
    }

    @Test
    fun `비밀번호를 입력하면 그 값이 상태에 반영된다`() {
        // Given: 아무것도 입력하지 않은 초기 화면
        assertEquals("", viewModel.uiState.value.password)

        // When
        viewModel.onPasswordChange(비밀번호)

        // Then
        assertEquals(비밀번호, viewModel.uiState.value.password)
    }

    // ─────────────────────────────────────────────────────────────
    // 2. 로그인 성공
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `로그인에 성공하면 isLoading 이 켜졌다 꺼지고 isLoggedIn 이 true 가 된다`() = runTest {
        // Given: 이메일·비밀번호를 입력했고, 서버 응답은 우리가 원하는 순간에 도착시킨다
        //        (응답을 붙잡아 두지 않으면 "로딩 중" 상태를 관찰할 틈이 없다)
        val 서버응답 = CompletableDeferred<Result<Unit>>()
        coEvery { authRepository.login(이메일, 비밀번호) } coAnswers { 서버응답.await() }
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)

        viewModel.uiState.test {
            val 입력완료 = awaitItem()
            assertFalse("아직 누르지 않았으므로 로딩이 아니다", 입력완료.isLoading)

            // When: 로그인 버튼을 누르고 → 서버가 200 을 돌려준다
            viewModel.onSubmit()
            val 요청중 = awaitItem()
            서버응답.complete(Result.success(Unit))
            val 로그인됨 = awaitItem()

            // Then: 스피너가 돌다가(true) 멈추고(false) 성공 신호가 올라온다
            assertTrue("요청을 보내는 동안에는 로딩이어야 한다", 요청중.isLoading)
            assertFalse("응답이 왔으면 로딩이 끝나야 한다", 로그인됨.isLoading)
            assertTrue("성공했으므로 화면이 홈으로 이동해야 한다", 로그인됨.isLoggedIn)
        }
    }

    @Test
    fun `로그인 요청은 이메일 앞뒤 공백만 지우고 비밀번호는 그대로 보낸다`() = runTest {
        // Given: 키보드 자동완성이 이메일 뒤에 공백을 붙였고, 비밀번호에도 공백이 섞여 있다
        coEvery { authRepository.login(any(), any()) } returns Result.success(Unit)
        viewModel.onEmailChange("  $이메일  ")
        viewModel.onPasswordChange("  $비밀번호  ")

        // When
        viewModel.onSubmit()

        // Then: 이메일은 trim, 비밀번호는 공백도 유효 문자이므로 원문 그대로
        coVerify(exactly = 1) { authRepository.login(이메일, "  $비밀번호  ") }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. 로그인 실패
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `비밀번호가 틀리면 서버가 준 한국어 문구가 errorMessage 에 담긴다`() = runTest {
        // Given: 401 INVALID_PASSWORD → Data 계층이 AppError.Unauthorized 로 번역해 준다
        coEvery { authRepository.login(이메일, 비밀번호) } returns
            Result.failure(AppError.Unauthorized("비밀번호가 올바르지 않습니다."))
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)

        // When
        viewModel.onSubmit()

        // Then
        assertEquals("비밀번호가 올바르지 않습니다.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `5회 실패로 계정이 잠기면 429 문구가 그대로 화면 문구가 된다`() = runTest {
        // Given: 429 TOO_MANY_LOGIN_ATTEMPTS (10분 잠금)
        coEvery { authRepository.login(이메일, 비밀번호) } returns Result.failure(
            AppError.Api(
                status = 429,
                code = "TOO_MANY_LOGIN_ATTEMPTS",
                message = "로그인 시도 횟수를 초과했습니다. 10분 후 다시 시도해 주세요.",
            ),
        )
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)

        // When
        viewModel.onSubmit()

        // Then: 백엔드 ErrorCode 이름(TOO_MANY_LOGIN_ATTEMPTS)이 아니라 사람이 읽을 문장이 담긴다
        assertEquals(
            "로그인 시도 횟수를 초과했습니다. 10분 후 다시 시도해 주세요.",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `로그인에 실패하면 isLoggedIn 은 false 로 유지된다`() = runTest {
        // Given: 미가입 이메일 404 MEMBER_NOT_FOUND
        coEvery { authRepository.login(이메일, 비밀번호) } returns
            Result.failure(AppError.Api(404, "MEMBER_NOT_FOUND", "가입되지 않은 이메일입니다."))
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)

        // When
        viewModel.onSubmit()

        // Then: 에러가 떴다고 홈으로 넘어가면 안 된다
        assertFalse(viewModel.uiState.value.isLoggedIn)
    }

    @Test
    fun `로그인에 실패하면 로딩이 풀려서 다시 시도할 수 있다`() = runTest {
        // Given
        coEvery { authRepository.login(이메일, 비밀번호) } returns
            Result.failure(AppError.Network())
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)

        // When
        viewModel.onSubmit()

        // Then: 스피너가 돌아가는 채로 굳어 버리면 버튼이 영원히 비활성이 된다
        assertFalse("실패해도 로딩은 반드시 꺼져야 한다", viewModel.uiState.value.isLoading)
        assertTrue("다시 누를 수 있어야 한다", viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `AppError 가 아닌 예외로 실패하면 스택트레이스 대신 기본 문구를 보여 준다`() = runTest {
        // Given: Repository 계약(항상 AppError) 위반 — 그래도 사용자에게 개발자 용어를 보이면 안 된다
        coEvery { authRepository.login(이메일, 비밀번호) } returns
            Result.failure(IllegalStateException("java.lang.IllegalStateException: boom"))
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)

        // When
        viewModel.onSubmit()

        // Then
        assertEquals(
            "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.",
            viewModel.uiState.value.errorMessage,
        )
    }

    // ─────────────────────────────────────────────────────────────
    // 4. 실패 후 복구 — 고치기 시작하면 에러 문구를 치운다
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `로그인 실패 후 이메일을 고치면 errorMessage 가 지워진다`() = runTest {
        // Given: 오타 난 이메일로 로그인해서 "가입되지 않은 이메일입니다." 를 본 상태
        coEvery { authRepository.login(any(), any()) } returns
            Result.failure(AppError.Api(404, "MEMBER_NOT_FOUND", "가입되지 않은 이메일입니다."))
        viewModel.onEmailChange("buyer@dongne.co")
        viewModel.onPasswordChange(비밀번호)
        viewModel.onSubmit()
        assertEquals("가입되지 않은 이메일입니다.", viewModel.uiState.value.errorMessage)

        // When: 사용자가 이메일을 고치기 시작한다
        viewModel.onEmailChange(이메일)

        // Then: 방금 실패한 것처럼 보이지 않도록 문구가 사라진다
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `로그인 실패 후 비밀번호를 고치면 errorMessage 가 지워진다`() = runTest {
        // Given: 비밀번호를 틀려서 에러 문구가 떠 있는 상태
        coEvery { authRepository.login(any(), any()) } returns
            Result.failure(AppError.Unauthorized("비밀번호가 올바르지 않습니다."))
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange("wrong-password")
        viewModel.onSubmit()
        assertEquals("비밀번호가 올바르지 않습니다.", viewModel.uiState.value.errorMessage)

        // When
        viewModel.onPasswordChange(비밀번호)

        // Then
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `실패 후 다시 로그인하면 이전 에러 문구부터 지우고 요청을 보낸다`() = runTest {
        // Given: 네트워크 오류로 한 번 실패한 뒤, 재시도 응답은 붙잡아 둔다
        coEvery { authRepository.login(이메일, 비밀번호) } returns Result.failure(AppError.Network())
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)
        viewModel.onSubmit()
        assertEquals("네트워크 연결을 확인해 주세요.", viewModel.uiState.value.errorMessage)

        val 재시도응답 = CompletableDeferred<Result<Unit>>()
        coEvery { authRepository.login(이메일, 비밀번호) } coAnswers { 재시도응답.await() }

        // When: 같은 입력 그대로 다시 누른다(응답은 아직 오지 않았다)
        viewModel.onSubmit()

        // Then: 스피너와 옛날 에러 문구가 함께 떠 있으면 안 된다
        assertNull("재시도 중에는 이전 실패 문구가 남아 있으면 안 된다", viewModel.uiState.value.errorMessage)

        재시도응답.complete(Result.success(Unit))
    }

    // ─────────────────────────────────────────────────────────────
    // 5. 중복 요청 차단 — 5회 실패 시 10분 잠기는 API 라 가장 중요하다
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `응답을 기다리는 동안 로그인을 또 눌러도 요청은 한 번만 나간다`() = runTest {
        // Given: 서버가 아직 응답하지 않아 요청이 공중에 떠 있는 상태
        val 서버응답 = CompletableDeferred<Result<Unit>>()
        coEvery { authRepository.login(이메일, 비밀번호) } coAnswers { 서버응답.await() }
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)

        // When: 사용자가 버튼을 연타한다
        viewModel.onSubmit()
        viewModel.onSubmit()
        viewModel.onSubmit()

        // Then: 로그인 시도 횟수가 3으로 세어지면 5회 잠금에 그만큼 빨리 걸린다
        assertTrue("첫 요청은 아직 응답 대기 중이다", viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { authRepository.login(이메일, 비밀번호) }

        서버응답.complete(Result.success(Unit))
    }

    @Test
    fun `로그인에 성공한 뒤 버튼을 다시 눌러도 요청이 나가지 않는다`() = runTest {
        // Given: 이미 성공해서 화면 이동을 기다리는 한두 프레임 사이
        coEvery { authRepository.login(이메일, 비밀번호) } returns Result.success(Unit)
        viewModel.onEmailChange(이메일)
        viewModel.onPasswordChange(비밀번호)
        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.isLoggedIn)

        // When
        viewModel.onSubmit()

        // Then
        coVerify(exactly = 1) { authRepository.login(이메일, 비밀번호) }
    }

    // ─────────────────────────────────────────────────────────────
    // 6. 빈 입력은 전송 자체를 막는다 (canSubmit)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `이메일이 비어 있으면 로그인 요청이 나가지 않는다`() = runTest {
        // Given: 비밀번호만 입력한 상태
        coEvery { authRepository.login(any(), any()) } returns Result.success(Unit)
        viewModel.onPasswordChange(비밀번호)

        // When
        viewModel.onSubmit()

        // Then
        assertFalse(viewModel.uiState.value.canSubmit)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `비밀번호가 비어 있으면 로그인 요청이 나가지 않는다`() = runTest {
        // Given: 이메일만 입력한 상태
        coEvery { authRepository.login(any(), any()) } returns Result.success(Unit)
        viewModel.onEmailChange(이메일)

        // When
        viewModel.onSubmit()

        // Then
        assertFalse(viewModel.uiState.value.canSubmit)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `이메일이 공백뿐이면 로그인 요청이 나가지 않는다`() = runTest {
        // Given: 공백만 친 이메일은 trim 하면 빈 문자열이라 서버 @Email 검증에서 400 이 난다
        coEvery { authRepository.login(any(), any()) } returns Result.success(Unit)
        viewModel.onEmailChange("   ")
        viewModel.onPasswordChange(비밀번호)

        // When
        viewModel.onSubmit()

        // Then
        assertFalse(viewModel.uiState.value.canSubmit)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `빈 입력으로 버튼을 눌러도 로딩 스피너가 돌지 않는다`() = runTest {
        // Given: 아무것도 입력하지 않은 초기 화면
        coEvery { authRepository.login(any(), any()) } returns Result.success(Unit)

        // When: 접근성 도구 등이 비활성 버튼에 이벤트를 보낸다
        viewModel.onSubmit()

        // Then: 보낸 요청이 없으니 스피너도 돌면 안 된다(돌면 영원히 안 꺼진다)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
