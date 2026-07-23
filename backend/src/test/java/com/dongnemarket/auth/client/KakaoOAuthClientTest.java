package com.dongnemarket.auth.client;

import com.dongnemarket.auth.config.OAuthWebClientConfig;
import com.dongnemarket.auth.entity.OAuthProvider;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link KakaoOAuthClient}를 실제 HTTP 서버(MockWebServer)로 검증한다. 원격 카카오에 의존하지 않고,
 * {@link OAuthWebClientConfig}가 실제로 적용하는 timeout/응답 크기 제한까지 함께 검증한다.
 */
class KakaoOAuthClientTest {

	private MockWebServer server;

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	private KakaoOAuthClient client(long responseTimeoutMillis, int maxInMemorySizeBytes) {
		WebClient webClient = new OAuthWebClientConfig()
				.oauthWebClient(WebClient.builder(), 2000, responseTimeoutMillis, maxInMemorySizeBytes);
		return new KakaoOAuthClient(
				webClient, "test-client-id", "test-client-secret",
				server.url("/oauth/token").toString(),
				server.url("/v2/user/me").toString(),
				responseTimeoutMillis);
	}

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
	}

	@Test
	@DisplayName("성공: 토큰 교환 + 사용자정보 조회 후 검증된 이메일로 신원을 반환한다")
	void resolveIdentity_success() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("""
						{"access_token":"kakao-access-token","token_type":"bearer","expires_in":21599}
						"""));
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("""
						{"id":123456789,"kakao_account":{"email":"user@kakao.com","is_email_valid":true,"is_email_verified":true}}
						"""));

		OAuthUserIdentity identity = client(5000, 1_000_000)
				.resolveIdentity("auth-code", "code-verifier", "https://app.example.com/callback", null);

		assertThat(identity.provider()).isEqualTo(OAuthProvider.KAKAO);
		assertThat(identity.providerUserId()).isEqualTo("123456789");
		assertThat(identity.email()).isEqualTo("user@kakao.com");
	}

	@Test
	@DisplayName("이메일이 미검증 상태면 OAUTH_EMAIL_NOT_VERIFIED를 던진다")
	void resolveIdentity_emailNotVerified_throws() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"access_token\":\"token\"}"));
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("""
						{"id":1,"kakao_account":{"email":"user@kakao.com","is_email_valid":true,"is_email_verified":false}}
						"""));

		assertThatThrownBy(() -> client(5000, 1_000_000)
				.resolveIdentity("code", "verifier", "https://app.example.com/callback", null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_EMAIL_NOT_VERIFIED);
	}

	@Test
	@DisplayName("이메일 자체가 없으면 OAUTH_EMAIL_NOT_PROVIDED를 던진다")
	void resolveIdentity_emailMissing_throws() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"access_token\":\"token\"}"));
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"id\":1,\"kakao_account\":{}}"));

		assertThatThrownBy(() -> client(5000, 1_000_000)
				.resolveIdentity("code", "verifier", "https://app.example.com/callback", null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_EMAIL_NOT_PROVIDED);
	}

	@Test
	@DisplayName("토큰 교환에서 4xx가 오면 잘못되거나 만료된 인가 코드로 매핑한다")
	void resolveIdentity_tokenExchange4xx_mapsToAuthorizationFailed() {
		server.enqueue(new MockResponse().setResponseCode(400)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"error\":\"invalid_grant\",\"error_description\":\"authorization code not found\"}"));

		assertThatThrownBy(() -> client(5000, 1_000_000)
				.resolveIdentity("bad-code", "verifier", "https://app.example.com/callback", null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
	}

	@Test
	@DisplayName("토큰 교환에서 5xx가 오면 제공자 장애로 매핑한다")
	void resolveIdentity_tokenExchange5xx_mapsToProviderError() {
		server.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));

		assertThatThrownBy(() -> client(5000, 1_000_000)
				.resolveIdentity("code", "verifier", "https://app.example.com/callback", null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_PROVIDER_ERROR);
	}

	@Test
	@DisplayName("응답이 설정된 타임아웃보다 늦으면 제공자 장애로 매핑한다")
	void resolveIdentity_timeout_mapsToProviderError() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"access_token\":\"token\"}")
				.setBodyDelay(3, TimeUnit.SECONDS));

		assertThatThrownBy(() -> client(500, 1_000_000)
				.resolveIdentity("code", "verifier", "https://app.example.com/callback", null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_PROVIDER_ERROR);
	}

	@Test
	@DisplayName("연결 자체가 끊기면(네트워크 오류) 제공자 장애로 매핑한다")
	void resolveIdentity_connectionReset_mapsToProviderError() {
		server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

		assertThatThrownBy(() -> client(2000, 1_000_000)
				.resolveIdentity("code", "verifier", "https://app.example.com/callback", null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_PROVIDER_ERROR);
	}

	@Test
	@DisplayName("응답 본문이 깨진 JSON이면 제공자 장애로 매핑한다")
	void resolveIdentity_malformedJson_mapsToProviderError() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("{not-valid-json"));

		assertThatThrownBy(() -> client(5000, 1_000_000)
				.resolveIdentity("code", "verifier", "https://app.example.com/callback", null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_PROVIDER_ERROR);
	}

	@Test
	@DisplayName("응답이 설정된 최대 크기를 넘으면 제공자 장애로 매핑한다")
	void resolveIdentity_oversizedResponse_mapsToProviderError() {
		String hugeBody = "{\"access_token\":\"" + "a".repeat(5000) + "\"}";
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(hugeBody));

		assertThatThrownBy(() -> client(5000, 256)
				.resolveIdentity("code", "verifier", "https://app.example.com/callback", null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_PROVIDER_ERROR);
	}
}
