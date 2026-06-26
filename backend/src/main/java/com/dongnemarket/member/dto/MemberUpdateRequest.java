package com.dongnemarket.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberUpdateRequest {

	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
	private String nickname;

	protected MemberUpdateRequest() {
	}

	public MemberUpdateRequest(String nickname) {
		this.nickname = nickname;
	}

	public String getNickname() {
		return nickname;
	}
}
