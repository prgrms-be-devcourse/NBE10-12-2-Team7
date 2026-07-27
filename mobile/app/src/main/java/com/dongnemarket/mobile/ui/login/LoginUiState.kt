package com.dongnemarket.mobile.ui.login

/**
 * 로그인 화면이 그리는 데 필요한 모든 것을 담은 **단일 상태 객체**.
 *
 * 왜 `sealed class`(Loading/Success/Error)가 아니라 `data class` 인가:
 * 목록 화면처럼 "로딩 중이면 목록이 없다"가 아니라, 로그인은 **입력 폼**이라서
 * 로딩 중에도 사용자가 입력한 이메일·비밀번호가 그대로 남아 있어야 한다.
 * 상태가 서로 배타적이지 않으므로 필드를 나란히 두는 편이 맞다.
 *
 * 이 클래스는 화면이 읽기만 하는 스냅샷이다(불변). ViewModel 이 `copy()` 로 새 값을 만들어
 * `StateFlow` 에 밀어 넣고, Compose 가 바뀐 부분만 다시 그린다.
 */
data class LoginUiState(
    /** 이메일 입력창의 현재 값(사용자가 타이핑한 원문. 공백 제거는 전송 직전에 한다) */
    val email: String = "",
    /** 비밀번호 입력창의 현재 값. 화면에서는 마스킹되지만 상태에는 평문으로 들고 있는다 */
    val password: String = "",
    /** 서버 응답을 기다리는 중 — 버튼을 비활성화하고 스피너를 돌린다 */
    val isLoading: Boolean = false,
    /**
     * 화면에 그대로 띄울 실패 문구. **`AppError.userMessage` 만 들어온다.**
     * 백엔드 `error` 코드(`INVALID_PASSWORD` 같은 ErrorCode 이름)는 절대 여기 담지 않는다.
     * `null` 이면 에러 영역을 그리지 않는다.
     */
    val errorMessage: String? = null,
    /**
     * 로그인 성공 신호. 화면이 이 값을 보고 `onLoginSuccess()`(홈으로 이동)를 한 번 호출한다.
     * 상태로 두는 이유: ViewModel 이 NavController 를 모르게 하려면 "이동해라"가 아니라
     * "성공했다"는 사실만 알려 주고 이동은 화면이 결정해야 한다.
     */
    val isLoggedIn: Boolean = false,
) {

    /**
     * 로그인 버튼을 누를 수 있는가. 화면과 ViewModel 이 같은 조건을 두 번 쓰지 않도록 여기 한 곳에 둔다.
     *
     * `!isLoggedIn` 조건이 붙은 이유: 성공 직후 화면이 이동하기까지 한두 프레임이 남는데,
     * 그 틈에 버튼이 다시 활성화되면 로그인 요청이 두 번 나갈 수 있다.
     */
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading && !isLoggedIn
}
