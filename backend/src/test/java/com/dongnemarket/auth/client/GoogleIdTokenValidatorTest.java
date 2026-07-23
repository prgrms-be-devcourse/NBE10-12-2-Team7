package com.dongnemarket.auth.client;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link GoogleIdTokenValidator}를 실제 RSA 서명 + MockWebServer로 서빙하는 JWKS로 검증한다.
 * 원격 구글에 의존하지 않고, 이 테스트가 직접 발급한(private key 보유) 토큰만 신뢰해야 하므로
 * 서명 검증이 통과하는 유일한 경로가 이 테스트의 키페어를 통해서인지가 핵심이다.
 */
class GoogleIdTokenValidatorTest {

	private static final String ISSUER = "https://accounts.google.com";
	private static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";
	private static final String KEY_ID = "test-key-1";

	private MockWebServer server;
	private GoogleIdTokenValidator validator;
	private RSAPrivateKey privateKey;

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		privateKey = (RSAPrivateKey) keyPair.getPrivate();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

		RSAKey jwk = new RSAKey.Builder(publicKey).keyID(KEY_ID).build();
		String jwkSetJson = "{\"keys\":[" + jwk.toJSONString() + "]}";

		server = new MockWebServer();
		server.start();
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(jwkSetJson));

		String jwkSetUri = server.url("/oauth2/v3/certs").toString();
		validator = new GoogleIdTokenValidator(jwkSetUri, ISSUER, CLIENT_ID);
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	private String sign(JWTClaimsSet claims) throws Exception {
		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(KEY_ID).build(),
				claims);
		jwt.sign(new RSASSASigner(privateKey));
		return jwt.serialize();
	}

	private JWTClaimsSet.Builder validClaims(String nonce) {
		Instant now = Instant.now();
		return new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.audience(CLIENT_ID)
				.subject("google-user-123")
				.claim("email", "user@example.com")
				.claim("email_verified", true)
				.claim("nonce", nonce)
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plusSeconds(300)));
	}

	@Test
	@DisplayName("서명·iss·aud·exp·email_verified·nonce가 모두 유효하면 Jwt를 반환한다")
	void validate_allValid_returnsJwt() throws Exception {
		String nonce = UUID.randomUUID().toString();
		String token = sign(validClaims(nonce).build());

		Jwt jwt = validator.validate(token, nonce);

		assertThat(jwt.getSubject()).isEqualTo("google-user-123");
		assertThat(jwt.getClaimAsString("email")).isEqualTo("user@example.com");
	}

	@Test
	@DisplayName("nonce가 저장된 값과 다르면 거부한다")
	void validate_wrongNonce_throws() throws Exception {
		String token = sign(validClaims("expected-nonce").build());

		assertThatThrownBy(() -> validator.validate(token, "different-nonce"))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
	}

	@Test
	@DisplayName("만료된 토큰은 거부한다")
	void validate_expiredToken_throws() throws Exception {
		String nonce = "n1";
		Instant past = Instant.now().minusSeconds(600);
		String token = sign(validClaims(nonce)
				.issueTime(Date.from(past.minusSeconds(300)))
				.expirationTime(Date.from(past))
				.build());

		assertThatThrownBy(() -> validator.validate(token, nonce))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
	}

	@Test
	@DisplayName("issuer가 다르면 거부한다")
	void validate_wrongIssuer_throws() throws Exception {
		String nonce = "n2";
		JWTClaimsSet claims = validClaims(nonce).issuer("https://evil.example.com").build();
		String token = sign(claims);

		assertThatThrownBy(() -> validator.validate(token, nonce))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
	}

	@Test
	@DisplayName("audience(client id)가 다르면 거부한다")
	void validate_wrongAudience_throws() throws Exception {
		String nonce = "n3";
		JWTClaimsSet claims = validClaims(nonce).audience("someone-elses-client-id").build();
		String token = sign(claims);

		assertThatThrownBy(() -> validator.validate(token, nonce))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
	}

	@Test
	@DisplayName("email_verified가 false면 거부한다")
	void validate_emailNotVerified_throws() throws Exception {
		String nonce = "n4";
		JWTClaimsSet claims = validClaims(nonce).claim("email_verified", false).build();
		String token = sign(claims);

		assertThatThrownBy(() -> validator.validate(token, nonce))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
	}

	@Test
	@DisplayName("다른 키로 서명된(위조된) 토큰은 서명 검증에서 거부한다")
	void validate_wrongSigningKey_throws() throws Exception {
		String nonce = "n5";
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		RSAPrivateKey otherPrivateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();

		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(KEY_ID).build(),
				validClaims(nonce).build());
		jwt.sign(new RSASSASigner(otherPrivateKey));
		String token = jwt.serialize();

		assertThatThrownBy(() -> validator.validate(token, nonce))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_AUTHORIZATION_FAILED);
	}
}
