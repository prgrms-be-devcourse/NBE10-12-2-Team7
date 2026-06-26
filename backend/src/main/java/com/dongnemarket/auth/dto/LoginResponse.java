package com.dongnemarket.auth.dto;

public class LoginResponse {

	private final String accessToken;

	private LoginResponse(String accessToken) {
		this.accessToken = accessToken;
	}

	public static LoginResponse of(String accessToken) {
		return new LoginResponse(accessToken);
	}

	public String getAccessToken() {
		return accessToken;
	}
}
