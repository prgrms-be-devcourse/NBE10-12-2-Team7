package com.dongnemarket.auth.service;

import com.dongnemarket.auth.entity.RefreshToken;
import com.dongnemarket.auth.repository.RefreshTokenRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Refresh Token의 저장·교체·검증을 전담한다.
 * <p>회원당 1개만 유지하며(단일 세션), 재로그인/재발급 시 기존 토큰을 교체한다.
 */
@Service
@Transactional
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider jwtTokenProvider;

	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtTokenProvider jwtTokenProvider) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	/** 로그인 성공 시 호출: 기존 row가 있으면 교체, 없으면 신규 저장 */
	public void saveOrReplace(Long memberId, String token) {
		LocalDateTime expiresAt = jwtTokenProvider.getExpiration(token);
		refreshTokenRepository.findByMemberId(memberId)
				.ifPresentOrElse(
						existing -> existing.replace(token, expiresAt),
						() -> refreshTokenRepository.save(RefreshToken.issue(memberId, token, expiresAt))
				);
	}

	/**
	 * Refresh Token을 검증하고 memberId를 반환한다.
	 * 검증 순서: 서명/만료(JWT) → Refresh Token 타입 여부(Access Token 오용 방지) → DB에 저장된 row 존재 → 저장값과 문자열 일치.
	 */
	@Transactional(readOnly = true)
	public Long validateAndGetMemberId(String refreshToken) {
		Long memberId;
		try {
			memberId = jwtTokenProvider.getMemberId(refreshToken);
			if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
				throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
			}
		} catch (ExpiredJwtException e) {
			throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		RefreshToken saved = refreshTokenRepository.findByMemberId(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
		if (!saved.matches(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		return memberId;
	}
}
