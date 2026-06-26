package com.dongnemarket.member.dto;

import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.entity.Role;

import java.time.LocalDateTime;

public class MemberResponse {

	private final Long memberId;
	private final String email;
	private final String nickname;
	private final Role role;
	private final MemberStatus status;
	private final LocalDateTime createdAt;

	private MemberResponse(Long memberId, String email, String nickname,
						   Role role, MemberStatus status, LocalDateTime createdAt) {
		this.memberId = memberId;
		this.email = email;
		this.nickname = nickname;
		this.role = role;
		this.status = status;
		this.createdAt = createdAt;
	}

	public static MemberResponse from(Member member) {
		return new MemberResponse(
				member.getId(),
				member.getEmail(),
				member.getNickname(),
				member.getRole(),
				member.getStatus(),
				member.getCreatedAt()
		);
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

	public Role getRole() {
		return role;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
