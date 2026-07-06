package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.LoginRequest;
import com.dongnemarket.auth.dto.LoginResponse;
import com.dongnemarket.auth.dto.SignupRequest;
import com.dongnemarket.auth.dto.SignupResponse;
import com.dongnemarket.auth.dto.TokenResponse;
import com.dongnemarket.auth.entity.RefreshToken;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.auth.repository.RefreshTokenRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Repository만 Mock 처리하고, 외부 시스템 의존성이 없는 PasswordEncoder·JwtTokenProvider·RefreshTokenService는
 * 실제 구현체를 사용해 AuthService의 비즈니스 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	MemberRepository memberRepository;

	@Mock
	RefreshTokenRepository refreshTokenRepository;

	@Mock
	EmailVerificationRepository emailVerificationRepository;

	PasswordEncoder passwordEncoder;
	JwtTokenProvider jwtTokenProvider;
	RefreshTokenService refreshTokenService;
	AuthService authService;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		jwtTokenProvider = new JwtTokenProvider(
				"test-jwt-secret-key-for-auth-service-unit-test-0123456789", 3600L, 604800L);
		refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtTokenProvider);
		authService = new AuthService(
				memberRepository, passwordEncoder, jwtTokenProvider, refreshTokenService, emailVerificationRepository);
	}

	// ===== signup =====

	@Test
	@DisplayName("이메일·닉네임이 중복되지 않고 이메일 인증이 완료됐으면 회원가입에 성공한다")
	void signup_success() {
		SignupRequest request = new SignupRequest("test@example.com", "password123", "tester");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(emailVerificationRepository.existsByEmailAndVerifiedTrue(request.getEmail())).willReturn(true);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
		given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = authService.signup(request);

		assertThat(response.getEmail()).isEqualTo(request.getEmail());
		assertThat(response.getNickname()).isEqualTo(request.getNickname());
	}

	@Test
	@DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL 예외가 발생한다")
	void signup_duplicateEmail_throwsException() {
		SignupRequest request = new SignupRequest("test@example.com", "password123", "tester");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

		verify(memberRepository, never()).save(any());
	}

	@Test
	@DisplayName("이메일 인증을 완료하지 않았으면 EMAIL_NOT_VERIFIED 예외가 발생한다")
	void signup_emailNotVerified_throwsException() {
		SignupRequest request = new SignupRequest("unverified@example.com", "password123", "unverifiedUser");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(emailVerificationRepository.existsByEmailAndVerifiedTrue(request.getEmail())).willReturn(false);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);

		verify(memberRepository, never()).save(any());
	}

	@Test
	@DisplayName("이메일 인증 요청 이력 자체가 없으면 EMAIL_NOT_VERIFIED 예외가 발생한다")
	void signup_noVerificationHistory_throwsException() {
		SignupRequest request = new SignupRequest("never-requested@example.com", "password123", "neverRequestedUser");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(emailVerificationRepository.existsByEmailAndVerifiedTrue(request.getEmail())).willReturn(false);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);

		verify(memberRepository, never()).save(any());
	}

	@Test
	@DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외가 발생한다")
	void signup_duplicateNickname_throwsException() {
		SignupRequest request = new SignupRequest("test@example.com", "password123", "tester");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(emailVerificationRepository.existsByEmailAndVerifiedTrue(request.getEmail())).willReturn(true);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);

		verify(memberRepository, never()).save(any());
	}

	@Test
	@DisplayName("중복 체크 통과 후 save() 시점에 이메일 unique 제약을 위반하면(race condition) DUPLICATE_EMAIL로 변환한다")
	void signup_raceConditionDuplicateEmail_throwsDuplicateEmail() {
		SignupRequest request = new SignupRequest("race@example.com", "password123", "racer");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false, true);
		given(emailVerificationRepository.existsByEmailAndVerifiedTrue(request.getEmail())).willReturn(true);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
		given(memberRepository.save(any(Member.class))).willThrow(new DataIntegrityViolationException("duplicate entry"));

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
	}

	@Test
	@DisplayName("중복 체크 통과 후 save() 시점에 닉네임 unique 제약을 위반하면(race condition) DUPLICATE_NICKNAME으로 변환한다")
	void signup_raceConditionDuplicateNickname_throwsDuplicateNickname() {
		SignupRequest request = new SignupRequest("racer2@example.com", "password123", "raceNick");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(emailVerificationRepository.existsByEmailAndVerifiedTrue(request.getEmail())).willReturn(true);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(false, true);
		given(memberRepository.save(any(Member.class))).willThrow(new DataIntegrityViolationException("duplicate entry"));

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
	}

	@Test
	@DisplayName("원인을 식별할 수 없는 무결성 제약 위반은 원본 예외를 그대로 던진다")
	void signup_unclassifiableIntegrityViolation_rethrowsOriginal() {
		SignupRequest request = new SignupRequest("unknown@example.com", "password123", "unknown");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(emailVerificationRepository.existsByEmailAndVerifiedTrue(request.getEmail())).willReturn(true);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
		DataIntegrityViolationException original = new DataIntegrityViolationException("unknown constraint");
		given(memberRepository.save(any(Member.class))).willThrow(original);

		assertThatThrownBy(() -> authService.signup(request))
				.isSameAs(original);
	}

	// ===== login =====

	@Test
	@DisplayName("올바른 이메일·비밀번호로 로그인하면 memberId가 담긴 accessToken·refreshToken을 반환한다")
	void login_success() {
		LoginRequest request = new LoginRequest("test@example.com", "password123");
		Member member = Member.createUser(request.getEmail(), passwordEncoder.encode(request.getPassword()), "tester");
		ReflectionTestUtils.setField(member, "id", 1L);
		given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.empty());

		LoginResponse response = authService.login(request);

		assertThat(response.getAccessToken()).isNotBlank();
		assertThat(response.getRefreshToken()).isNotBlank();
		assertThat(jwtTokenProvider.getMemberId(response.getAccessToken())).isEqualTo(1L);
		assertThat(jwtTokenProvider.getMemberId(response.getRefreshToken())).isEqualTo(1L);
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("이미 Refresh Token이 저장된 회원이 재로그인하면 신규 저장 대신 기존 토큰을 교체한다")
	void login_existingRefreshToken_replacesInsteadOfInserting() {
		LoginRequest request = new LoginRequest("test@example.com", "password123");
		Member member = Member.createUser(request.getEmail(), passwordEncoder.encode(request.getPassword()), "tester");
		ReflectionTestUtils.setField(member, "id", 1L);
		given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
		RefreshToken existing = RefreshToken.issue(1L, "old-token", LocalDateTime.now().plusDays(7));
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of(existing));

		LoginResponse response = authService.login(request);

		assertThat(existing.getToken()).isEqualTo(response.getRefreshToken());
		verify(refreshTokenRepository, never()).save(any());
	}

	// ===== reissue =====

	@Test
	@DisplayName("유효한 Refresh Token으로 재발급하면 새 accessToken과 동일한 refreshToken을 반환한다")
	void reissue_success() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);
		Member member = Member.createUser("test@example.com", "encoded", "tester");
		ReflectionTestUtils.setField(member, "id", 1L);
		given(refreshTokenRepository.findByMemberId(1L))
				.willReturn(Optional.of(RefreshToken.issue(1L, refreshToken, LocalDateTime.now().plusDays(7))));
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		TokenResponse response = authService.reissue(refreshToken);

		assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
		assertThat(jwtTokenProvider.getMemberId(response.getAccessToken())).isEqualTo(1L);
	}

	@Test
	@DisplayName("만료된 Refresh Token으로 재발급하면 EXPIRED_REFRESH_TOKEN 예외가 발생한다")
	void reissue_expiredRefreshToken_throwsException() {
		JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
				"test-jwt-secret-key-for-auth-service-unit-test-0123456789", 3600L, 0L);
		String expiredToken = shortLivedProvider.createRefreshToken(1L);

		assertThatThrownBy(() -> authService.reissue(expiredToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("다른 키로 서명된(위조) Refresh Token으로 재발급하면 INVALID_REFRESH_TOKEN 예외가 발생한다")
	void reissue_forgedSignature_throwsInvalidRefreshToken() {
		JwtTokenProvider otherProvider = new JwtTokenProvider(
				"a-totally-different-secret-key-for-forgery-0123456789", 3600L, 604800L);
		String forgedToken = otherProvider.createRefreshToken(1L);

		assertThatThrownBy(() -> authService.reissue(forgedToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("DB에 저장된 Refresh Token이 없으면 REFRESH_TOKEN_NOT_FOUND 예외가 발생한다")
	void reissue_notFoundInDb_throwsRefreshTokenNotFound() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.reissue(refreshToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);
	}

	@Test
	@DisplayName("DB에 저장된 값과 다른 Refresh Token으로 재발급하면 INVALID_REFRESH_TOKEN 예외가 발생한다")
	void reissue_tokenMismatch_throwsInvalidRefreshToken() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);
		given(refreshTokenRepository.findByMemberId(1L))
				.willReturn(Optional.of(RefreshToken.issue(1L, "different-stored-token", LocalDateTime.now().plusDays(7))));

		assertThatThrownBy(() -> authService.reissue(refreshToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("Access Token으로 재발급을 시도하면 INVALID_REFRESH_TOKEN 예외가 발생한다")
	void reissue_accessTokenPresented_throwsInvalidRefreshToken() {
		String accessToken = jwtTokenProvider.createAccessToken(1L, "ROLE_USER");

		assertThatThrownBy(() -> authService.reissue(accessToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("탈퇴한 회원이 Refresh Token으로 재발급을 시도하면 DELETED_MEMBER 예외가 발생한다")
	void reissue_deletedMember_throwsException() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);
		Member member = Member.createUser("test@example.com", "encoded", "tester");
		ReflectionTestUtils.setField(member, "id", 1L);
		member.changeStatus(MemberStatus.DELETED);
		given(refreshTokenRepository.findByMemberId(1L))
				.willReturn(Optional.of(RefreshToken.issue(1L, refreshToken, LocalDateTime.now().plusDays(7))));
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.reissue(refreshToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_MEMBER);
	}

	@Test
	@DisplayName("정지된 회원이 Refresh Token으로 재발급을 시도하면 SUSPENDED_MEMBER 예외가 발생한다")
	void reissue_suspendedMember_throwsException() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);
		Member member = Member.createUser("test@example.com", "encoded", "tester");
		ReflectionTestUtils.setField(member, "id", 1L);
		member.changeStatus(MemberStatus.SUSPENDED);
		given(refreshTokenRepository.findByMemberId(1L))
				.willReturn(Optional.of(RefreshToken.issue(1L, refreshToken, LocalDateTime.now().plusDays(7))));
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.reissue(refreshToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUSPENDED_MEMBER);
	}

	// ===== logout =====

	@Test
	@DisplayName("로그아웃하면 저장된 Refresh Token이 삭제된다")
	void logout_callsRefreshTokenServiceDeleteByMemberId() {
		authService.logout(1L);

		verify(refreshTokenRepository).deleteByMemberId(1L);
	}

	@Test
	@DisplayName("로그아웃을 여러 번 호출해도 항상 성공한다(멱등)")
	void logout_calledTwice_bothSucceedWithoutException() {
		authService.logout(1L);

		assertThatCode(() -> authService.logout(1L)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("로그아웃 이후 기존 Refresh Token으로 재발급을 시도하면 REFRESH_TOKEN_NOT_FOUND 예외가 발생한다")
	void reissue_afterLogout_throwsRefreshTokenNotFound() {
		LoginRequest request = new LoginRequest("test@example.com", "password123");
		Member member = Member.createUser(request.getEmail(), passwordEncoder.encode(request.getPassword()), "tester");
		ReflectionTestUtils.setField(member, "id", 1L);
		given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
		// 로그인 시점에는 저장된 row가 없어 신규 저장되고, 로그아웃(삭제) 이후 재발급 시점에도 row가 없는 상태를 그대로 재현한다.
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.empty());

		LoginResponse loginResponse = authService.login(request);
		String refreshToken = loginResponse.getRefreshToken();

		authService.logout(1L);

		assertThatThrownBy(() -> authService.reissue(refreshToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);
	}

	@Test
	@DisplayName("존재하지 않는 이메일로 로그인하면 MEMBER_NOT_FOUND 예외가 발생한다")
	void login_emailNotFound_throwsException() {
		LoginRequest request = new LoginRequest("none@example.com", "password123");
		given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("비밀번호가 일치하지 않으면 INVALID_PASSWORD 예외가 발생한다")
	void login_wrongPassword_throwsException() {
		LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");
		Member member = Member.createUser(request.getEmail(), passwordEncoder.encode("password123"), "tester");
		given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
	}

	@Test
	@DisplayName("탈퇴한 회원이 로그인하면 DELETED_MEMBER 예외가 발생한다")
	void login_deletedMember_throwsException() {
		LoginRequest request = new LoginRequest("deleted@example.com", "password123");
		Member member = Member.createUser(request.getEmail(), passwordEncoder.encode(request.getPassword()), "tester");
		member.changeStatus(MemberStatus.DELETED);
		given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_MEMBER);
	}

	@Test
	@DisplayName("정지된 회원이 로그인하면 SUSPENDED_MEMBER 예외가 발생한다")
	void login_suspendedMember_throwsException() {
		LoginRequest request = new LoginRequest("suspended@example.com", "password123");
		Member member = Member.createUser(request.getEmail(), passwordEncoder.encode(request.getPassword()), "tester");
		member.changeStatus(MemberStatus.SUSPENDED);
		given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUSPENDED_MEMBER);
	}
}
