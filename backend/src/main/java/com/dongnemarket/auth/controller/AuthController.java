package com.dongnemarket.auth.controller;

import com.dongnemarket.auth.dto.AccessTokenResponse;
import com.dongnemarket.auth.dto.LoginRequest;
import com.dongnemarket.auth.dto.LoginResponse;
import com.dongnemarket.auth.dto.SignupRequest;
import com.dongnemarket.auth.dto.SignupResponse;
import com.dongnemarket.auth.dto.TokenResponse;
import com.dongnemarket.auth.service.AuthService;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

	private final AuthService authService;
	private final long refreshTokenValiditySeconds;
	private final boolean cookieSecure;

	public AuthController(
			AuthService authService,
			@Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds,
			@Value("${auth.cookie.secure:false}") boolean cookieSecure) {
		this.authService = authService;
		this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
		this.cookieSecure = cookieSecure;
	}

	@Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임으로 회원가입을 진행한다. 이용약관·개인정보 수집 및 이용 동의는 필수이며, " +
			"동의 이력(버전/동의시각/IP/User-Agent)이 함께 저장된다.")
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
			@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
		SignupResponse response = authService.signup(
				request, extractIpAddress(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(HttpStatus.CREATED.value(), "회원가입이 완료되었습니다.", response));
	}

	/** 리버스 프록시(nginx 등) 뒤에 있으면 X-Forwarded-For의 첫 값을 우선한다. 동의 이력 증적용이라 엄격한 신뢰 검증까지는 하지 않는다. */
	private String extractIpAddress(HttpServletRequest httpRequest) {
		String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return httpRequest.getRemoteAddr();
	}

	@Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 JWT Access Token을 발급한다. Refresh Token은 HttpOnly 쿠키로 내려간다 " +
			"(autoLogin=true면 Max-Age가 있는 영속 쿠키, false면 브라우저 종료 시 사라지는 세션 쿠키).")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AccessTokenResponse>> login(
			@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
		LoginResponse response = authService.login(request);
		Duration maxAge = request.isAutoLogin() ? Duration.ofSeconds(refreshTokenValiditySeconds) : null;
		setRefreshTokenCookie(httpResponse, response.getRefreshToken(), maxAge);
		return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", AccessTokenResponse.of(response.getAccessToken())));
	}

	@Operation(summary = "Access Token 재발급", description = "HttpOnly 쿠키의 Refresh Token으로 만료된 Access Token을 재발급한다. Refresh Token은 회전되지 않는다.")
	@PostMapping("/reissue")
	public ResponseEntity<ApiResponse<AccessTokenResponse>> reissue(
			@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		TokenResponse response = authService.reissue(refreshToken);
		return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", AccessTokenResponse.of(response.getAccessToken())));
	}

	@Operation(summary = "로그아웃", description = "현재 로그인한 사용자의 Refresh Token을 삭제하고 쿠키를 만료시킨다. 여러 번 호출해도 항상 성공한다.")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long memberId, HttpServletResponse httpResponse) {
		authService.logout(memberId);
		setRefreshTokenCookie(httpResponse, "", Duration.ZERO);
		return ResponseEntity.ok(ApiResponse.success());
	}

	/**
	 * Refresh Token을 HttpOnly 쿠키로 내려보낸다. Domain은 지정하지 않아 Host-Only 쿠키로 유지한다.
	 * @param maxAge null이면 Max-Age/Expires를 지정하지 않는 세션 쿠키(브라우저 종료 시 삭제)로 발급한다.
	 */
	private void setRefreshTokenCookie(HttpServletResponse httpResponse, String value, Duration maxAge) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
				.httpOnly(true)
				.secure(cookieSecure)
				.sameSite("Lax")
				.path("/");
		if (maxAge != null) {
			builder.maxAge(maxAge);
		}
		httpResponse.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
	}
}
