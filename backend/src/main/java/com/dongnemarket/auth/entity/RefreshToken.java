package com.dongnemarket.auth.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false, unique = true)
	private Long memberId;

	@Column(nullable = false, length = 512)
	private String token;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	protected RefreshToken() {
	}

	private RefreshToken(Long memberId, String token, LocalDateTime expiresAt) {
		this.memberId = memberId;
		this.token = token;
		this.expiresAt = expiresAt;
	}

	/** 최초 로그인 시 발급 */
	public static RefreshToken issue(Long memberId, String token, LocalDateTime expiresAt) {
		return new RefreshToken(memberId, token, expiresAt);
	}

	/** 재로그인/재발급 시 기존 토큰을 새 값으로 교체(회원당 1개 세션 정책) */
	public void replace(String token, LocalDateTime expiresAt) {
		this.token = token;
		this.expiresAt = expiresAt;
	}

	/** 저장된 토큰 문자열과 일치하는지 확인 (교체되어 폐기된 구 토큰 재사용 방지) */
	public boolean matches(String token) {
		return this.token.equals(token);
	}

	public Long getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getToken() {
		return token;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}
}
