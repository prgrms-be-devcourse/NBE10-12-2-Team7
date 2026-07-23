package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.PasswordResetConfirmRequest;
import com.dongnemarket.auth.dto.PasswordResetRequest;
import com.dongnemarket.auth.mail.EmailSender;
import com.dongnemarket.auth.repository.PasswordResetTokenRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 비밀번호 찾기(재설정 이메일 링크) 요청/확인을 담당한다.
 * 계정 존재 여부를 외부에 노출하지 않기 위해 요청 API는 가입 여부·탈퇴 여부와 무관하게 항상 같은 응답을 반환한다
 * (실제 토큰 발급·발송은 활성/정지 회원에게만 조용히 수행된다).
 * <p>원문 토큰은 이메일 링크로만 전달되고 저장소에는 SHA-256 해시만 저장한다.
 * <p>1회용·TTL 데이터라 {@link PasswordResetTokenRepository}(Redis)에 담긴다 — 별도 정리(cleanup) 작업 없이
 * TTL 만료로 자연 삭제된다.
 */
@Service
@Transactional
public class PasswordResetService {

	private static final long COOLDOWN_SECONDS = 60;
	private static final long TOKEN_TTL_MINUTES = 30;
	private static final String RESET_PAGE_PATH = "/password-reset";

	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;
	private final EmailSender emailSender;
	private final String frontendBaseUrl;
	private final SecureRandom secureRandom = new SecureRandom();

	public PasswordResetService(
			PasswordResetTokenRepository passwordResetTokenRepository,
			MemberRepository memberRepository,
			PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokenService,
			EmailSender emailSender,
			@Value("${auth.frontend-base-url}") String frontendBaseUrl) {
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
		this.emailSender = emailSender;
		this.frontendBaseUrl = frontendBaseUrl;
	}

	public void requestReset(PasswordResetRequest request) {
		memberRepository.findByEmail(request.getEmail())
				.filter(member -> member.getStatus() != MemberStatus.DELETED)
				.filter(Member::isLocalLoginEnabled)
				.ifPresent(this::issueAndSendTokenIfNotCoolingDown);
	}

	private void issueAndSendTokenIfNotCoolingDown(Member member) {
		Long memberId = member.getId();
		Duration ttl = Duration.ofMinutes(TOKEN_TTL_MINUTES);

		Optional<String> existingHash = passwordResetTokenRepository.findTokenHashByMemberId(memberId);
		if (existingHash.isPresent()) {
			Duration remaining = passwordResetTokenRepository.getRemainingTtlByMemberId(memberId).orElse(Duration.ZERO);
			if (remaining.compareTo(ttl.minusSeconds(COOLDOWN_SECONDS)) > 0) {
				return;
			}
			// 재요청(쿨다운 경과): 이전 토큰은 즉시 무효화하고 새 토큰으로 교체한다.
			passwordResetTokenRepository.deleteByTokenHash(existingHash.get());
		}

		String rawToken = generateRawToken();
		String tokenHash = hash(rawToken);
		passwordResetTokenRepository.save(memberId, tokenHash, ttl);

		emailSender.send(member.getEmail(), "[마켓온] 비밀번호 재설정 안내", buildResetEmailBody(rawToken));
	}

	private String buildResetEmailBody(String rawToken) {
		String resetLink = frontendBaseUrl + RESET_PAGE_PATH + "?token=" + rawToken;
		return "안녕하세요, 마켓온입니다.\n\n"
				+ "비밀번호 재설정을 요청하셨습니다. 아래 링크를 클릭해 새 비밀번호를 설정해주세요.\n\n"
				+ resetLink + "\n\n"
				+ "이 링크는 발급 시점으로부터 " + TOKEN_TTL_MINUTES + "분간 유효하며, 1회만 사용할 수 있습니다.\n"
				+ "본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.\n\n"
				+ "감사합니다.\n"
				+ "마켓온 드림";
	}

	public void confirmReset(PasswordResetConfirmRequest request) {
		String tokenHash = hash(request.getToken());
		Long memberId = passwordResetTokenRepository.findMemberIdByTokenHash(tokenHash)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_TOKEN));

		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		// 정상 흐름상 도달 불가(소셜 전용 회원에게는 애초에 토큰이 발급되지 않는다) — 방어적 가드.
		if (!member.isLocalLoginEnabled()) {
			throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
		}

		member.changePassword(passwordEncoder.encode(request.getNewPassword()));
		passwordResetTokenRepository.deleteByTokenHash(tokenHash);
		passwordResetTokenRepository.deleteByMemberId(memberId);
		refreshTokenService.deleteByMemberId(member.getId());
	}

	/** 링크에 담을 원문 토큰(256비트 무작위값, URL-safe). 저장소에는 해시만 남기고 원문은 저장하지 않는다. */
	private String generateRawToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(rawToken.getBytes());
			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
		}
	}
}
