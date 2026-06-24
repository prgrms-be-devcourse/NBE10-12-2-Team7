package com.dongnemarket.global.exception;

import com.dongnemarket.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기. 모든 예외를 공통 {@link ErrorResponse} 포맷으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 비즈니스 예외 (도메인 ErrorCode 기반) */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("BusinessException: [{}] {}", errorCode.getCode(), e.getMessage());
		return ResponseEntity.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, e.getMessage()));
	}

	/** @Valid 검증 실패 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(FieldError::getDefaultMessage)
				.orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
		log.warn("Validation failed: {}", message);
		return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
				.body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message));
	}

	/** 인가 실패 (@PreAuthorize 등에서 컨트롤러 계층까지 전파된 경우) */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
		return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatus())
				.body(ErrorResponse.of(ErrorCode.FORBIDDEN));
	}

	/** 그 외 처리되지 않은 예외 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
				.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
