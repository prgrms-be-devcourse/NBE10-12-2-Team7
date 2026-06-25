package com.dongnemarket.auth.controller;

import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
}
