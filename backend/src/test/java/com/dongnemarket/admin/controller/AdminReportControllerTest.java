package com.dongnemarket.admin.controller;

import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import com.dongnemarket.report.repository.ReportRepository;
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
 * 관리자 신고 조회 API(/api/admin/reports) 통합 테스트 (H2, Docker 불필요).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminReportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ReportRepository reportRepository;

    @AfterEach
    void cleanUp() {
        reportRepository.deleteAll();
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
    @DisplayName("관리자가 신고 목록을 조회하면 200과 전체 신고를 반환한다")
    void getReports_asAdmin_success() throws Exception {
        reportRepository.save(Report.ofProduct(1L, 10L, ReportReason.FAKE_ITEM, "가짜 상품"));
        reportRepository.save(Report.ofMember(1L, 20L, ReportReason.FRAUD_SUSPECTED, "사기 의심"));

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("관리자가 특정 신고를 조회하면 200과 해당 신고 정보를 반환한다")
    void getReport_asAdmin_success() throws Exception {
        Report report = reportRepository.save(Report.ofProduct(1L, 10L, ReportReason.PROHIBITED_ITEM, "금지 품목"));

        mockMvc.perform(get("/api/admin/reports/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(report.getId()))
                .andExpect(jsonPath("$.data.reason").value("PROHIBITED_ITEM"))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("관리자가 존재하지 않는 신고를 조회하면 404와 REPORT_NOT_FOUND를 반환한다")
    void getReport_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/reports/{reportId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REPORT_NOT_FOUND"));
    }

    @Test
    @DisplayName("일반 사용자가 신고 목록을 조회하면 403과 FORBIDDEN을 반환한다")
    void getReports_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("토큰 없이 신고 목록을 조회하면 401과 UNAUTHORIZED를 반환한다")
    void getReports_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
