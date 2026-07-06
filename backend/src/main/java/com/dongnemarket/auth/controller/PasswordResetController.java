package com.dongnemarket.auth.controller;

import com.dongnemarket.auth.dto.PasswordResetConfirmRequest;
import com.dongnemarket.auth.dto.PasswordResetRequest;
import com.dongnemarket.auth.service.PasswordResetService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth/password-resets")
public class PasswordResetController {

	private static final String REQUEST_MESSAGE = "해당 이메일로 가입된 계정이 있다면 비밀번호 재설정 메일을 발송했습니다.";

	private final PasswordResetService passwordResetService;

	public PasswordResetController(PasswordResetService passwordResetService) {
		this.passwordResetService = passwordResetService;
	}

	@Operation(summary = "비밀번호 재설정 요청", description = "이메일로 비밀번호 재설정 토큰을 발송한다. 계정 존재 여부를 노출하지 않기 위해 " +
			"가입 여부와 무관하게 항상 동일한 응답을 반환한다.")
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> requestReset(@Valid @RequestBody PasswordResetRequest request) {
		passwordResetService.requestReset(request);
		return ResponseEntity.ok(ApiResponse.success(REQUEST_MESSAGE, null));
	}

	@Operation(summary = "비밀번호 재설정 확인", description = "재설정 토큰을 확인해 새 비밀번호로 변경한다. 토큰이 유효하지 않거나 이미 사용됐으면 400 " +
			"INVALID_RESET_TOKEN, 만료됐으면 400 EXPIRED_RESET_TOKEN을 반환한다. 성공 시 기존 Refresh Token은 삭제된다.")
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponse<Void>> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
		passwordResetService.confirmReset(request);
		return ResponseEntity.ok(ApiResponse.success("비밀번호가 재설정되었습니다.", null));
	}
}
