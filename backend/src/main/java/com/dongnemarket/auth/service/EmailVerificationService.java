package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.EmailVerificationConfirmRequest;
import com.dongnemarket.auth.dto.EmailVerificationConfirmResponse;
import com.dongnemarket.auth.dto.EmailVerificationRequest;
import com.dongnemarket.auth.dto.EmailVerificationResponse;
import com.dongnemarket.auth.entity.EmailVerification;
import com.dongnemarket.auth.mail.EmailSender;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 회원가입 전 이메일 인증 코드의 생성·저장·재요청 쿨다운을 담당한다.
 * 실제 발송은 {@link EmailSender}에 위임한다(SMTP 등 발송 수단이 바뀌어도 이 클래스는 영향받지 않는다).
 */
@Service
@Transactional
public class EmailVerificationService {

	private static final long COOLDOWN_SECONDS = 60;
	private static final long CODE_TTL_MINUTES = 5;

	private final EmailVerificationRepository emailVerificationRepository;
	private final MemberRepository memberRepository;
	private final EmailSender emailSender;
	private final SecureRandom secureRandom = new SecureRandom();

	public EmailVerificationService(
			EmailVerificationRepository emailVerificationRepository,
			MemberRepository memberRepository,
			EmailSender emailSender) {
		this.emailVerificationRepository = emailVerificationRepository;
		this.memberRepository = memberRepository;
		this.emailSender = emailSender;
	}

	public EmailVerificationResponse requestVerification(EmailVerificationRequest request) {
		String email = request.getEmail();
		if (memberRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expiresAt = now.plusMinutes(CODE_TTL_MINUTES);
		String code = generateCode();

		emailVerificationRepository.findByEmail(email)
				.ifPresentOrElse(
						existing -> {
							if (existing.isCoolingDown(now, COOLDOWN_SECONDS)) {
								throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUEST_TOO_SOON);
							}
							existing.replace(code, now, expiresAt);
						},
						() -> emailVerificationRepository.save(EmailVerification.issue(email, code, now, expiresAt))
				);

		emailSender.send(email, "[마켓온] 이메일 인증 코드 안내", buildVerificationEmailBody(code));

		return new EmailVerificationResponse(email, expiresAt);
	}

	private String buildVerificationEmailBody(String code) {
		return "안녕하세요, 마켓온입니다.\n\n"
				+ "요청하신 이메일 인증 코드를 안내드립니다.\n\n"
				+ "인증 코드: " + code + "\n\n"
				+ "이 코드는 발급 시점으로부터 " + CODE_TTL_MINUTES + "분간 유효합니다.\n"
				+ "본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.\n\n"
				+ "감사합니다.\n"
				+ "마켓온 드림";
	}

	/**
	 * 인증 코드를 확인한다. 이미 인증 완료된 건에 같은 이메일로 재요청하면(중복 확인) 코드 검사 없이 그대로 성공을 반환한다(멱등).
	 */
	public EmailVerificationConfirmResponse confirmVerification(EmailVerificationConfirmRequest request) {
		String email = request.getEmail();
		EmailVerification verification = emailVerificationRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

		if (verification.isVerified()) {
			return new EmailVerificationConfirmResponse(email, true);
		}

		LocalDateTime now = LocalDateTime.now();
		if (verification.isExpired(now)) {
			throw new BusinessException(ErrorCode.EXPIRED_VERIFICATION_CODE);
		}
		if (!verification.matchesCode(request.getCode())) {
			throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
		}

		verification.verify(now);
		return new EmailVerificationConfirmResponse(email, true);
	}

	private String generateCode() {
		return String.format("%06d", secureRandom.nextInt(1_000_000));
	}
}
