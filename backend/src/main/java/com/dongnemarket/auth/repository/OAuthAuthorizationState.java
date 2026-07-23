package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.OAuthProvider;

import java.time.Instant;

/**
 * OAuth 인가 시작 시점에 발급되어 Redis에 저장되는 1회용 상태.
 * @param oidcNonce Google ID Token의 nonce 클레임 검증용. 카카오는 서명 ID Token을 쓰지 않으므로 빈 문자열.
 */
public record OAuthAuthorizationState(
		OAuthProvider provider,
		String browserCorrelationHash,
		String redirectUri,
		String codeVerifier,
		String oidcNonce,
		Instant issuedAt) {
}
