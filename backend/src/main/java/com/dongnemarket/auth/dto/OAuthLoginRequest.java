package com.dongnemarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class OAuthLoginRequest {

	@NotBlank
	private String code;

	@NotBlank
	private String state;

	protected OAuthLoginRequest() {
	}

	public String getCode() {
		return code;
	}

	public String getState() {
		return state;
	}
}
