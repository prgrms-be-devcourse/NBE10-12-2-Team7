package com.dongnemarket.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 백엔드 공통 성공 응답 껍데기.
 * 원본: backend `com.dongnemarket.global.response.ApiResponse<T>`
 *
 * ```json
 * { "status": 200, "message": "요청이 성공적으로 처리되었습니다.", "data": { ... } }
 * ```
 *
 * 모든 API가 이 형태로 감싸서 주므로, ApiService 의 반환 타입은
 * `ApiEnvelope<LoginResponseDto>` 처럼 항상 이걸 한 겹 씌운다.
 * `data` 가 nullable 인 이유: 삭제 API 처럼 데이터 없는 성공(`data` 생략)이 있다.
 */
@Serializable
data class ApiEnvelope<T>(
    val status: Int,
    val message: String? = null,
    val data: T? = null,
)

/**
 * 백엔드 공통 에러 응답 껍데기.
 * 원본: backend `com.dongnemarket.global.response.ErrorResponse`
 *
 * ```json
 * { "status": 404, "error": "PRODUCT_NOT_FOUND", "message": "...", "timestamp": "2026-06-17T12:00:00" }
 * ```
 *
 * `error` 는 백엔드 ErrorCode enum 이름이다. 화면에 노출하지 않고
 * [com.dongnemarket.mobile.domain.model.AppError] 로 번역할 때 참고용으로만 쓴다.
 */
@Serializable
data class ErrorEnvelope(
    val status: Int = 0,
    val error: String? = null,
    val message: String? = null,
    val timestamp: String? = null,
)
