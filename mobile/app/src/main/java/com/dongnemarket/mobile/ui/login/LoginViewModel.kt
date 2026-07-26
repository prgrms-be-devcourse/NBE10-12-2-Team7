package com.dongnemarket.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 로그인 화면의 상태 보관소 겸 이벤트 처리기.
 *
 * Spring 에 빗대면 화면 하나짜리 컨트롤러 + 세션 스코프 모델에 가깝다.
 * 화면 회전이나 리컴포지션으로 Composable 이 다시 실행돼도 이 객체는 살아 있어서
 * 입력값과 로딩 상태가 초기화되지 않는다.
 *
 * 규칙 두 가지:
 *  1. `android.*` UI 타입(Context·View·Toast)을 참조하지 않는다 → 순수 JVM 단위 테스트가 가능해진다.
 *  2. 토큰을 만지지 않는다. `AuthRepository.login()` 이 성공하면 **저장은 Repository 가 이미 끝냈고**
 *     이후 요청의 `Authorization` 헤더는 `AuthInterceptor` 가 자동으로 붙인다.
 *     그래서 여기서는 "성공했다"는 사실만 상태에 남긴다.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    /** 화면이 구독하는 읽기 전용 창구. `MutableStateFlow` 를 그대로 노출하면 화면이 상태를 바꿀 수 있다. */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * 이메일 입력. 값을 갱신하면서 **이전 에러 문구를 지운다** —
     * 사용자가 고치기 시작했는데 "비밀번호가 일치하지 않습니다"가 계속 붙어 있으면 방금 실패한 것처럼 보인다.
     */
    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    /** 비밀번호 입력. 처리 이유는 [onEmailChange] 와 같다. */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    /**
     * 로그인 버튼 클릭.
     *
     * 버튼이 비활성 상태여도 다시 [LoginUiState.canSubmit] 을 확인하는 이유:
     * 버튼 비활성은 "그림"일 뿐이고, 연타·테스트 코드·접근성 도구가 이벤트를 두 번 보낼 수 있다.
     * 로그인은 실패 5회면 10분 잠기는 API(429 `TOO_MANY_LOGIN_ATTEMPTS`)라 중복 호출이 특히 위험하다.
     *
     * 이메일만 `trim()` 하는 이유: 키보드 자동완성이 뒤에 공백을 붙이면 서버의 `@Email` 검증이 실패하는데,
     * 비밀번호는 공백도 유효한 문자이므로 손대면 안 된다.
     */
    fun onSubmit() {
        val current = _uiState.value
        if (!current.canSubmit) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.login(
                email = current.email.trim(),
                password = current.password,
            )

            result
                .onSuccess {
                    // isLoading 을 내리면서 isLoggedIn 을 올린다 → 화면이 LaunchedEffect 로 홈으로 이동한다.
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                }
                .onFailure { error ->
                    // 실패 사유가 5가지(미가입 404 / 비번 401 / 정지 403 / 5회초과 429 / 탈퇴 400)지만
                    // 서버가 이미 한국어 문구를 주므로 여기서 분기하지 않는다. AppError 가 아닌 예외는
                    // Repository 계약상 오지 않지만, 만약 온다면 사용자에게 스택트레이스를 보이지 않도록 기본 문구로 대체.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = (error as? AppError)?.userMessage
                                ?: "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                        )
                    }
                }
        }
    }
}
