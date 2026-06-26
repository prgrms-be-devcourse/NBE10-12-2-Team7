package com.dongnemarket.member.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(nullable = false, unique = true, length = 20)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberStatus status;

	@Column
	private LocalDateTime deletedAt;

	protected Member() {
	}

	private Member(String email, String password, String nickname, Role role, MemberStatus status) {
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.role = role;
		this.status = status;
	}

	/** 회원가입 시 일반 사용자(ROLE_USER, ACTIVE) 생성 */
	public static Member createUser(String email, String password, String nickname) {
		return new Member(email, password, nickname, Role.ROLE_USER, MemberStatus.ACTIVE);
	}

	public void update(String nickname) {
		this.nickname = nickname;
	}

	public void softDelete() {
		this.status = MemberStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
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

	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}
}
