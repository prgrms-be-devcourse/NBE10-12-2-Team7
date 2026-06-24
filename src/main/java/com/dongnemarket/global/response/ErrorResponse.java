package com.dongnemarket.global.response;

import com.dongnemarket.global.exception.ErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 공통 에러 응답 포맷.
 * <pre>{ "status": 404, "error": "PRODUCT_NOT_FOUND", "message": "...", "timestamp": "2026-06-17T12:00:00" }</pre>
 * {@code error} 는 ErrorCode 의 이름(enum 상수명)이다.
 */
@Getter
public class ErrorResponse {

	private final int status;
	private final String error;
	private final String message;
	private final LocalDateTime timestamp;

	private ErrorResponse(int status, String error, String message) {
		this.status = status;
		this.error = error;
		this.message = message;
		this.timestamp = LocalDateTime.now();
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.getStatus(), errorCode.name(), errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.getStatus(), errorCode.name(), message);
	}
}
