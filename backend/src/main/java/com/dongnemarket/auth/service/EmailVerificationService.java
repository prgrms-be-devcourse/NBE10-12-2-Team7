package com.dongnemarket.auth.service;

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

		emailSender.send(email, "[동네마켓] 이메일 인증 코드", "인증 코드: " + code + " (5분 이내에 입력해주세요)");

		return new EmailVerificationResponse(email, expiresAt);
	}

	private String generateCode() {
		return String.format("%06d", secureRandom.nextInt(1_000_000));
	}
}
