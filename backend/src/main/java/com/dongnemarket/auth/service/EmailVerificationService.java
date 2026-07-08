package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.EmailVerificationConfirmRequest;
import com.dongnemarket.auth.dto.EmailVerificationConfirmResponse;
import com.dongnemarket.auth.dto.EmailVerificationRequest;
import com.dongnemarket.auth.dto.EmailVerificationResponse;
import com.dongnemarket.auth.entity.EmailVerification;
import com.dongnemarket.auth.mail.EmailSender;
import com.dongnemarket.auth.repository.EmailVerificationCodeRepository;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 회원가입 전 이메일 인증 코드의 생성·저장·재요청 쿨다운을 담당한다.
 * 실제 발송은 {@link EmailSender}에 위임한다(SMTP 등 발송 수단이 바뀌어도 이 클래스는 영향받지 않는다).
 * <p>코드 자체(발급·비교·쿨다운·만료)는 TTL 데이터라 {@link EmailVerificationCodeRepository}(Redis)가 담당하고,
 * "인증 완료" 여부만 {@link EmailVerificationRepository}(DB)에 남긴다 — 회원가입은 코드 TTL과 무관하게
 * 이 완료 상태를 확인하기 때문이다({@code AuthService.signup} 참고).
 */
@Service
@Transactional
public class EmailVerificationService {

	private static final long COOLDOWN_SECONDS = 60;
	private static final long CODE_TTL_MINUTES = 5;

	private final EmailVerificationCodeRepository emailVerificationCodeRepository;
	private final EmailVerificationRepository emailVerificationRepository;
	private final MemberRepository memberRepository;
	private final EmailSender emailSender;
	private final SecureRandom secureRandom = new SecureRandom();

	public EmailVerificationService(
			EmailVerificationCodeRepository emailVerificationCodeRepository,
			EmailVerificationRepository emailVerificationRepository,
			MemberRepository memberRepository,
			EmailSender emailSender) {
		this.emailVerificationCodeRepository = emailVerificationCodeRepository;
		this.emailVerificationRepository = emailVerificationRepository;
		this.memberRepository = memberRepository;
		this.emailSender = emailSender;
	}

	public EmailVerificationResponse requestVerification(EmailVerificationRequest request) {
		String email = request.getEmail();
		if (memberRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		Duration ttl = Duration.ofMinutes(CODE_TTL_MINUTES);
		emailVerificationCodeRepository.getRemainingTtl(email).ifPresent(remaining -> {
			if (remaining.compareTo(ttl.minusSeconds(COOLDOWN_SECONDS)) > 0) {
				throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUEST_TOO_SOON);
			}
		});

		String code = generateCode();
		emailVerificationCodeRepository.save(email, code, ttl);
		// 새 코드를 발급했으니 이전 인증 상태(있었다면)는 무효화 — 새 코드에 대해 다시 인증해야 한다.
		emailVerificationRepository.findByEmail(email).ifPresent(EmailVerification::unverify);

		emailSender.send(email, "[마켓온] 이메일 인증 코드 안내", buildVerificationEmailBody(code));

		return new EmailVerificationResponse(email, LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
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
		if (emailVerificationRepository.existsByEmailAndVerifiedTrue(email)) {
			return new EmailVerificationConfirmResponse(email, true);
		}

		String storedCode = emailVerificationCodeRepository.findCode(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));
		if (!storedCode.equals(request.getCode())) {
			throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
		}

		emailVerificationCodeRepository.delete(email);
		LocalDateTime now = LocalDateTime.now();
		emailVerificationRepository.findByEmail(email)
				.ifPresentOrElse(
						verification -> verification.verify(now),
						() -> emailVerificationRepository.save(EmailVerification.verified(email, now))
				);

		return new EmailVerificationConfirmResponse(email, true);
	}

	private String generateCode() {
		return String.format("%06d", secureRandom.nextInt(1_000_000));
	}
}
