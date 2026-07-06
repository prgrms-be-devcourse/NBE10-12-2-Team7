package com.dongnemarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailVerificationRequest {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String email;

	protected EmailVerificationRequest() {
	}

	public EmailVerificationRequest(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}
}
