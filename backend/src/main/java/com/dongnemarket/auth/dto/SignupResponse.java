package com.dongnemarket.auth.dto;

import com.dongnemarket.member.entity.Member;

public class SignupResponse {

	private final Long memberId;
	private final String email;
	private final String nickname;

	private SignupResponse(Long memberId, String email, String nickname) {
		this.memberId = memberId;
		this.email = email;
		this.nickname = nickname;
	}

	public static SignupResponse from(Member member) {
		return new SignupResponse(member.getId(), member.getEmail(), member.getNickname());
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getEmail() {
		return email;
	}

	public String getNickname() {
		return nickname;
	}
}
