package com.dongnemarket.auth.client;

import com.dongnemarket.auth.entity.OAuthProvider;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 구글 REST API 직접 연동. 인가 코드를 토큰으로 교환하면 함께 오는 ID Token을
 * {@link GoogleIdTokenValidator}로 로컬 검증해 신원을 얻는다(별도 사용자정보 API 호출 없음).
 */
@Component
public class GoogleOAuthClient implements OAuthClient {

	private final WebClient webClient;
	private final GoogleIdTokenValidator idTokenValidator;
	private final String clientId;
	private final String clientSecret;
	private final String tokenUri;
	private final Duration responseTimeout;

	public GoogleOAuthClient(
			WebClient oauthWebClient,
			GoogleIdTokenValidator idTokenValidator,
			@Value("${oauth.google.client-id}") String clientId,
			@Value("${oauth.google.client-secret}") String clientSecret,
			@Value("${oauth.google.token-uri}") String tokenUri,
			@Value("${oauth.http.response-timeout-millis}") long responseTimeoutMillis) {
		this.webClient = oauthWebClient;
		this.idTokenValidator = idTokenValidator;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.tokenUri = tokenUri;
		this.responseTimeout = Duration.ofMillis(responseTimeoutMillis);
	}

	@Override
	public OAuthProvider provider() {
		return OAuthProvider.GOOGLE;
	}

	@Override
	public OAuthUserIdentity resolveIdentity(String code, String codeVerifier, String redirectUri, String oidcNonce) {
		String idToken = exchangeToken(code, codeVerifier, redirectUri);
		Jwt jwt = idTokenValidator.validate(idToken, oidcNonce);

		String email = jwt.getClaimAsString("email");
		if (email == null || email.isBlank()) {
			throw new BusinessException(ErrorCode.OAUTH_EMAIL_NOT_PROVIDED);
		}
		return new OAuthUserIdentity(OAuthProvider.GOOGLE, jwt.getSubject(), email);
	}

	private String exchangeToken(String code, String codeVerifier, String redirectUri) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("redirect_uri", redirectUri);
		form.add("code", code);
		form.add("code_verifier", codeVerifier);

		GoogleTokenResponse response = OAuthClientErrorMapper.call(() -> webClient.post()
				.uri(tokenUri)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(form))
				.retrieve()
				.bodyToMono(GoogleTokenResponse.class)
				.timeout(responseTimeout)
				.block());

		if (response == null || response.idToken() == null || response.idToken().isBlank()) {
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}
		return response.idToken();
	}

	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GoogleTokenResponse(String idToken) {
	}
}
