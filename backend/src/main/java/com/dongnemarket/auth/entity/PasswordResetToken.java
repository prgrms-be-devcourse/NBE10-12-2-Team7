package com.dongnemarket.auth.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 원문 토큰은 이메일로만 전달되고 DB에는 저장하지 않는다. 여기 저장되는 값은 SHA-256 해시뿐이라,
 * DB가 유출돼도 원문 토큰(=계정 탈취 수단)을 복원할 수 없다.
 * <p>1회용 토큰이며, 사용에 성공하면 row 자체가 즉시 삭제된다({@code PasswordResetService.confirmReset}).
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false, unique = true)
	private Long memberId;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "sent_at", nullable = false)
	private LocalDateTime sentAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	protected PasswordResetToken() {
	}

	private PasswordResetToken(Long memberId, String tokenHash, LocalDateTime sentAt, LocalDateTime expiresAt) {
		this.memberId = memberId;
		this.tokenHash = tokenHash;
		this.sentAt = sentAt;
		this.expiresAt = expiresAt;
	}

	/** 최초 재설정 요청 시 발급 */
	public static PasswordResetToken issue(Long memberId, String tokenHash, LocalDateTime sentAt, LocalDateTime expiresAt) {
		return new PasswordResetToken(memberId, tokenHash, sentAt, expiresAt);
	}

	/** 재요청(쿨다운 경과 후) 시 기존 row를 새 토큰 해시로 교체(회원당 1개 유지, 이전 토큰은 즉시 무효화). */
	public void replace(String tokenHash, LocalDateTime sentAt, LocalDateTime expiresAt) {
		this.tokenHash = tokenHash;
		this.sentAt = sentAt;
		this.expiresAt = expiresAt;
	}

	/** 마지막 발송 이후 쿨다운 시간이 지나지 않았는지 확인 (재요청 스팸 방지) */
	public boolean isCoolingDown(LocalDateTime now, long cooldownSeconds) {
		return now.isBefore(sentAt.plusSeconds(cooldownSeconds));
	}

	/** 토큰 만료 여부 확인 */
	public boolean isExpired(LocalDateTime now) {
		return now.isAfter(expiresAt);
	}

	public Long getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public LocalDateTime getSentAt() {
		return sentAt;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}
}
