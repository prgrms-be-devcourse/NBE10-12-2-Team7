package com.dongnemarket.auth.controller;

import com.dongnemarket.auth.dto.LoginRequest;
import com.dongnemarket.auth.dto.LoginResponse;
import com.dongnemarket.auth.dto.ReissueRequest;
import com.dongnemarket.auth.dto.SignupRequest;
import com.dongnemarket.auth.dto.SignupResponse;
import com.dongnemarket.auth.dto.TokenResponse;
import com.dongnemarket.auth.service.AuthService;
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
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임으로 회원가입을 진행한다.")
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
		SignupResponse response = authService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(HttpStatus.CREATED.value(), "회원가입이 완료되었습니다.", response));
	}

	@Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 JWT Access Token/Refresh Token을 발급한다.")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		LoginResponse response = authService.login(request);
		return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
	}

	@Operation(summary = "Access Token 재발급", description = "유효한 Refresh Token으로 만료된 Access Token을 재발급한다. Refresh Token은 회전되지 않는다.")
	@PostMapping("/reissue")
	public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
		TokenResponse response = authService.reissue(request.getRefreshToken());
		return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
	}
}
