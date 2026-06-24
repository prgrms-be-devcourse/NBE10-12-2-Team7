package com.dongnemarket.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 공통 성공 응답 포맷.
 * <pre>{ "status": 200, "message": "...", "data": {} }</pre>
 * 모든 도메인은 Controller에서 성공 응답을 이 타입으로 감싼다.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";

	private final int status;
	private final String message;
	private final T data;

	private ApiResponse(int status, String message, T data) {
		this.status = status;
		this.message = message;
		this.data = data;
	}

	/** 200 + 기본 메시지 + 데이터 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(200, DEFAULT_SUCCESS_MESSAGE, data);
	}

	/** 200 + 커스텀 메시지 + 데이터 */
	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(200, message, data);
	}

	/** 커스텀 status(예: 201) + 메시지 + 데이터 */
	public static <T> ApiResponse<T> success(int status, String message, T data) {
		return new ApiResponse<>(status, message, data);
	}

	/** 데이터 없는 성공 (예: 삭제) */
	public static ApiResponse<Void> success() {
		return new ApiResponse<>(200, DEFAULT_SUCCESS_MESSAGE, null);
	}
}
