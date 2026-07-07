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

	private static final String DELETED_DISPLAY_NAME = "탈퇴한 회원입니다";

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

	/** 관리자 계정 시드용 (ROLE_ADMIN, ACTIVE) */
	public static Member createAdmin(String email, String password, String nickname) {
		return new Member(email, password, nickname, Role.ROLE_ADMIN, MemberStatus.ACTIVE);
	}

	public void update(String nickname) {
		this.nickname = nickname;
	}

	/** 비밀번호 변경. 이미 인코딩된 값을 받는다(인코딩 책임은 Service). */
	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	public void softDelete() {
		this.status = MemberStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}

	/** 관리자에 의한 회원 상태 변경 */
	public void changeStatus(MemberStatus status) {
		this.status = status;
		this.deletedAt = (status == MemberStatus.DELETED) ? LocalDateTime.now() : null;
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

	/** 다른 도메인이 작성자·상대방 닉네임을 노출할 때 쓰는 표시용 닉네임. 탈퇴 회원은 실제 닉네임 대신 고정 문구를 반환한다. */
	public String getDisplayNickname() {
		return status == MemberStatus.DELETED ? DELETED_DISPLAY_NAME : nickname;
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
