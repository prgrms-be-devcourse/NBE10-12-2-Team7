package com.dongnemarket.admin.controller;

import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 대시보드 API(/api/admin/dashboard) 통합 테스트 (H2, Docker 불필요).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        memberRepository.deleteAll();
    }

    private String adminToken() {
        Member admin = memberRepository.save(Member.createUser("admin@example.com", "encodedPassword", "관리자"));
        return jwtTokenProvider.createAccessToken(admin.getId(), "ROLE_ADMIN");
    }

    private String userToken() {
        Member user = memberRepository.save(Member.createUser("user@example.com", "encodedPassword", "일반회원"));
        return jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
    }

    @Test
    @DisplayName("관리자가 대시보드를 조회하면 200과 집계 필드를 반환한다")
    void getDashboard_asAdmin_success() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalMembers").exists())
                .andExpect(jsonPath("$.data.totalProducts").exists())
                .andExpect(jsonPath("$.data.totalReports").exists())
                .andExpect(jsonPath("$.data.pendingReports").exists())
                .andExpect(jsonPath("$.data.totalComments").exists());
    }

    @Test
    @DisplayName("일반 사용자가 대시보드를 조회하면 403과 FORBIDDEN을 반환한다")
    void getDashboard_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("토큰 없이 대시보드를 조회하면 401과 UNAUTHORIZED를 반환한다")
    void getDashboard_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
