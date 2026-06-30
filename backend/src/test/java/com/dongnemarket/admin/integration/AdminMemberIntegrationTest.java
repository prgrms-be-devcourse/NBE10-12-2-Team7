package com.dongnemarket.admin.integration;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [통합] 관리자 회원 조회 — 진짜 MySQL(Testcontainers).
 * 시드전략 1(Initializer 가 만든 관리자) + 시드전략 2(@Sql 시나리오)를 한 테스트에서 확인한다.
 */
@DisplayName("[통합] 관리자 회원 조회 — 진짜 MySQL")
class AdminMemberIntegrationTest extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    /** 시드전략 1: AdminAccountInitializer 가 부팅 시 만든 관리자로 로그인 */
    private String loginAsSeededAdmin() throws Exception {
        String body = "{\"email\":\"admin@dongnemarket.com\",\"password\":\"admin1234!\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("시드 관리자 토큰으로 회원 목록을 조회한다")
    @Sql("/sql/products-scenario.sql") // 시드전략 2: 이 테스트 전용 데이터(판매자 회원 1명 추가)
    void listMembers() throws Exception {
        String token = loginAsSeededAdmin();

        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
