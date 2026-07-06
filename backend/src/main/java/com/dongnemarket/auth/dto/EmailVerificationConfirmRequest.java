package com.dongnemarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailVerificationConfirmRequest {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String email;

	@NotBlank(message = "인증 코드는 필수입니다.")
	private String code;

	protected EmailVerificationConfirmRequest() {
	}

	public EmailVerificationConfirmRequest(String email, String code) {
		this.email = email;
		this.code = code;
	}

	public String getEmail() {
		return email;
	}

	public String getCode() {
		return code;
	}
}
