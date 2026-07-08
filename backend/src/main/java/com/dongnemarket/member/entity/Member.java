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

	/** 탈퇴 회원의 표시용 닉네임 마스킹 문구. */
	private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

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

	/**
	 * 탈퇴(DELETED)한 회원인지 여부. "탈퇴" 판정의 단일 기준점으로, 닉네임 마스킹·채팅 전송 차단 등
	 * 탈퇴 여부에 반응하는 모든 지점이 이 메서드를 사용한다.
	 * (SUSPENDED는 관리자 정지일 뿐 탈퇴가 아니므로 여기에 포함하지 않는다.)
	 */
	public boolean isWithdrawn() {
		return status == MemberStatus.DELETED;
	}

	/**
	 * 화면 표시용 닉네임. 탈퇴한 회원은 실명 닉네임 대신 마스킹 문구를 반환한다.
	 * 닉네임이 노출되는 모든 지점(채팅 상대·판매자 등)에서 이 메서드를 사용해 마스킹을 일관 적용한다.
	 */
	public String getDisplayNickname() {
		return isWithdrawn() ? WITHDRAWN_NICKNAME : nickname;
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
