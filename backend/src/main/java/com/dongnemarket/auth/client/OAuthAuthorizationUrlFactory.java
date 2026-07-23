package com.dongnemarket.auth.client;

import com.dongnemarket.auth.entity.OAuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 카카오/구글 인가 화면 URL을 조립한다. 프론트가 쿼리 파라미터를 직접 구성하지 않도록, 이 팩토리가
 * 완성된 URL을 만든다(scope·response_type·PKCE 파라미터 이름 등을 프론트가 알 필요가 없다).
 */
@Component
public class OAuthAuthorizationUrlFactory {

	private final String kakaoAuthorizationUri;
	private final String kakaoClientId;
	private final String kakaoRedirectUri;
	private final String googleAuthorizationUri;
	private final String googleClientId;
	private final String googleRedirectUri;

	public OAuthAuthorizationUrlFactory(
			@Value("${oauth.kakao.authorization-uri}") String kakaoAuthorizationUri,
			@Value("${oauth.kakao.client-id}") String kakaoClientId,
			@Value("${oauth.kakao.redirect-uri}") String kakaoRedirectUri,
			@Value("${oauth.google.authorization-uri}") String googleAuthorizationUri,
			@Value("${oauth.google.client-id}") String googleClientId,
			@Value("${oauth.google.redirect-uri}") String googleRedirectUri) {
		this.kakaoAuthorizationUri = kakaoAuthorizationUri;
		this.kakaoClientId = kakaoClientId;
		this.kakaoRedirectUri = kakaoRedirectUri;
		this.googleAuthorizationUri = googleAuthorizationUri;
		this.googleClientId = googleClientId;
		this.googleRedirectUri = googleRedirectUri;
	}

	public String redirectUri(OAuthProvider provider) {
		return switch (provider) {
			case KAKAO -> kakaoRedirectUri;
			case GOOGLE -> googleRedirectUri;
		};
	}

	/** @param oidcNonce 구글만 사용. 카카오는 null이면 nonce 파라미터를 붙이지 않는다. */
	public String build(OAuthProvider provider, String state, String codeChallenge, String oidcNonce) {
		return switch (provider) {
			case KAKAO -> UriComponentsBuilder.fromUriString(kakaoAuthorizationUri)
					.queryParam("client_id", kakaoClientId)
					.queryParam("redirect_uri", kakaoRedirectUri)
					.queryParam("response_type", "code")
					.queryParam("scope", "account_email")
					.queryParam("state", state)
					.queryParam("code_challenge", codeChallenge)
					.queryParam("code_challenge_method", "S256")
					.build()
					.toUriString();
			case GOOGLE -> UriComponentsBuilder.fromUriString(googleAuthorizationUri)
					.queryParam("client_id", googleClientId)
					.queryParam("redirect_uri", googleRedirectUri)
					.queryParam("response_type", "code")
					.queryParam("scope", "openid email")
					.queryParam("state", state)
					.queryParam("nonce", oidcNonce)
					.queryParam("code_challenge", codeChallenge)
					.queryParam("code_challenge_method", "S256")
					.build()
					.toUriString();
		};
	}
}
