package com.dongnemarket.mobile.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 로그인 화면(앱의 시작 화면).
 *
 * ViewModel 을 아는 쪽과 모르는 쪽을 일부러 두 개로 나눴다:
 *  - [LoginScreen]     : `hiltViewModel()` 로 상태를 구독하고 성공 시 이동을 알린다(연결 담당).
 *  - [LoginContent]    : 상태와 콜백만 받는 순수 UI → `@Preview` 로 검수하고 UI 테스트에서 단독 실행한다.
 *
 * 이 화면은 [androidx.navigation.NavController] 를 받지 않는다. "홈으로 가라"가 아니라
 * "로그인이 끝났다"만 [onLoginSuccess] 로 알리고, 어디로 갈지는 NavHost 가 결정한다.
 *
 * ⚠️ 로그인 성공 후 이어지는 호출(내 정보 `GET /api/members/me` → 내 동네 → 찜 목록 동기화)은
 * 이 화면의 책임이 아니다. 여기서는 토큰 확보까지만 끝내고 [onLoginSuccess] 로 넘긴다.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // isLoggedIn 이 true 로 바뀌는 순간 딱 한 번 실행된다(리컴포지션마다 다시 호출되지 않는다).
    // 이 한 줄이 "ViewModel 은 이동을 모른다"는 규칙을 지키면서도 화면 전환을 가능하게 하는 이음새다.
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        modifier = modifier,
    )
}

/**
 * 로그인 화면의 그림 부분. 상태를 만들지 않고 받은 것만 그린다(stateless).
 *
 * 예외적으로 "비밀번호 보기" 토글만 여기서 `rememberSaveable` 로 들고 있는다 —
 * 서버·도메인과 무관한 순수 표시 옵션이라 UiState 에 넣으면 ViewModel 이 UI 관심사를 알게 된다.
 * (`rememberSaveable` 이라서 화면 회전에도 유지된다.)
 */
@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // 로그인을 누르면 포커스를 해제한다 → 소프트 키보드가 함께 내려가고, 결과(에러 문구·스피너)가 가려지지 않는다.
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // enableEdgeToEdge() 로 시스템 바 아래까지 그려지므로 상태바/내비바 높이를 직접 피해 준다.
            .systemBarsPadding()
            // imePadding 을 verticalScroll 보다 '먼저'(바깥에) 두어야 키보드가 올라온 만큼
            // 스크롤 영역 자체가 줄어든다. 순서를 바꾸면 입력창이 키보드에 가려진다.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(72.dp))

        // ── 브랜드 ─────────────────────────────────────────────
        Text(
            text = "MarketON",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "우리 동네 중고거래, 마켓온",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        // ── 이메일 ─────────────────────────────────────────────
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email"),
            label = { Text("이메일") },
            singleLine = true,
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            shape = MaterialTheme.shapes.small,
        )

        // ── 비밀번호 ────────────────────────────────────────────
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password"),
            label = { Text("비밀번호") },
            singleLine = true,
            enabled = !uiState.isLoading,
            // 토글 상태에 따라 마스킹을 켜고 끈다. 저장되는 값은 항상 평문이고 표시만 바뀐다.
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            // 키보드의 완료 키로도 로그인되게 한다(버튼까지 손을 옮기지 않아도 되도록).
            keyboardActions = KeyboardActions(
                onDone = {
                    if (uiState.canSubmit) {
                        focusManager.clearFocus()
                        onSubmit()
                    }
                },
            ),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.testTag("login_password_toggle"),
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                    )
                }
            },
            shape = MaterialTheme.shapes.small,
        )

        // ── 에러 문구 ───────────────────────────────────────────
        // 스낵바가 아니라 폼 안에 붙이는 이유: 로그인 실패는 "방금 입력한 값"에 대한 피드백이라
        // 사라지는 토스트보다 입력창 옆에 남아 있는 편이 낫다. 문구는 서버가 준 한국어 그대로다.
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_error"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Start,
            )
        }

        Spacer(Modifier.height(4.dp))

        // ── 로그인 버튼 ─────────────────────────────────────────
        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("login_submit"),
            enabled = uiState.canSubmit,
            shape = MaterialTheme.shapes.medium,
        ) {
            if (uiState.isLoading) {
                // 버튼 높이가 고정이라 스피너로 바뀌어도 레이아웃이 들썩이지 않는다.
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = "로그인",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // 회원가입·비밀번호 찾기 링크는 일부러 두지 않았다.
        // 가입 화면이 Phase 1 에 없고, 비밀번호 재설정은 앱 딥링크가 아니라 웹 페이지로 열린다(계약 §8-14).
        // 누를 수 없는 버튼을 두는 것보다 없는 편이 낫다.
    }
}

@Preview(name = "로그인 · 초기", showBackground = true, heightDp = 720)
@Composable
private fun LoginContentEmptyPreview() {
    MarketOnTheme {
        LoginContent(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
        )
    }
}

@Preview(name = "로그인 · 입력 완료", showBackground = true, heightDp = 720)
@Composable
private fun LoginContentFilledPreview() {
    MarketOnTheme {
        LoginContent(
            uiState = LoginUiState(email = "buyer@marketon.com", password = "Passw0rd!!"),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
        )
    }
}

@Preview(name = "로그인 · 로딩", showBackground = true, heightDp = 720)
@Composable
private fun LoginContentLoadingPreview() {
    MarketOnTheme {
        LoginContent(
            uiState = LoginUiState(
                email = "buyer@marketon.com",
                password = "Passw0rd!!",
                isLoading = true,
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
        )
    }
}

@Preview(name = "로그인 · 실패", showBackground = true, heightDp = 720)
@Composable
private fun LoginContentErrorPreview() {
    MarketOnTheme {
        LoginContent(
            uiState = LoginUiState(
                email = "buyer@marketon.com",
                password = "wrong",
                errorMessage = "비밀번호가 일치하지 않습니다.",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
        )
    }
}
