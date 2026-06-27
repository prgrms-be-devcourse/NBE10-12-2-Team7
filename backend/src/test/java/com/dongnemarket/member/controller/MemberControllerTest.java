package com.dongnemarket.member.controller;

import com.dongnemarket.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 HTTP 요청으로 내 정보 조회/수정 성공/실패를 검증하는 통합 테스트 (H2, MySQL/Docker 불필요).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAll();
	}

	private String getAccessToken(String email, String password, String nickname) throws Exception {
		String signup = String.format(
				"{\"email\":\"%s\",\"password\":\"%s\",\"nickname\":\"%s\"}", email, password, nickname);
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signup));

		String login = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(login))
				.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	@Test
	@DisplayName("유효한 토큰으로 GET /api/members/me 요청하면 200과 내 정보를 반환한다")
	void getMyInfo_success() throws Exception {
		String token = getAccessToken("me@example.com", "password123", "meUser");

		mockMvc.perform(get("/api/members/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.email").value("me@example.com"))
				.andExpect(jsonPath("$.data.nickname").value("meUser"))
				.andExpect(jsonPath("$.data.memberId").exists())
				.andExpect(jsonPath("$.data.role").value("ROLE_USER"))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	@Test
	@DisplayName("토큰 없이 GET /api/members/me 요청하면 401을 반환한다")
	void getMyInfo_noToken_returns401() throws Exception {
		mockMvc.perform(get("/api/members/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("유효하지 않은 토큰으로 GET /api/members/me 요청하면 401을 반환한다")
	void getMyInfo_invalidToken_returns401() throws Exception {
		mockMvc.perform(get("/api/members/me")
						.header("Authorization", "Bearer invalid.jwt.token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	// ===== PATCH /api/members/me =====

	@Test
	@DisplayName("유효한 토큰으로 PATCH /api/members/me 요청하면 200과 변경된 닉네임을 반환한다")
	void updateMyInfo_success() throws Exception {
		String token = getAccessToken("patch@example.com", "password123", "oldNick");

		mockMvc.perform(patch("/api/members/me")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"newNick\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.nickname").value("newNick"))
				.andExpect(jsonPath("$.data.email").value("patch@example.com"));
	}

	@Test
	@DisplayName("토큰 없이 PATCH /api/members/me 요청하면 401을 반환한다")
	void updateMyInfo_noToken_returns401() throws Exception {
		mockMvc.perform(patch("/api/members/me")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"newNick\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("다른 회원이 사용 중인 닉네임으로 수정하면 409와 DUPLICATE_NICKNAME을 반환한다")
	void updateMyInfo_duplicateNickname_returns409() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"other@example.com\",\"password\":\"password123\",\"nickname\":\"takenNick\"}"));

		String token = getAccessToken("me2@example.com", "password123", "myNick");

		mockMvc.perform(patch("/api/members/me")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"takenNick\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("DUPLICATE_NICKNAME"));
	}

	@Test
	@DisplayName("닉네임이 1자이면 400과 INVALID_INPUT_VALUE를 반환한다")
	void updateMyInfo_blankNickname_returns400() throws Exception {
		String token = getAccessToken("valid@example.com", "password123", "validUser");

		mockMvc.perform(patch("/api/members/me")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"x\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	// ===== DELETE /api/members/me =====

	@Test
	@DisplayName("유효한 토큰으로 DELETE /api/members/me 요청하면 200을 반환한다")
	void deleteMyInfo_success() throws Exception {
		String token = getAccessToken("delete@example.com", "password123", "deleteUser");

		mockMvc.perform(delete("/api/members/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));
	}

	@Test
	@DisplayName("탈퇴 후 같은 계정으로 로그인하면 400과 DELETED_MEMBER를 반환한다")
	void deleteMyInfo_thenLoginFails() throws Exception {
		String token = getAccessToken("del2@example.com", "password123", "del2User");
		mockMvc.perform(delete("/api/members/me")
				.header("Authorization", "Bearer " + token));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"del2@example.com\",\"password\":\"password123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("DELETED_MEMBER"));
	}

	@Test
	@DisplayName("토큰 없이 DELETE /api/members/me 요청하면 401을 반환한다")
	void deleteMyInfo_noToken_returns401() throws Exception {
		mockMvc.perform(delete("/api/members/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("이미 탈퇴한 회원이 DELETE /api/members/me 재요청하면 400과 DELETED_MEMBER를 반환한다")
	void deleteMyInfo_alreadyDeleted_returns400() throws Exception {
		String token = getAccessToken("del3@example.com", "password123", "del3User");
		mockMvc.perform(delete("/api/members/me")
				.header("Authorization", "Bearer " + token));

		mockMvc.perform(delete("/api/members/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("DELETED_MEMBER"));
	}

	@Test
	@DisplayName("정지된 회원이 DELETE /api/members/me 요청하면 403과 SUSPENDED_MEMBER를 반환한다")
	void deleteMyInfo_suspendedMember_returns403() throws Exception {
		String token = getAccessToken("susp3@example.com", "password123", "susp3User");
		jdbcTemplate.update("UPDATE members SET status = 'SUSPENDED' WHERE email = ?", "susp3@example.com");

		mockMvc.perform(delete("/api/members/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("SUSPENDED_MEMBER"));
	}

	// ===== 상태 체크 — GET =====

	@Test
	@DisplayName("탈퇴한 회원 토큰으로 GET /api/members/me 요청하면 400과 DELETED_MEMBER를 반환한다")
	void getMyInfo_deletedMember_returns400() throws Exception {
		String token = getAccessToken("del4@example.com", "password123", "del4User");
		jdbcTemplate.update("UPDATE members SET status = 'DELETED' WHERE email = ?", "del4@example.com");

		mockMvc.perform(get("/api/members/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("DELETED_MEMBER"));
	}

	@Test
	@DisplayName("정지된 회원 토큰으로 GET /api/members/me 요청하면 403과 SUSPENDED_MEMBER를 반환한다")
	void getMyInfo_suspendedMember_returns403() throws Exception {
		String token = getAccessToken("susp1@example.com", "password123", "susp1User");
		jdbcTemplate.update("UPDATE members SET status = 'SUSPENDED' WHERE email = ?", "susp1@example.com");

		mockMvc.perform(get("/api/members/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("SUSPENDED_MEMBER"));
	}

	// ===== 상태 체크 — PATCH =====

	@Test
	@DisplayName("탈퇴한 회원 토큰으로 PATCH /api/members/me 요청하면 400과 DELETED_MEMBER를 반환한다")
	void updateMyInfo_deletedMember_returns400() throws Exception {
		String token = getAccessToken("del5@example.com", "password123", "del5User");
		jdbcTemplate.update("UPDATE members SET status = 'DELETED' WHERE email = ?", "del5@example.com");

		mockMvc.perform(patch("/api/members/me")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"newNick\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("DELETED_MEMBER"));
	}

	@Test
	@DisplayName("정지된 회원 토큰으로 PATCH /api/members/me 요청하면 403과 SUSPENDED_MEMBER를 반환한다")
	void updateMyInfo_suspendedMember_returns403() throws Exception {
		String token = getAccessToken("susp2@example.com", "password123", "susp2User");
		jdbcTemplate.update("UPDATE members SET status = 'SUSPENDED' WHERE email = ?", "susp2@example.com");

		mockMvc.perform(patch("/api/members/me")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"newNick\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("SUSPENDED_MEMBER"));
	}
}
