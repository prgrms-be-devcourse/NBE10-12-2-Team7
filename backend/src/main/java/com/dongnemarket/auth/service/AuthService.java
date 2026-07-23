package com.dongnemarket.auth.service;

import com.dongnemarket.auth.client.OAuthAuthorizationUrlFactory;
import com.dongnemarket.auth.client.OAuthClient;
import com.dongnemarket.auth.client.OAuthUserIdentity;
import com.dongnemarket.auth.dto.LoginRequest;
import com.dongnemarket.auth.dto.LoginResponse;
import com.dongnemarket.auth.dto.OAuthAuthorizationStart;
import com.dongnemarket.auth.dto.SignupRequest;
import com.dongnemarket.auth.dto.SignupResponse;
import com.dongnemarket.auth.dto.TokenResponse;
import com.dongnemarket.auth.entity.MemberSocialAccount;
import com.dongnemarket.auth.entity.OAuthProvider;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.auth.repository.MemberSocialAccountRepository;
import com.dongnemarket.auth.repository.OAuthAuthorizationState;
import com.dongnemarket.auth.repository.OAuthStateRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.AgreementType;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberAgreement;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberAgreementRepository;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

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
	private final OAuthStateRepository oauthStateRepository;
	private final MemberSocialAccountRepository memberSocialAccountRepository;
	private final OAuthSignupTransaction oauthSignupTransaction;
	private final OAuthAuthorizationUrlFactory oauthAuthorizationUrlFactory;
	private final Duration oauthStateTtl;
	private final int oauthMaxPendingPerBrowser;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService,
			EmailVerificationRepository emailVerificationRepository,
			MemberAgreementRepository memberAgreementRepository,
			LoginAttemptService loginAttemptService,
			OAuthStateRepository oauthStateRepository,
			MemberSocialAccountRepository memberSocialAccountRepository,
			OAuthSignupTransaction oauthSignupTransaction,
			OAuthAuthorizationUrlFactory oauthAuthorizationUrlFactory,
			@Value("${oauth.authorization-state.ttl-seconds}") long oauthStateTtlSeconds,
			@Value("${oauth.authorization-state.max-pending-per-browser}") int oauthMaxPendingPerBrowser) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenService = refreshTokenService;
		this.emailVerificationRepository = emailVerificationRepository;
		this.memberAgreementRepository = memberAgreementRepository;
		this.loginAttemptService = loginAttemptService;
		this.oauthStateRepository = oauthStateRepository;
		this.memberSocialAccountRepository = memberSocialAccountRepository;
		this.oauthSignupTransaction = oauthSignupTransaction;
		this.oauthAuthorizationUrlFactory = oauthAuthorizationUrlFactory;
		this.oauthStateTtl = Duration.ofSeconds(oauthStateTtlSeconds);
		this.oauthMaxPendingPerBrowser = oauthMaxPendingPerBrowser;
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
			// 소셜 전용 회원은 "존재하지 않는 이메일"과 완전히 같은 경로(BCrypt 호출 없이 즉시 MEMBER_NOT_FOUND)로
			// 합류시킨다 — INVALID_PASSWORD 쪽으로 보내면 "회원이 존재한다"는 신호가 응답/타이밍으로 새어나간다.
			if (!member.isLocalLoginEnabled()) {
				throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
			}

			validateActiveStatus(member);
			if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
				throw new BusinessException(ErrorCode.INVALID_PASSWORD);
			}

			LoginResponse response = issueTokens(member);
			loginAttemptService.recordSuccess(email);
			return response;
		} catch (BusinessException e) {
			if (e.getErrorCode() == ErrorCode.MEMBER_NOT_FOUND || e.getErrorCode() == ErrorCode.INVALID_PASSWORD) {
				loginAttemptService.recordFailure(email);
			}
			throw e;
		}
	}

	/**
	 * 소셜 로그인 인가를 시작한다: state·PKCE code_verifier·(구글만) OIDC nonce를 발급해 Redis에 저장하고,
	 * 프론트가 그대로 리다이렉트할 수 있는 완성된 인가 URL을 돌려준다.
	 * <p>DB를 전혀 쓰지 않으므로(Redis만) 트랜잭션이 필요 없다 — 클래스 레벨
	 * {@code @Transactional(readOnly = true)}가 걸려도 문제되진 않지만, 의미상 맞지 않아 명시적으로 뺀다.
	 *
	 * @param browserCorrelationHash Controller가 {@code oauth_bcid} 쿠키 원문을 해시해 넘긴 값
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public OAuthAuthorizationStart startAuthorization(OAuthClient client, String browserCorrelationHash) {
		String state = generateUrlSafeRandom(32);
		String codeVerifier = generateUrlSafeRandom(64);
		String codeChallenge = base64UrlSha256(codeVerifier);
		String oidcNonce = client.provider() == OAuthProvider.GOOGLE ? generateUrlSafeRandom(32) : null;
		String redirectUri = oauthAuthorizationUrlFactory.redirectUri(client.provider());

		OAuthAuthorizationState value = new OAuthAuthorizationState(
				client.provider(), browserCorrelationHash, redirectUri, codeVerifier, oidcNonce, Instant.now());
		boolean issued = oauthStateRepository.issue(state, value, oauthStateTtl, oauthMaxPendingPerBrowser);
		if (!issued) {
			throw new BusinessException(ErrorCode.TOO_MANY_OAUTH_ATTEMPTS);
		}

		String authorizationUrl = oauthAuthorizationUrlFactory.build(client.provider(), state, codeChallenge, oidcNonce);
		return new OAuthAuthorizationStart(authorizationUrl, state, oauthStateTtl.toSeconds());
	}

	private String generateUrlSafeRandom(int bytes) {
		byte[] value = new byte[bytes];
		secureRandom.nextBytes(value);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	/** PKCE S256: code_challenge = BASE64URL(SHA256(code_verifier)) */
	private String base64UrlSha256(String codeVerifier) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(codeVerifier.getBytes());
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
		}
	}

	/**
	 * 소셜 로그인. state 소비(Redis, 원자적) → 제공자 토큰교환/신원확인(네트워크, DB 트랜잭션 밖) →
	 * 기존 연동 조회 또는 신규가입 → 토큰 발급 순으로 진행한다.
	 * <p>{@code Propagation.NOT_SUPPORTED}로 이 메서드 자체는 트랜잭션을 열지 않는다 — 클래스 레벨
	 * {@code @Transactional(readOnly = true)}를 상속받으면 네트워크 호출(제공자 토큰교환)이 DB 커넥션을
	 * 붙든 채로 일어나고, {@link OAuthSignupTransaction}의 두 메서드가 서로 다른 트랜잭션으로 실행돼야
	 * 하는 요구사항도 깨진다(상위 트랜잭션이 있으면 REQUIRED 전파로 같은 트랜잭션에 합류해버린다).
	 *
	 * @param client 호출할 provider의 {@link OAuthClient}. Controller가 리터럴 엔드포인트별로 고정해서 넘긴다
	 *     (provider가 사용자 입력으로 결정되는 지점이 없다).
	 * @param browserCorrelationHash {@code oauth_bcid} 쿠키 원문을 해시한 값
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public LoginResponse oauthLogin(OAuthClient client, String code, String state, String browserCorrelationHash) {
		OAuthAuthorizationState authState = oauthStateRepository.consume(state, client.provider(), browserCorrelationHash)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OAUTH_STATE));

		// state는 이미 소비됐다 — 아래에서 제공자 호출이 실패(timeout/장애)해도 이 state는 복구·재사용하지
		// 않는다. 사용자는 새 OAuth 흐름을 처음부터 다시 시작해야 한다.
		OAuthUserIdentity identity = client.resolveIdentity(
				code, authState.codeVerifier(), authState.redirectUri(), authState.oidcNonce());

		Member member = memberSocialAccountRepository
				.findByProviderAndProviderUserIdFetchMember(identity.provider(), identity.providerUserId())
				.map(MemberSocialAccount::getMember)
				.orElseGet(() -> signUpOrReconcile(identity));

		validateActiveStatus(member);
		return issueTokens(member);
	}

	/**
	 * 신규 소셜 가입을 시도한다. 사전 이메일 존재 체크는 흔한 경우를 빠르게 걸러내는 최적화일 뿐,
	 * 정확성의 근거가 아니다 — 실제 동시성 안전성은 DB UNIQUE 제약 + 위반 시 재조회에서 나온다.
	 * <p>{@link OAuthSignupTransaction#signUp}이 UNIQUE 위반으로 예외를 던지면 그 트랜잭션은 이미
	 * 완전히 롤백된 상태다(rollback-only 트랜잭션 안에서 재조회하지 않는다). 이 메서드(트랜잭션 없음)가
	 * 그 예외를 받아 {@link OAuthSignupTransaction#reconcileAfterConflict}로 새 read-only 트랜잭션을
	 * 연다 — 동일 연동이 동시 요청으로 먼저 만들어졌으면 그 회원으로 로그인을 이어가고, 아니면(이메일 등
	 * 다른 제약 충돌) 공통 에러로 응답한다. 예외 메시지나 DB 벤더별 문구에는 의존하지 않는다.
	 */
	private Member signUpOrReconcile(OAuthUserIdentity identity) {
		if (memberRepository.existsByEmail(identity.email())) {
			throw new BusinessException(ErrorCode.OAUTH_EMAIL_CONFLICT);
		}
		try {
			return oauthSignupTransaction.signUp(identity);
		} catch (DataIntegrityViolationException e) {
			return oauthSignupTransaction.reconcileAfterConflict(identity.provider(), identity.providerUserId())
					.orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_EMAIL_CONFLICT));
		}
	}

	private LoginResponse issueTokens(Member member) {
		String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
		refreshTokenService.saveOrReplace(member.getId(), refreshToken);
		return LoginResponse.of(accessToken, refreshToken);
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
