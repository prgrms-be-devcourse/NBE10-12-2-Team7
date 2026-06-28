package com.dongnemarket.admin.controller;

import com.dongnemarket.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 회원 조회 API(/api/admin/members) 통합 테스트 (H2, MySQL/Docker 불필요).
 * 관리자 토큰은 시드 없이 signup → jdbc로 role 승격 → login 으로 발급한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMemberControllerTest {

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

    // ===== 토큰 발급 헬퍼 =====

    private void signup(String email, String password, String nickname) throws Exception {
        String body = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"nickname\":\"%s\"}", email, password, nickname);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String login(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    /** 일반 사용자(ROLE_USER) 토큰 */
    private String getUserToken(String email, String password, String nickname) throws Exception {
        signup(email, password, nickname);
        return login(email, password);
    }

    /** 관리자(ROLE_ADMIN) 토큰: 가입 후 role 승격 → 그 다음 로그인해야 토큰에 ROLE_ADMIN 이 담긴다 */
    private String getAdminToken(String email, String password, String nickname) throws Exception {
        signup(email, password, nickname);
        jdbcTemplate.update("UPDATE members SET role = 'ROLE_ADMIN' WHERE email = ?", email);
        return login(email, password);
    }

    private Long memberIdOf(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM members WHERE email = ?", Long.class, email);
    }

    // ===== GET /api/admin/members =====

    @Test
    @DisplayName("관리자가 회원 목록을 조회하면 200과 전체 회원을 반환한다")
    void getMembers_asAdmin_success() throws Exception {
        String adminToken = getAdminToken("admin@example.com", "password123", "adminUser");
        signup("user1@example.com", "password123", "user1");   // 일반 회원 1명 추가 → 총 2명

        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("일반 사용자가 회원 목록을 조회하면 403과 FORBIDDEN을 반환한다")
    void getMembers_asUser_returns403() throws Exception {
        String userToken = getUserToken("user@example.com", "password123", "normalUser");

        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("토큰 없이 회원 목록을 조회하면 401과 UNAUTHORIZED를 반환한다")
    void getMembers_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ===== GET /api/admin/members/{memberId} =====

    @Test
    @DisplayName("관리자가 특정 회원을 조회하면 200과 해당 회원 정보를 반환한다")
    void getMember_asAdmin_success() throws Exception {
        String adminToken = getAdminToken("admin2@example.com", "password123", "admin2");
        signup("target@example.com", "password123", "targetUser");
        Long targetId = memberIdOf("target@example.com");

        mockMvc.perform(get("/api/admin/members/{memberId}", targetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.memberId").exists())
                .andExpect(jsonPath("$.data.email").value("target@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("targetUser"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("관리자가 존재하지 않는 회원을 조회하면 404와 MEMBER_NOT_FOUND를 반환한다")
    void getMember_notFound_returns404() throws Exception {
        String adminToken = getAdminToken("admin3@example.com", "password123", "admin3");

        mockMvc.perform(get("/api/admin/members/{memberId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MEMBER_NOT_FOUND"));
    }
}