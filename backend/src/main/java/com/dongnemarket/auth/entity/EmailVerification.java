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
 * 이메일의 "인증 완료" 여부만 담는다. 인증 코드 자체(발급·비교·쿨다운·만료)는 TTL 데이터라 Redis
 * ({@code EmailVerificationCodeRepository})가 담당하고, 여기서는 관리하지 않는다.
 * <p>인증 완료 상태는 코드의 5분 TTL과 무관하게 회원가입 시점까지 유지돼야 하므로(인증 후 한참 뒤에
 * 가입해도 통과) TTL 저장소가 아닌 DB에 남긴다({@code AuthService.signup}의 existsByEmailAndVerifiedTrue 참고).
 */
@Entity
@Table(name = "email_verifications")
public class EmailVerification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false)
	private boolean verified = false;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	protected EmailVerification() {
	}

	private EmailVerification(String email) {
		this.email = email;
	}

	/** 새 인증 코드로 인증에 성공했을 때 최초 발급 */
	public static EmailVerification verified(String email, LocalDateTime verifiedAt) {
		EmailVerification verification = new EmailVerification(email);
		verification.verify(verifiedAt);
		return verification;
	}

	/** 인증 완료로 표시 */
	public void verify(LocalDateTime now) {
		this.verified = true;
		this.verifiedAt = now;
	}

	/** 같은 이메일로 새 인증 코드를 재요청하면, 이전 인증 상태를 무효화한다(새 코드에 대해 다시 인증해야 함). */
	public void unverify() {
		this.verified = false;
		this.verifiedAt = null;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public boolean isVerified() {
		return verified;
	}

	public LocalDateTime getVerifiedAt() {
		return verifiedAt;
	}
}
