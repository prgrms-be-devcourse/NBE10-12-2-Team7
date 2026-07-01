package com.dongnemarket.report.integration;

import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [통합] 신고 API — 유즈케이스 + 스펙 문서 역할을 겸하는 핵심 케이스만 작성.
 * 진짜 MySQL(Testcontainers) 위에서 JWT 인증까지 포함한 전 계층을 검증한다.
 */
@DisplayName("[통합] 신고 API")
@Sql("/sql/report-scenario.sql")
class ReportControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired ProductRepository productRepository;

    // ========== 헬퍼 ==========

    private String loginAs(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"pw\"}", email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long productId() {
        return productRepository.findAll().get(0).getId();
    }

    private Long targetMemberId() {
        return memberRepository.findByEmail("target@test.com").orElseThrow().getId();
    }

    // ========== 상품 신고 ==========

    @Test
    @DisplayName("다른 사람의 상품을 신고하면 201과 RECEIVED 상태를 반환한다")
    void reportProduct_success() throws Exception {
        String token = loginAs("reporter@test.com");

        mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FAKE_ITEM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportType").value("PRODUCT"))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("본인이 등록한 상품을 신고하면 400과 CANNOT_REPORT_OWN_PRODUCT를 반환한다")
    void reportProduct_ownProduct_returns400() throws Exception {
        String token = loginAs("seller@test.com");

        mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FAKE_ITEM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CANNOT_REPORT_OWN_PRODUCT"));
    }

    @Test
    @DisplayName("같은 상품을 두 번 신고하면 409와 DUPLICATE_REPORT를 반환한다")
    void reportProduct_duplicate_returns409() throws Exception {
        String token = loginAs("reporter@test.com");
        String body = "{\"reason\":\"FAKE_ITEM\"}";

        mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_REPORT"));
    }

    @Test
    @DisplayName("토큰 없이 상품 신고를 요청하면 401과 UNAUTHORIZED를 반환한다")
    void reportProduct_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FAKE_ITEM\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ========== 회원 신고 ==========

    @Test
    @DisplayName("다른 회원을 신고하면 201과 RECEIVED 상태를 반환한다")
    void reportMember_success() throws Exception {
        String token = loginAs("reporter@test.com");

        mockMvc.perform(post("/api/members/{memberId}/reports", targetMemberId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FRAUD_SUSPECTED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportType").value("MEMBER"))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("본인 계정을 신고하면 400과 CANNOT_REPORT_SELF를 반환한다")
    void reportMember_self_returns400() throws Exception {
        String token = loginAs("reporter@test.com");
        Long reporterId = memberRepository.findByEmail("reporter@test.com").orElseThrow().getId();

        mockMvc.perform(post("/api/members/{memberId}/reports", reporterId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FRAUD_SUSPECTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CANNOT_REPORT_SELF"));
    }

    // ========== 내 신고 내역 조회 ==========

    @Test
    @DisplayName("내 신고 내역을 조회하면 200과 신고 목록을 반환한다")
    void getMyReports_success() throws Exception {
        String token = loginAs("reporter@test.com");
        String body = "{\"reason\":\"FAKE_ITEM\"}";

        mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/members/me/reports")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reportType").value("PRODUCT"));
    }

    @Test
    @DisplayName("토큰 없이 내 신고 내역을 조회하면 401과 UNAUTHORIZED를 반환한다")
    void getMyReports_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/members/me/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
