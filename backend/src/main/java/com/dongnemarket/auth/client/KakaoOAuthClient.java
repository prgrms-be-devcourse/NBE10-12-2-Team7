package com.dongnemarket.auth.client;

import com.dongnemarket.auth.entity.OAuthProvider;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 카카오 REST API 직접 연동. 인가 코드를 액세스 토큰으로 교환한 뒤, 그 토큰으로 사용자정보를 조회한다
 * (카카오는 OIDC ID Token을 쓰지 않는 REST API 흐름이라 이 프로젝트에서는 별도 검증 없이 access_token
 * 기반 사용자정보 API만 사용한다).
 */
@Component
public class KakaoOAuthClient implements OAuthClient {

	private final WebClient webClient;
	private final String clientId;
	private final String clientSecret;
	private final String tokenUri;
	private final String userInfoUri;
	private final Duration responseTimeout;

	public KakaoOAuthClient(
			WebClient oauthWebClient,
			@Value("${oauth.kakao.client-id}") String clientId,
			@Value("${oauth.kakao.client-secret}") String clientSecret,
			@Value("${oauth.kakao.token-uri}") String tokenUri,
			@Value("${oauth.kakao.user-info-uri}") String userInfoUri,
			@Value("${oauth.http.response-timeout-millis}") long responseTimeoutMillis) {
		this.webClient = oauthWebClient;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.tokenUri = tokenUri;
		this.userInfoUri = userInfoUri;
		this.responseTimeout = Duration.ofMillis(responseTimeoutMillis);
	}

	@Override
	public OAuthProvider provider() {
		return OAuthProvider.KAKAO;
	}

	@Override
	public OAuthUserIdentity resolveIdentity(String code, String codeVerifier, String redirectUri, String oidcNonce) {
		String accessToken = exchangeToken(code, codeVerifier, redirectUri);
		KakaoUserResponse user = fetchUser(accessToken);
		return toIdentity(user);
	}

	private String exchangeToken(String code, String codeVerifier, String redirectUri) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("redirect_uri", redirectUri);
		form.add("code", code);
		form.add("code_verifier", codeVerifier);
		if (clientSecret != null && !clientSecret.isBlank()) {
			form.add("client_secret", clientSecret);
		}

		KakaoTokenResponse response = OAuthClientErrorMapper.call(() -> webClient.post()
				.uri(tokenUri)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(form))
				.retrieve()
				.bodyToMono(KakaoTokenResponse.class)
				.timeout(responseTimeout)
				.block());

		if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}
		return response.accessToken();
	}

	private KakaoUserResponse fetchUser(String accessToken) {
		KakaoUserResponse response = OAuthClientErrorMapper.call(() -> webClient.get()
				.uri(userInfoUri)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
				.retrieve()
				.bodyToMono(KakaoUserResponse.class)
				.timeout(responseTimeout)
				.block());

		if (response == null) {
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}
		return response;
	}

	private OAuthUserIdentity toIdentity(KakaoUserResponse user) {
		if (user.id() == null) {
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}
		KakaoUserResponse.KakaoAccount account = user.kakaoAccount();
		if (account == null || account.email() == null || account.email().isBlank()) {
			throw new BusinessException(ErrorCode.OAUTH_EMAIL_NOT_PROVIDED);
		}
		boolean verified = Boolean.TRUE.equals(account.isEmailValid()) && Boolean.TRUE.equals(account.isEmailVerified());
		if (!verified) {
			throw new BusinessException(ErrorCode.OAUTH_EMAIL_NOT_VERIFIED);
		}
		return new OAuthUserIdentity(OAuthProvider.KAKAO, String.valueOf(user.id()), account.email());
	}

	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record KakaoTokenResponse(String accessToken) {
	}

	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record KakaoUserResponse(Long id, KakaoAccount kakaoAccount) {

		@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
		@JsonIgnoreProperties(ignoreUnknown = true)
		private record KakaoAccount(String email, Boolean isEmailValid, Boolean isEmailVerified) {
		}
	}
}
