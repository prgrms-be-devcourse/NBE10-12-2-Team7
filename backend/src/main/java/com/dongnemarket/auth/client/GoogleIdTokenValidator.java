package com.dongnemarket.auth.client;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.List;

/**
 * 구글 ID Token(JWS)을 로컬에서 검증한다. JWKS 공개키로 서명을 확인하므로 매 로그인마다 구글에
 * 원격 검증(tokeninfo)을 호출하지 않는다(JWKS는 {@link NimbusJwtDecoder}가 캐싱한다).
 * <p>검증 항목: RS256 서명, exp, issuer, audience(client id), {@code email_verified == true}.
 * nonce는 별도로(Redis state record와) 상수시간 비교한다 — 검증 실패 시 어떤 조건이 틀렸는지 구분해
 * 노출하지 않고 {@link ErrorCode#OAUTH_AUTHORIZATION_FAILED}로 통일한다.
 */
@Component
public class GoogleIdTokenValidator {

	private final JwtDecoder jwtDecoder;

	public GoogleIdTokenValidator(
			@Value("${oauth.google.jwk-set-uri}") String jwkSetUri,
			@Value("${oauth.google.issuer}") String issuer,
			@Value("${oauth.google.client-id}") String clientId) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
				.jwsAlgorithm(SignatureAlgorithm.RS256)
				.build();

		OAuth2TokenValidator<Jwt> withTimestampAndIssuer = JwtValidators.createDefaultWithIssuer(issuer);
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
				JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId));
		OAuth2TokenValidator<Jwt> emailVerifiedValidator = new JwtClaimValidator<Boolean>(
				"email_verified", Boolean.TRUE::equals);

		decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
				withTimestampAndIssuer, audienceValidator, emailVerifiedValidator));
		this.jwtDecoder = decoder;
	}

	/** @param expectedNonce Redis state record에 저장된 oidcNonce(평문) */
	public Jwt validate(String idToken, String expectedNonce) {
		Jwt jwt;
		try {
			jwt = jwtDecoder.decode(idToken);
		} catch (JwtException e) {
			throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
		}

		String nonceClaim = jwt.getClaimAsString("nonce");
		if (!constantTimeEquals(expectedNonce, nonceClaim)) {
			throw new BusinessException(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
		}
		return jwt;
	}

	private boolean constantTimeEquals(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(expected.getBytes(), actual.getBytes());
	}
}
