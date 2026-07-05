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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.mock.web.MockMultipartFile;

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
    @DisplayName("증빙 이미지를 첨부해 상품을 신고하면 응답에 이미지 URL이 포함된다")
    void reportProduct_withEvidenceImage_returnsUrlInResponse() throws Exception {
        String token = loginAs("reporter@test.com");

        mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FAKE_ITEM\",\"evidenceImageUrl\":\"https://example.com/evidence.jpg\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.evidenceImageUrl").value("https://example.com/evidence.jpg"));
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

    // ========== 신고 취소 ==========

    private Long createReportAndGetId(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FAKE_ITEM\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("reportId").asLong();
    }

    @Test
    @DisplayName("RECEIVED 상태인 내 신고를 취소하면 200을 반환하고 목록에서 사라진다")
    void cancelReport_success() throws Exception {
        String token = loginAs("reporter@test.com");
        Long reportId = createReportAndGetId(token);

        mockMvc.perform(delete("/api/members/me/reports/{reportId}", reportId)
                        .header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/members/me/reports")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("타인의 신고를 취소하려 하면 403과 REPORT_OWNER_ONLY를 반환한다")
    void cancelReport_notOwner_returns403() throws Exception {
        String reporterToken = loginAs("reporter@test.com");
        Long reportId = createReportAndGetId(reporterToken);

        String sellerToken = loginAs("seller@test.com");
        mockMvc.perform(delete("/api/members/me/reports/{reportId}", reportId)
                        .header("Authorization", sellerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("REPORT_OWNER_ONLY"));
    }

    @Test
    @DisplayName("존재하지 않는 신고를 취소하려 하면 404와 REPORT_NOT_FOUND를 반환한다")
    void cancelReport_notFound_returns404() throws Exception {
        String token = loginAs("reporter@test.com");

        mockMvc.perform(delete("/api/members/me/reports/{reportId}", 999_999L)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REPORT_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이 신고 취소를 요청하면 401과 UNAUTHORIZED를 반환한다")
    void cancelReport_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/members/me/reports/{reportId}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ========== 증빙 이미지 업로드 ==========

    @Test
    @DisplayName("이미지를 업로드하면 201과 접근 URL을 반환하고, 그 URL로 파일을 다시 조회할 수 있다")
    void uploadEvidenceImage_success_thenServesFile() throws Exception {
        String token = loginAs("reporter@test.com");
        MockMultipartFile file = new MockMultipartFile(
                "file", "evidence.png", "image/png", "fake-image-bytes".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/reports/evidence-image")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn();

        String url = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("evidenceImageUrl").asText();
        assertThatUrlLooksValid(url);

        mockMvc.perform(get(url).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("업로드한 이미지 URL을 신고에 첨부하면 내 신고 내역 조회 시 그대로 반환된다")
    void uploadEvidenceImage_thenAttachToReport() throws Exception {
        String token = loginAs("reporter@test.com");
        MockMultipartFile file = new MockMultipartFile(
                "file", "evidence.jpg", "image/jpeg", "fake-jpg-bytes".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/reports/evidence-image")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn();
        String url = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("evidenceImageUrl").asText();

        mockMvc.perform(post("/api/products/{productId}/reports", productId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"FAKE_ITEM\",\"evidenceImageUrl\":\"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.evidenceImageUrl").value(url));
    }

    @Test
    @DisplayName("이미지가 아닌 파일을 업로드하면 400과 INVALID_EVIDENCE_IMAGE를 반환한다")
    void uploadEvidenceImage_nonImage_returns400() throws Exception {
        String token = loginAs("reporter@test.com");
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "not an image".getBytes());

        mockMvc.perform(multipart("/api/reports/evidence-image")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_EVIDENCE_IMAGE"));
    }

    @Test
    @DisplayName("토큰 없이 이미지를 업로드하면 401과 UNAUTHORIZED를 반환한다")
    void uploadEvidenceImage_withoutToken_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evidence.png", "image/png", "fake-image-bytes".getBytes());

        mockMvc.perform(multipart("/api/reports/evidence-image").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("존재하지 않는 증빙 이미지를 조회하면 404와 EVIDENCE_IMAGE_NOT_FOUND를 반환한다")
    void getEvidenceImage_notFound_returns404() throws Exception {
        String token = loginAs("reporter@test.com");

        mockMvc.perform(get("/api/reports/evidence-image/{filename}", "does-not-exist.png")
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVIDENCE_IMAGE_NOT_FOUND"));
    }

    private void assertThatUrlLooksValid(String url) {
        org.assertj.core.api.Assertions.assertThat(url).startsWith("/api/reports/evidence-image/");
    }
}
