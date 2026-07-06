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
@Table(name = "email_verifications")
public class EmailVerification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false, length = 6)
	private String code;

	@Column(name = "sent_at", nullable = false)
	private LocalDateTime sentAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	protected EmailVerification() {
	}

	private EmailVerification(String email, String code, LocalDateTime sentAt, LocalDateTime expiresAt) {
		this.email = email;
		this.code = code;
		this.sentAt = sentAt;
		this.expiresAt = expiresAt;
	}

	/** 최초 인증코드 발송 시 발급 */
	public static EmailVerification issue(String email, String code, LocalDateTime sentAt, LocalDateTime expiresAt) {
		return new EmailVerification(email, code, sentAt, expiresAt);
	}

	/** 재요청(쿨다운 경과 후) 시 기존 row를 새 코드로 교체(이메일당 1개 유지) */
	public void replace(String code, LocalDateTime sentAt, LocalDateTime expiresAt) {
		this.code = code;
		this.sentAt = sentAt;
		this.expiresAt = expiresAt;
	}

	/** 마지막 발송 이후 쿨다운 시간이 지나지 않았는지 확인 (재요청 스팸 방지) */
	public boolean isCoolingDown(LocalDateTime now, long cooldownSeconds) {
		return now.isBefore(sentAt.plusSeconds(cooldownSeconds));
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getCode() {
		return code;
	}

	public LocalDateTime getSentAt() {
		return sentAt;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}
}
