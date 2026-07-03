package com.dongnemarket.auth.controller;

import com.dongnemarket.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
	@DisplayName("올바른 이메일·비밀번호로 로그인하면 200과 accessToken·refreshToken을 반환한다")
	void login_success() throws Exception {
		String signup = "{ \"email\": \"login@example.com\", \"password\": \"password123\", \"nickname\": \"loginUser\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"login@example.com\", \"password\": \"password123\" }";
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.accessToken").exists())
				.andExpect(jsonPath("$.data.refreshToken").exists());
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
	@DisplayName("유효한 Refresh Token으로 재발급하면 인증 헤더 없이도 200과 새 accessToken을 반환한다")
	void reissue_success() throws Exception {
		String signup = "{ \"email\": \"reissue@example.com\", \"password\": \"password123\", \"nickname\": \"reissueUser\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"reissue@example.com\", \"password\": \"password123\" }";
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andReturn();
		String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data").path("refreshToken").asText();

		String reissueBody = String.format("{ \"refreshToken\": \"%s\" }", refreshToken);
		mockMvc.perform(post("/api/auth/reissue").contentType(MediaType.APPLICATION_JSON).content(reissueBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists())
				.andExpect(jsonPath("$.data.refreshToken").value(refreshToken));
	}

	@Test
	@DisplayName("Access Token으로 재발급을 시도하면 401과 INVALID_REFRESH_TOKEN을 반환한다")
	void reissue_withAccessToken_returnsInvalidRefreshToken() throws Exception {
		String signup = "{ \"email\": \"reissue-access@example.com\", \"password\": \"password123\", \"nickname\": \"reissueAccess\" }";
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
				.andExpect(status().isCreated());

		String login = "{ \"email\": \"reissue-access@example.com\", \"password\": \"password123\" }";
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
				.andReturn();
		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();

		String reissueBody = String.format("{ \"refreshToken\": \"%s\" }", accessToken);
		mockMvc.perform(post("/api/auth/reissue").contentType(MediaType.APPLICATION_JSON).content(reissueBody))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	@DisplayName("Refresh Token이 빈 값이면 400과 INVALID_INPUT_VALUE를 반환한다")
	void reissue_blankRefreshToken() throws Exception {
		String body = "{ \"refreshToken\": \"\" }";

		mockMvc.perform(post("/api/auth/reissue").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("형식이 깨진(malformed) Refresh Token으로 재발급하면 401과 INVALID_REFRESH_TOKEN을 반환한다")
	void reissue_malformedToken() throws Exception {
		String body = "{ \"refreshToken\": \"not.a.valid.token\" }";

		mockMvc.perform(post("/api/auth/reissue").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
	}
}
