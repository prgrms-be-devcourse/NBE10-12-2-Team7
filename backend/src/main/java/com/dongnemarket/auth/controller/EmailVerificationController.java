package com.dongnemarket.auth.controller;

import com.dongnemarket.auth.dto.EmailVerificationRequest;
import com.dongnemarket.auth.dto.EmailVerificationResponse;
import com.dongnemarket.auth.service.EmailVerificationService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth/email-verifications")
public class EmailVerificationController {

	private final EmailVerificationService emailVerificationService;

	public EmailVerificationController(EmailVerificationService emailVerificationService) {
		this.emailVerificationService = emailVerificationService;
	}

	@Operation(summary = "이메일 인증 코드 발송", description = "회원가입 전 이메일로 인증 코드를 발송한다. 이미 가입된 이메일이면 409, 60초 이내 재요청이면 429를 반환한다.")
	@PostMapping
	public ResponseEntity<ApiResponse<EmailVerificationResponse>> requestVerification(
			@Valid @RequestBody EmailVerificationRequest request) {
		EmailVerificationResponse response = emailVerificationService.requestVerification(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(HttpStatus.CREATED.value(), "인증 코드가 발송되었습니다.", response));
	}
}
