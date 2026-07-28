package com.dongnemarket.mobile.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.repository.AuthRepository
import com.dongnemarket.mobile.ui.theme.MarketOnTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 로그인 화면 Compose UI 테스트 — **사용자가 실제로 하는 조작만** 한다.
 *
 * ## 왜 [LoginScreen] 을 띄우고 [LoginContent] 를 띄우지 않았나
 * 순수 UI 함수인 `LoginContent` 는 `private` 이라 이 소스셋에서 호출할 수 없다.
 * 제품 코드의 가시성을 테스트 편의로 넓히지 않기로 하고, 대신 **Hilt 없이**
 * `LoginScreen(viewModel = LoginViewModel(가짜_저장소))` 로 직접 주입해서 띄운다.
 * 덕분에 `canSubmit` 판정·로딩 전이·에러 문구 변환 같은 **진짜 로직**까지 함께 검증된다.
 *
 * ## 가짜 저장소를 쓰는 이유
 * androidTest 소스셋에는 MockK 가 없다(단위 테스트 전용). 어차피 목을 세우고 그 목이
 * 불렸는지만 보는 테스트는 가치가 없으므로, 손으로 쓴 [FakeAuthRepository] 가
 * **무엇을 받았는지**를 기록하게 해 "화면에 친 글자가 그대로 서버로 나가는가"를 본다.
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 화면이 붙인 testTag (LoginScreen.kt 와 같은 문자열을 쓴다)
    private val emailField = "login_email"
    private val passwordField = "login_password"
    private val passwordToggle = "login_password_toggle"
    private val submitButton = "login_submit"
    private val errorText = "login_error"

    private val validEmail = "buyer@marketon.com"
    private val validPassword = "Passw0rd!!"

    // ──────────────────────────── 1. 화면 구성 ────────────────────────────

    @Test
    fun `화면이_열리면_이메일·비밀번호_입력창과_로그인_버튼이_보인다`() {
        // Given: 아무것도 입력하지 않은 최초 진입
        val repository = FakeAuthRepository()

        // When
        composeTestRule.showLoginScreen(repository)

        // Then: 로그인에 필요한 세 요소가 모두 화면에 있다
        composeTestRule.onNodeWithTag(emailField).assertIsDisplayed()
        composeTestRule.onNodeWithTag(passwordField).assertIsDisplayed()
        composeTestRule.onNodeWithTag(submitButton).assertIsDisplayed()
    }

    // ──────────────────────────── 2. 버튼 활성 조건 ────────────────────────────

    @Test
    fun `이메일과_비밀번호가_모두_비어_있으면_로그인_버튼을_누를_수_없다`() {
        // Given
        composeTestRule.showLoginScreen(FakeAuthRepository())

        // When: 아무것도 입력하지 않는다

        // Then
        composeTestRule.onNodeWithTag(submitButton).assertIsNotEnabled()
    }

    @Test
    fun `이메일만_입력하고_비밀번호를_비워_두면_로그인_버튼을_누를_수_없다`() {
        // Given
        composeTestRule.showLoginScreen(FakeAuthRepository())

        // When
        composeTestRule.onNodeWithTag(emailField).performTextInput(validEmail)

        // Then: 한쪽만 채운 상태는 제출 조건이 아니다
        composeTestRule.onNodeWithTag(submitButton).assertIsNotEnabled()
    }

    @Test
    fun `이메일과_비밀번호를_모두_입력하면_로그인_버튼이_활성된다`() {
        // Given
        composeTestRule.showLoginScreen(FakeAuthRepository())

        // When
        composeTestRule.onNodeWithTag(emailField).performTextInput(validEmail)
        composeTestRule.onNodeWithTag(passwordField).performTextInput(validPassword)

        // Then
        composeTestRule.onNodeWithTag(submitButton).performScrollTo().assertIsEnabled()
    }

    // ──────────────────────────── 3. 제출 ────────────────────────────

    @Test
    fun `로그인_버튼을_누르면_입력한_이메일과_비밀번호가_그대로_저장소로_전달된다`() {
        // Given
        val repository = FakeAuthRepository()
        composeTestRule.showLoginScreen(repository)
        composeTestRule.onNodeWithTag(emailField).performTextInput(validEmail)
        composeTestRule.onNodeWithTag(passwordField).performTextInput(validPassword)

        // When
        composeTestRule.onNodeWithTag(submitButton).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Then
        assertEquals(validEmail, repository.receivedEmail)
        assertEquals(validPassword, repository.receivedPassword)
    }

    @Test
    fun `이메일_뒤에_붙은_공백은_잘라서_전송하고_비밀번호는_손대지_않는다`() {
        // Given: 키보드 자동완성이 이메일 끝에 공백을 붙인 상황
        val repository = FakeAuthRepository()
        composeTestRule.showLoginScreen(repository)
        composeTestRule.onNodeWithTag(emailField).performTextInput("  $validEmail  ")
        composeTestRule.onNodeWithTag(passwordField).performTextInput("  spaced  ")

        // When
        composeTestRule.onNodeWithTag(submitButton).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Then: 이메일만 trim 된다(비밀번호의 공백은 유효한 문자다)
        assertEquals(validEmail, repository.receivedEmail)
        assertEquals("  spaced  ", repository.receivedPassword)
    }

    // ──────────────────────────── 4. 로딩 ────────────────────────────

    @Test
    fun `서버_응답을_기다리는_동안에는_로그인_버튼이_비활성된다`() {
        // Given: 응답이 영원히 오지 않는 저장소 → 화면이 로딩 상태에 머무른다
        val repository = FakeAuthRepository(neverResponds = true)
        composeTestRule.showLoginScreen(repository)
        composeTestRule.onNodeWithTag(emailField).performTextInput(validEmail)
        composeTestRule.onNodeWithTag(passwordField).performTextInput(validPassword)

        // When
        composeTestRule.onNodeWithTag(submitButton).performScrollTo().performClick()

        // Then: 연타로 로그인 요청이 두 번 나가지 않는다(429 잠금 방지)
        composeTestRule.onNodeWithTag(submitButton).assertIsNotEnabled()
    }

    @Test
    fun `응답을_기다리는_동안_버튼을_한_번_더_눌러도_로그인_요청은_한_번만_나간다`() {
        // Given
        val repository = FakeAuthRepository(neverResponds = true)
        composeTestRule.showLoginScreen(repository)
        composeTestRule.onNodeWithTag(emailField).performTextInput(validEmail)
        composeTestRule.onNodeWithTag(passwordField).performTextInput(validPassword)

        // When: 사용자가 두 번 눌렀다
        composeTestRule.onNodeWithTag(submitButton).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(submitButton).performClick()
        composeTestRule.waitForIdle()

        // Then
        assertEquals(1, repository.callCount)
    }

    // ──────────────────────────── 5. 실패 ────────────────────────────

    @Test
    fun `로그인에_실패하면_서버가_준_한국어_문구가_폼_안에_그대로_보인다`() {
        // Given: 비밀번호 불일치 401
        val repository = FakeAuthRepository(
            result = Result.failure(
                AppError.Api(
                    status = 401,
                    code = "INVALID_PASSWORD",
                    message = "비밀번호가 일치하지 않습니다.",
                ),
            ),
        )
        composeTestRule.showLoginScreen(repository)
        composeTestRule.onNodeWithTag(emailField).performTextInput(validEmail)
        composeTestRule.onNodeWithTag(passwordField).performTextInput("wrong-password")

        // When
        composeTestRule.onNodeWithTag(submitButton).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Then: ErrorCode 이름(INVALID_PASSWORD)이 아니라 사용자 문구가 노출된다
        composeTestRule.onNodeWithTag(errorText)
            .performScrollTo()
            .assertTextEquals("비밀번호가 일치하지 않습니다.")
    }

    @Test
    fun `에러가_떠_있어도_이메일을_다시_고치기_시작하면_에러_문구가_사라진다`() {
        // Given: 한 번 실패해서 에러 문구가 떠 있는 상태
        val repository = FakeAuthRepository(
            result = Result.failure(AppError.Network()),
        )
        composeTestRule.showLoginScreen(repository)
        composeTestRule.onNodeWithTag(emailField).performTextInput(validEmail)
        composeTestRule.onNodeWithTag(passwordField).performTextInput(validPassword)
        composeTestRule.onNodeWithTag(submitButton).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(errorText).performScrollTo().assertIsDisplayed()

        // When: 사용자가 이메일을 고치기 시작한다
        composeTestRule.onNodeWithTag(emailField).performTextInput("x")
        composeTestRule.waitForIdle()

        // Then: 방금 실패한 것처럼 보이지 않도록 문구가 사라진다
        composeTestRule.onNodeWithTag(errorText).assertDoesNotExist()
    }

    // ──────────────────────────── 6. 성공 ────────────────────────────

    @Test
    fun `로그인에_성공하면_화면이_onLoginSuccess_로_이동_신호를_올린다`() {
        // Given
        var loginSucceeded = false
        val repository = FakeAuthRepository(result = Result.success(Unit))
        composeTestRule.showLoginScreen(repository) { loginSucceeded = true }
        composeTestRule.onNodeWithTag(emailField).performTextInput(validEmail)
        composeTestRule.onNodeWithTag(passwordField).performTextInput(validPassword)

        // When
        composeTestRule.onNodeWithTag(submitButton).performScrollTo().performClick()

        // Then
        composeTestRule.waitUntil(timeoutMillis = 5_000) { loginSucceeded }
        assertTrue(loginSucceeded)
    }

    // ──────────────────────────── 7. 비밀번호 보기 토글 ────────────────────────────

    @Test
    fun `비밀번호_보기_아이콘을_누르면_숨기기_아이콘으로_바뀐다`() {
        // Given: 마스킹된 초기 상태
        composeTestRule.showLoginScreen(FakeAuthRepository())
        composeTestRule.onNodeWithContentDescription("비밀번호 보기").assertIsDisplayed()

        // When
        composeTestRule.onNodeWithTag(passwordToggle).performScrollTo().performClick()

        // Then
        composeTestRule.onNodeWithContentDescription("비밀번호 숨기기").assertIsDisplayed()
    }

    // ──────────────────────────── 테스트 도우미 ────────────────────────────

    /**
     * Hilt 없이 ViewModel 을 직접 주입해 로그인 화면을 띄운다.
     *
     * ViewModel 을 `setContent` **바깥**에서 만드는 것이 중요하다 —
     * 람다 안에서 만들면 리컴포지션마다 새 인스턴스가 생겨 입력값이 매번 초기화된다.
     */
    private fun ComposeContentTestRule.showLoginScreen(
        repository: AuthRepository,
        onLoginSuccess: () -> Unit = {},
    ) {
        val viewModel = LoginViewModel(repository)
        setContent {
            MarketOnTheme {
                LoginScreen(
                    onLoginSuccess = onLoginSuccess,
                    viewModel = viewModel,
                )
            }
        }
    }
}

/**
 * 손으로 쓴 가짜 인증 저장소.
 *
 * @param result `login()` 이 돌려줄 결과.
 * @param neverResponds true 면 `login()` 이 영원히 끝나지 않는다 → 화면이 로딩 상태에 머무른다.
 */
private class FakeAuthRepository(
    private val result: Result<Unit> = Result.success(Unit),
    private val neverResponds: Boolean = false,
) : AuthRepository {

    /** 화면이 실제로 보낸 값. "친 글자가 그대로 나갔는가" 를 확인하는 창구다. */
    var receivedEmail: String? = null
        private set
    var receivedPassword: String? = null
        private set
    var callCount: Int = 0
        private set

    override val isLoggedIn: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun login(email: String, password: String): Result<Unit> {
        callCount++
        receivedEmail = email
        receivedPassword = password
        if (neverResponds) {
            // 절대 완료되지 않는 Deferred — 응답 대기 중 상태를 그대로 붙잡아 둔다.
            return CompletableDeferred<Result<Unit>>().await()
        }
        return result
    }

    override suspend fun logout(): Result<Unit> = Result.success(Unit)
}
