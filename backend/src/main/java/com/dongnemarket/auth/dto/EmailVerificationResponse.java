package com.dongnemarket.auth.dto;

import java.time.LocalDateTime;

public class EmailVerificationResponse {

	private final String email;
	private final LocalDateTime expiresAt;

	public EmailVerificationResponse(String email, LocalDateTime expiresAt) {
		this.email = email;
		this.expiresAt = expiresAt;
	}

	public String getEmail() {
		return email;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}
}
