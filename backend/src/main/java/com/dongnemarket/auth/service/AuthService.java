package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.LoginRequest;
import com.dongnemarket.auth.dto.LoginResponse;
import com.dongnemarket.auth.dto.SignupRequest;
import com.dongnemarket.auth.dto.SignupResponse;
import com.dongnemarket.auth.dto.TokenResponse;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.AgreementType;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberAgreement;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberAgreementRepository;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AuthService {

	/** 약관/개인정보 동의 버전. 별도 버전 관리 테이블 없이 우선 고정값으로 둔다(이후 약관 개정 시 재검토). */
	private static final String AGREEMENT_VERSION = "v1.0";

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	private final EmailVerificationRepository emailVerificationRepository;
	private final MemberAgreementRepository memberAgreementRepository;
	private final LoginAttemptService loginAttemptService;

	public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService,
			EmailVerificationRepository emailVerificationRepository,
			MemberAgreementRepository memberAgreementRepository,
			LoginAttemptService loginAttemptService) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenService = refreshTokenService;
		this.emailVerificationRepository = emailVerificationRepository;
		this.memberAgreementRepository = memberAgreementRepository;
		this.loginAttemptService = loginAttemptService;
	}

	/**
	 * @param ipAddress 약관 동의 이력 증적용. 요청자 식별 목적이 아니라 동의 시점 증빙 목적이다.
	 * @param userAgent 약관 동의 이력 증적용(위와 동일한 목적).
	 */
	@Transactional
	public SignupResponse signup(SignupRequest request, String ipAddress, String userAgent) {
		if (!request.isTermsAgreed()) {
			throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
		}
		if (!request.isPersonalInfoCollectionAgreed()) {
			throw new BusinessException(ErrorCode.PERSONAL_INFO_COLLECTION_NOT_AGREED);
		}
		if (memberRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}
		if (!emailVerificationRepository.existsByEmailAndVerifiedTrue(request.getEmail())) {
			throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
		}
		if (memberRepository.existsByNickname(request.getNickname())) {
			throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
		}

		String encodedPassword = passwordEncoder.encode(request.getPassword());
		Member member = Member.createUser(request.getEmail(), encodedPassword, request.getNickname());

		try {
			Member savedMember = memberRepository.save(member);
			saveAgreements(savedMember, ipAddress, userAgent);
			return SignupResponse.from(savedMember);
		} catch (DataIntegrityViolationException e) {
			throw resolveDuplicateException(request, e);
		}
	}

	/** 필수 동의 항목(이용약관/개인정보 수집·이용) 각각을 별도 이력 row로 저장한다. */
	private void saveAgreements(Member member, String ipAddress, String userAgent) {
		LocalDateTime agreedAt = LocalDateTime.now();
		memberAgreementRepository.save(MemberAgreement.of(
				member, AgreementType.TERMS_OF_SERVICE, AGREEMENT_VERSION, agreedAt, ipAddress, userAgent));
		memberAgreementRepository.save(MemberAgreement.of(
				member, AgreementType.PERSONAL_INFO_COLLECTION, AGREEMENT_VERSION, agreedAt, ipAddress, userAgent));
	}

	/**
	 * 로그인 실패(이메일 없음/비밀번호 불일치)만 실패 횟수에 반영한다 — 탈퇴/정지 회원 거부는 자격증명 추측
	 * 신호가 아니므로 카운트하지 않는다. 임계값 도달 시 이후 로그인은 자격증명 확인 전에 즉시 차단된다.
	 */
	@Transactional
	public LoginResponse login(LoginRequest request) {
		String email = request.getEmail();
		loginAttemptService.assertNotBlocked(email);
		try {
			Member member = memberRepository.findByEmail(email)
					.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

			validateActiveStatus(member);
			if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
				throw new BusinessException(ErrorCode.INVALID_PASSWORD);
			}

			String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
			String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
			refreshTokenService.saveOrReplace(member.getId(), refreshToken);
			loginAttemptService.recordSuccess(email);

			return LoginResponse.of(accessToken, refreshToken);
		} catch (BusinessException e) {
			if (e.getErrorCode() == ErrorCode.MEMBER_NOT_FOUND || e.getErrorCode() == ErrorCode.INVALID_PASSWORD) {
				loginAttemptService.recordFailure(email);
			}
			throw e;
		}
	}

	/**
	 * Refresh Token을 검증하고 Access Token과 Refresh Token을 함께 재발급한다(Rotation).
	 * <p>기존 Refresh Token은 검증 즉시 저장소에서 새 값으로 교체돼 무효화된다 — 탈취된 옛 토큰이 재사용되면
	 * (이미 교체된 뒤라) 저장값과 불일치해 실패하므로, 재사용을 탐지하는 효과도 있다.
	 */
	@Transactional
	public TokenResponse reissue(String refreshToken) {
		Long memberId = refreshTokenService.validateAndGetMemberId(refreshToken);
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		validateActiveStatus(member);

		String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId());
		refreshTokenService.saveOrReplace(member.getId(), newRefreshToken);
		return TokenResponse.of(newAccessToken, newRefreshToken);
	}

	/**
	 * 로그아웃: 저장된 Refresh Token만 삭제한다(멱등 — 여러 번 호출해도 항상 성공).
	 * <p>Access Token 자체는 서버에서 즉시 무효화하지 않는다(Stateless JWT 정책 유지) —
	 * 이미 발급된 Access Token은 만료 시각(최대 15분)까지 그대로 유효하며, 그 사이 재발급만 막힌다.
	 */
	@Transactional
	public void logout(Long memberId) {
		refreshTokenService.deleteByMemberId(memberId);
	}

	/** 탈퇴/정지 회원은 로그인/재발급 모두 불가 (login()과 reissue()의 정책을 일관되게 유지) */
	private void validateActiveStatus(Member member) {
		if (member.getStatus() == MemberStatus.DELETED) {
			throw new BusinessException(ErrorCode.DELETED_MEMBER);
		}
		if (member.getStatus() == MemberStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.SUSPENDED_MEMBER);
		}
	}

	/** 중복 체크 이후 save() 사이의 race condition으로 unique 제약을 위반한 경우, 원인을 재조회해 알맞은 BusinessException으로 변환한다. */
	private BusinessException resolveDuplicateException(SignupRequest request, DataIntegrityViolationException e) {
		if (memberRepository.existsByEmail(request.getEmail())) {
			return new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}
		if (memberRepository.existsByNickname(request.getNickname())) {
			return new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
		}
		throw e;
	}
}
