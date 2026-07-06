package com.dongnemarket.auth.dto;

public class EmailVerificationConfirmResponse {

	private final String email;
	private final boolean verified;

	public EmailVerificationConfirmResponse(String email, boolean verified) {
		this.email = email;
		this.verified = verified;
	}

	public String getEmail() {
		return email;
	}

	public boolean isVerified() {
		return verified;
	}
}
