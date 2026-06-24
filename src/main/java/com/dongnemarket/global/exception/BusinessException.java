package com.dongnemarket.global.exception;

import lombok.Getter;

/**
 * 모든 비즈니스 예외의 단일 타입.
 * <p>도메인 로직에서 RuntimeException 을 직접 던지지 않고 이 예외 + {@link ErrorCode} 를 사용한다.
 * (00-ai-common-rules.md 절대 규칙 8)
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	/** 기본 메시지 대신 상황별 메시지를 덧붙일 때 */
	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
