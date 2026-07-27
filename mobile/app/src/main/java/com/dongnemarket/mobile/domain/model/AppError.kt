package com.dongnemarket.mobile.domain.model

/**
 * 앱이 다루는 실패의 종류. Domain 계층이 소유한다.
 *
 * 왜 필요한가: 백엔드는 실패 시 `{status, error:"PRODUCT_NOT_FOUND", message, timestamp}` 를 준다.
 * 이 `error`(ErrorCode 이름)를 화면까지 그대로 흘리면 사용자에게 개발자 용어가 노출되고,
 * UI가 백엔드 enum에 묶인다. 그래서 Data 계층에서 이 타입으로 한 번 번역하고,
 * UI는 [userMessage] 만 읽어서 보여준다. (workflow §2.2)
 *
 * Throwable 을 상속하는 이유: Repository가 `Result<T>` 로 성공/실패를 반환하는데,
 * Kotlin 의 `Result.failure()` 는 Throwable 만 받기 때문이다.
 */
sealed class AppError(
    /** 화면에 그대로 띄워도 되는 한국어 문장 */
    val userMessage: String,
    cause: Throwable? = null,
) : Throwable(userMessage, cause) {

    /** 인터넷이 끊김·서버에 닿지 못함·타임아웃 (IOException 계열) */
    class Network(cause: Throwable? = null) :
        AppError("네트워크 연결을 확인해 주세요.", cause)

    /** 토큰이 없거나 만료됨(HTTP 401). 로그인 화면으로 보내는 신호로 쓴다. */
    class Unauthorized(message: String = "다시 로그인해 주세요.") :
        AppError(message)

    /**
     * 서버가 정상적으로 응답했지만 실패 상태인 경우(4xx·5xx).
     * @param status HTTP 상태 코드
     * @param code 백엔드 ErrorCode 이름 — 로그·분기용이며 화면에 노출하지 않는다
     */
    class Api(val status: Int, val code: String?, message: String) :
        AppError(message)

    /** 응답 껍데기는 왔는데 data 가 비어 있음 — 계약 위반이므로 버그로 취급 */
    class EmptyBody :
        AppError("서버 응답이 올바르지 않습니다.")

    /** JSON 파싱 실패 등 예상 못 한 오류 */
    class Unknown(cause: Throwable? = null) :
        AppError("알 수 없는 오류가 발생했습니다.", cause)
}
