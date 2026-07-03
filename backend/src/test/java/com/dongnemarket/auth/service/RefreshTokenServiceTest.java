package com.dongnemarket.auth.service;

import com.dongnemarket.auth.entity.RefreshToken;
import com.dongnemarket.auth.repository.RefreshTokenRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	RefreshTokenRepository refreshTokenRepository;

	JwtTokenProvider jwtTokenProvider;
	RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider(
				"test-jwt-secret-key-for-refresh-token-service-unit-test-0123456789", 3600L, 604800L);
		refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtTokenProvider);
	}

	// ===== saveOrReplace =====

	@Test
	@DisplayName("저장된 Refresh Token이 없으면 새로 저장한다")
	void saveOrReplace_noExisting_insertsNew() {
		String token = jwtTokenProvider.createRefreshToken(1L);
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.empty());

		refreshTokenService.saveOrReplace(1L, token);

		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("이미 저장된 Refresh Token이 있으면 신규 저장 대신 기존 row를 교체한다")
	void saveOrReplace_existing_replacesInPlace() {
		String token = jwtTokenProvider.createRefreshToken(1L);
		RefreshToken existing = RefreshToken.issue(1L, "old-token", LocalDateTime.now().minusDays(1));
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of(existing));

		refreshTokenService.saveOrReplace(1L, token);

		assertThat(existing.getToken()).isEqualTo(token);
		verify(refreshTokenRepository, never()).save(any());
	}

	// ===== validateAndGetMemberId =====

	@Test
	@DisplayName("서명·만료가 유효하고 DB 저장값과 일치하면 memberId를 반환한다")
	void validateAndGetMemberId_success() {
		String token = jwtTokenProvider.createRefreshToken(1L);
		given(refreshTokenRepository.findByMemberId(1L))
				.willReturn(Optional.of(RefreshToken.issue(1L, token, LocalDateTime.now().plusDays(7))));

		Long memberId = refreshTokenService.validateAndGetMemberId(token);

		assertThat(memberId).isEqualTo(1L);
	}

	@Test
	@DisplayName("만료된 토큰이면 EXPIRED_REFRESH_TOKEN 예외가 발생한다")
	void validateAndGetMemberId_expired_throwsExpiredRefreshToken() {
		JwtTokenProvider expiredProvider = new JwtTokenProvider(
				"test-jwt-secret-key-for-refresh-token-service-unit-test-0123456789", 3600L, 0L);
		String expiredToken = expiredProvider.createRefreshToken(1L);

		assertThatThrownBy(() -> refreshTokenService.validateAndGetMemberId(expiredToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("다른 키로 서명된(위조) 토큰이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
	void validateAndGetMemberId_forgedSignature_throwsInvalidRefreshToken() {
		JwtTokenProvider otherProvider = new JwtTokenProvider(
				"a-totally-different-secret-key-for-forgery-0123456789", 3600L, 604800L);
		String forgedToken = otherProvider.createRefreshToken(1L);

		assertThatThrownBy(() -> refreshTokenService.validateAndGetMemberId(forgedToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("서명·만료는 정상이나 DB에 저장된 row가 없으면 REFRESH_TOKEN_NOT_FOUND 예외가 발생한다")
	void validateAndGetMemberId_notFoundInDb_throwsRefreshTokenNotFound() {
		String token = jwtTokenProvider.createRefreshToken(1L);
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> refreshTokenService.validateAndGetMemberId(token))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);
	}

	@Test
	@DisplayName("DB에 저장된 값과 다른 토큰이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
	void validateAndGetMemberId_tokenMismatch_throwsInvalidRefreshToken() {
		String token = jwtTokenProvider.createRefreshToken(1L);
		given(refreshTokenRepository.findByMemberId(1L))
				.willReturn(Optional.of(RefreshToken.issue(1L, "different-stored-token", LocalDateTime.now().plusDays(7))));

		assertThatThrownBy(() -> refreshTokenService.validateAndGetMemberId(token))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
	}
}
