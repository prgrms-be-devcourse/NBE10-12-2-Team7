package com.dongnemarket.auth.controller;

import com.dongnemarket.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 HTTP 요청으로 회원가입 성공/실패를 검증하는 통합 테스트 (H2, MySQL/Docker 불필요).
 * Postman 시나리오(docs/postman/auth-signup.md)의 응답 예시는 이 테스트로 직접 확인한 값이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	ObjectMapper objectMapper;

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAll();
	}

	private String extractRefreshTokenCookie(MvcResult result) {
		Cookie cookie = result.getResponse().getCookie("refreshToken");
		return cookie != null ? cookie.getValue() : null;
	}

	@Test
	@DisplayName("회원가입 성공 시 201과 회원 정보를 반환한다")
	void signup_success() throws Exception {
		String body = "{ \"email\": \"test@example.com\", \"password\": \"password123\", \"nickname\": \"tester\" }";

		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(201))
				.andExpect(jsonPath("$.data.email").value("test@example.com"))
				.andExpect(jsonPath("$.data.nickname").value("tester"))
				.andExpect(jsonPath("$.data.memberId").exists());
	}

	@Test
	@DisplayName("이메일이 중복되면 409와 DUPLICATE_EMAIL을 반환한다")
	void signup_duplicateEmail() throws Exception {
		String first = "{ \"email\": \"dup@example.com\", \"password\": \"password123\", \"nickname\": \"first\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(first));

		String second = "{ \"email\": \"dup@example.com\", \"password\": \"password123\", \"nickname\": \"second\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(second))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"));
	}

	@Test
	@DisplayName("닉네임이 중복되면 409와 DUPLICATE_NICKNAME을 반환한다")
	void signup_duplicateNickname() throws Exception {
		String first = "{ \"email\": \"a@example.com\", \"password\": \"password123\", \"nickname\": \"dupNick\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(first));

		String second = "{ \"email\": \"b@example.com\", \"password\": \"password123\", \"nickname\": \"dupNick\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(second))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("DUPLICATE_NICKNAME"));
	}

	@Test
	@DisplayName("닉네임이 비어 있으면 400과 INVALID_INPUT_VALUE를 반환한다")
	void signup_blankNickname() throws Exception {
		String body = "{ \"email\": \"valid@example.com\", \"password\": \"password123\", \"nickname\": \"\" }";

		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	// ===== login =====

	@Test
	@DisplayName("올바른 이메일·비밀번호로 로그인하면 200과 accessToken을 반환하고, Refresh Token은 HttpOnly 쿠키로 내려간다")
	void login_success() throws Exception {
		String signup = "{ \"email\": \"login@example.com\", \"password\": \"password123\", \"nickname\": \"loginUser\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"login@example.com\", \"password\": \"password123\" }";
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.accessToken").exists())
				.andExpect(jsonPath("$.data.refreshToken").doesNotExist())
				.andExpect(cookie().exists("refreshToken"))
				.andExpect(cookie().httpOnly("refreshToken", true))
				.andExpect(cookie().path("refreshToken", "/"));
	}

	@Test
	@DisplayName("autoLogin=true로 로그인하면 Refresh Token 쿠키에 Max-Age(7일)가 설정된 영속 쿠키로 내려간다")
	void login_autoLoginTrue_setsPersistentCookieWithMaxAge() throws Exception {
		String signup = "{ \"email\": \"autologin-true@example.com\", \"password\": \"password123\", \"nickname\": \"autoLoginTrue\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"autologin-true@example.com\", \"password\": \"password123\", \"autoLogin\": true }";
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("refreshToken"))
				.andExpect(cookie().maxAge("refreshToken", 604800));
	}

	@Test
	@DisplayName("autoLogin=false(또는 미지정)로 로그인하면 Refresh Token 쿠키가 Max-Age 없는 세션 쿠키로 내려간다")
	void login_autoLoginFalse_setsSessionCookieWithoutMaxAge() throws Exception {
		String signup = "{ \"email\": \"autologin-false@example.com\", \"password\": \"password123\", \"nickname\": \"autoLoginFalse\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"autologin-false@example.com\", \"password\": \"password123\", \"autoLogin\": false }";
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("refreshToken"))
				.andExpect(cookie().maxAge("refreshToken", -1));
	}

	@Test
	@DisplayName("존재하지 않는 이메일로 로그인하면 404와 MEMBER_NOT_FOUND를 반환한다")
	void login_emailNotFound() throws Exception {
		String body = "{ \"email\": \"none@example.com\", \"password\": \"password123\" }";

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("MEMBER_NOT_FOUND"));
	}

	@Test
	@DisplayName("비밀번호가 틀리면 401과 INVALID_PASSWORD를 반환한다")
	void login_wrongPassword() throws Exception {
		String signup = "{ \"email\": \"pw@example.com\", \"password\": \"password123\", \"nickname\": \"pwUser\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"pw@example.com\", \"password\": \"wrongPassword\" }";
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_PASSWORD"));
	}

	@Test
	@DisplayName("이메일 형식이 올바르지 않으면 400과 INVALID_INPUT_VALUE를 반환한다")
	void login_invalidEmailFormat() throws Exception {
		String body = "{ \"email\": \"not-an-email\", \"password\": \"password123\" }";

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("비밀번호가 빈 값이면 400과 INVALID_INPUT_VALUE를 반환한다")
	void login_blankPassword() throws Exception {
		String body = "{ \"email\": \"test@example.com\", \"password\": \"\" }";

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	// ===== reissue =====

	@Test
	@DisplayName("유효한 Refresh Token 쿠키로 재발급하면 200과 새 accessToken을 반환한다")
	void reissue_success() throws Exception {
		String signup = "{ \"email\": \"reissue@example.com\", \"password\": \"password123\", \"nickname\": \"reissueUser\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"reissue@example.com\", \"password\": \"password123\" }";
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andReturn();
		String refreshToken = extractRefreshTokenCookie(loginResult);

		mockMvc.perform(post("/api/auth/reissue").cookie(new Cookie("refreshToken", refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists())
				.andExpect(jsonPath("$.data.refreshToken").doesNotExist());
	}

	@Test
	@DisplayName("Access Token을 쿠키에 담아 재발급을 시도하면 401과 INVALID_REFRESH_TOKEN을 반환한다")
	void reissue_withAccessToken_returnsInvalidRefreshToken() throws Exception {
		String signup = "{ \"email\": \"reissue-access@example.com\", \"password\": \"password123\", \"nickname\": \"reissueAccess\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"reissue-access@example.com\", \"password\": \"password123\" }";
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andReturn();
		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();

		mockMvc.perform(post("/api/auth/reissue").cookie(new Cookie("refreshToken", accessToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	@DisplayName("Refresh Token 쿠키가 없으면 401과 INVALID_REFRESH_TOKEN을 반환한다")
	void reissue_noCookie_returnsInvalidRefreshToken() throws Exception {
		mockMvc.perform(post("/api/auth/reissue"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	@DisplayName("형식이 깨진(malformed) Refresh Token 쿠키로 재발급하면 401과 INVALID_REFRESH_TOKEN을 반환한다")
	void reissue_malformedToken() throws Exception {
		mockMvc.perform(post("/api/auth/reissue").cookie(new Cookie("refreshToken", "not.a.valid.token")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
	}

	// ===== logout =====

	@Test
	@DisplayName("로그인한 사용자가 로그아웃하면 200을 반환하고 Refresh Token 쿠키를 만료시킨다")
	void logout_success() throws Exception {
		String signup = "{ \"email\": \"logout@example.com\", \"password\": \"password123\", \"nickname\": \"logoutUser\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"logout@example.com\", \"password\": \"password123\" }";
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andReturn();
		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();

		MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andReturn();

		Cookie clearedCookie = logoutResult.getResponse().getCookie("refreshToken");
		assertThat(clearedCookie).isNotNull();
		assertThat(clearedCookie.getMaxAge()).isEqualTo(0);
	}

	@Test
	@DisplayName("인증 헤더 없이 로그아웃을 시도하면 401을 반환한다")
	void logout_withoutToken_returns401() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("로그아웃을 여러 번 호출해도 항상 200을 반환한다(멱등)")
	void logout_calledTwice_bothReturn200() throws Exception {
		String signup = "{ \"email\": \"logout-twice@example.com\", \"password\": \"password123\", \"nickname\": \"logoutTwice\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"logout-twice@example.com\", \"password\": \"password123\" }";
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andReturn();
		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();

		mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("로그아웃 이후 기존 Refresh Token으로 재발급을 시도하면 401과 REFRESH_TOKEN_NOT_FOUND를 반환한다")
	void logout_thenReissueWithOldRefreshToken_returns401RefreshTokenNotFound() throws Exception {
		String signup = "{ \"email\": \"logout-reissue@example.com\", \"password\": \"password123\", \"nickname\": \"logoutReissue\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"logout-reissue@example.com\", \"password\": \"password123\" }";
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andReturn();
		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
		String refreshToken = extractRefreshTokenCookie(loginResult);

		mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/reissue").cookie(new Cookie("refreshToken", refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("REFRESH_TOKEN_NOT_FOUND"));
	}
}
