package com.dongnemarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	private String password;

	/** 자동 로그인 체크 여부. true면 Refresh Token 쿠키를 브라우저 종료 후에도 유지되게 발급하고, false면 세션 쿠키로 발급한다. */
	private boolean autoLogin;

	protected LoginRequest() {
	}

	public LoginRequest(String email, String password) {
		this(email, password, false);
	}

	public LoginRequest(String email, String password, boolean autoLogin) {
		this.email = email;
		this.password = password;
		this.autoLogin = autoLogin;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public boolean isAutoLogin() {
		return autoLogin;
	}
}
