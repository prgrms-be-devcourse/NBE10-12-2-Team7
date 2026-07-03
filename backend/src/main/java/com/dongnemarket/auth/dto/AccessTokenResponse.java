package com.dongnemarket.auth.dto;

public class AccessTokenResponse {

	private final String accessToken;

	private AccessTokenResponse(String accessToken) {
		this.accessToken = accessToken;
	}

	public static AccessTokenResponse of(String accessToken) {
		return new AccessTokenResponse(accessToken);
	}

	public String getAccessToken() {
		return accessToken;
	}
}
