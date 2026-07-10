package com.dongnemarket.admin.controller;

import com.dongnemarket.admin.dto.OrphanDeleteRequest;
import com.dongnemarket.admin.dto.OrphanDeleteRequest.OrphanTarget;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.global.storage.FileStorageService;
import com.dongnemarket.global.storage.StoredObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [통합/E2E] 관리자 저장소 고아파일 API. 보안(ADMIN)·응답 봉투를 엔드포인트로 검증한다.
 * FileStorageService만 목으로 대체(디스크/S3 무관하게 결정적으로), 참조 레포는 실제 H2(빈 상태)를 탄다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminStorageControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtTokenProvider jwtTokenProvider;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    FileStorageService fileStorageService;

    private String adminToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(1L, "ROLE_ADMIN");
    }

    private String userToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(2L, "ROLE_USER");
    }

    private StoredObject oldOrphan(String name) {
        return new StoredObject(name, 123L, Instant.now().minus(Duration.ofDays(2)));
    }

    @Test
    @DisplayName("관리자가 고아 목록을 조회하면 200 + 봉투(data.orphans/totalCount)")
    void getOrphans_asAdmin_returns200() throws Exception {
        given(fileStorageService.list("product-images")).willReturn(List.of(oldOrphan("orphan.png")));
        given(fileStorageService.list("report-evidence")).willReturn(List.of());

        mockMvc.perform(get("/api/admin/storage/orphans").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.orphans[0].filename").value("orphan.png"))
                .andExpect(jsonPath("$.data.orphans[0].directory").value("product-images"));
    }

    @Test
    @DisplayName("토큰 없이 조회하면 401")
    void getOrphans_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/storage/orphans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("일반 사용자(ROLE_USER)가 조회하면 403")
    void getOrphans_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/storage/orphans").header("Authorization", userToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자가 고아를 삭제하면 200 + deleted 집계")
    void deleteOrphans_asAdmin_returns200() throws Exception {
        given(fileStorageService.list("product-images")).willReturn(List.of(oldOrphan("orphan.png")));
        given(fileStorageService.list("report-evidence")).willReturn(List.of());
        given(fileStorageService.delete("orphan.png", "product-images")).willReturn(true);

        String body = objectMapper.writeValueAsString(
                new OrphanDeleteRequest(List.of(new OrphanTarget("product-images", "orphan.png"))));

        mockMvc.perform(delete("/api/admin/storage/orphans")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(1))
                .andExpect(jsonPath("$.data.deleted").value(1))
                .andExpect(jsonPath("$.data.skipped").value(0));
    }
}
