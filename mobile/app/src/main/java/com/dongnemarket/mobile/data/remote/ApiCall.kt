package com.dongnemarket.mobile.data.remote

import com.dongnemarket.mobile.data.remote.dto.ApiEnvelope
import com.dongnemarket.mobile.data.remote.dto.ErrorEnvelope
import com.dongnemarket.mobile.domain.model.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * 에러 응답 본문 파싱 전용 Json. 서버가 필드를 추가해도 죽지 않도록 ignoreUnknownKeys.
 * (정상 응답 파싱용 Json 은 NetworkModule 이 Retrofit 에 물려 준다.)
 */
private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * 모든 Repository가 API를 호출할 때 통과하는 단일 관문.
 *
 * 하는 일 두 가지:
 *  1. 껍데기(`ApiEnvelope`) 를 벗겨 `data` 만 꺼낸다 → 도메인·UI는 껍데기를 몰라도 된다.
 *  2. 던져진 예외를 [AppError] 로 번역해 `Result.failure` 로 담는다
 *     → 예외가 ViewModel·화면까지 튀어 올라가지 않는다. (workflow §2.2)
 *
 * 사용 예:
 * ```
 * override suspend fun login(email: String, password: String): Result<String> =
 *     apiCall { api.login(LoginRequestDto(email, password)) }.map { it.accessToken }
 * ```
 */
suspend fun <T : Any> apiCall(block: suspend () -> ApiEnvelope<T>): Result<T> =
    try {
        val envelope = block()
        val data = envelope.data
        if (data == null) Result.failure(AppError.EmptyBody()) else Result.success(data)
    } catch (e: CancellationException) {
        // 코루틴 취소는 "실패"가 아니라 정상적인 중단 신호 → 삼키면 안 되고 그대로 위로 던진다.
        throw e
    } catch (e: Throwable) {
        Result.failure(e.toAppError())
    }

/**
 * 응답 `data` 가 없는 성공(삭제 등)을 위한 변형.
 * `data == null` 을 실패로 보지 않고 성공(Unit)으로 처리한다.
 */
suspend fun apiCallForUnit(block: suspend () -> ApiEnvelope<Unit>): Result<Unit> =
    try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.toAppError())
    }

/** 던져진 예외 → 앱이 이해하는 실패 종류로 번역. */
private fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this

    // 인터넷 끊김·서버 미도달·타임아웃
    is IOException -> AppError.Network(this)

    // 서버가 4xx/5xx 로 응답 (Retrofit 이 던진다)
    is HttpException -> {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val parsed = body
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { errorJson.decodeFromString<ErrorEnvelope>(it) }.getOrNull() }

        val message = parsed?.message?.takeIf { it.isNotBlank() } ?: "요청을 처리할 수 없습니다."
        if (code() == 401) AppError.Unauthorized(message)
        else AppError.Api(status = code(), code = parsed?.error, message = message)
    }

    // JSON 형태가 계약과 다름 등
    else -> AppError.Unknown(this)
}
