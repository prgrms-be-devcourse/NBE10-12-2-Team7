package com.dongnemarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class ReissueRequest {

	@NotBlank(message = "Refresh Token은 필수입니다.")
	private String refreshToken;

	protected ReissueRequest() {
	}

	public ReissueRequest(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}
}
