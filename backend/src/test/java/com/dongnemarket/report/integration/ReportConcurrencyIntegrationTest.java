package com.dongnemarket.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.repository.ReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dongnemarket.support.BaseIntegrationTest;

/**
 * [통합] 같은 상품에 대한 동시 중복 신고 요청이 DB 유니크 제약으로 최종 차단되는지 검증한다.
 * 애플리케이션 레벨 existsBy... 체크만으로는 레이스 컨디션(여러 요청이 동시에 "아직 없음"을 확인하고
 * 동시에 저장 시도)을 막을 수 없어, uk_reports_reporter_target_product 제약이 최후 방어선이 된다.
 */
@DisplayName("[통합] 신고 동시성 - DB 레벨 중복 방지")
@Sql("/sql/report-scenario.sql")
class ReportConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProductRepository productRepository;
    @Autowired ReportRepository reportRepository;

    private String loginAs(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"pw\"}", email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long productId() {
        return productRepository.findAll().get(0).getId();
    }

    @Test
    @DisplayName("같은 상품을 동시에 여러 번 신고 요청해도 실제 저장된 신고는 1건뿐이다")
    void concurrentDuplicateReports_onlyOneSucceeds() throws Exception {
        String token = loginAs("reporter@test.com");
        Long targetProductId = productId();
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/products/{productId}/reports", targetProductId)
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"reason\":\"FAKE_ITEM\"}"))
                            .andReturn();
                    int status = result.getResponse().getStatus();
                    if (status == HttpStatus.CREATED.value()) {
                        successCount.incrementAndGet();
                    } else if (status == HttpStatus.CONFLICT.value()) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);

        List<Report> savedReports = reportRepository.findAll();
        assertThat(savedReports).hasSize(1);
    }
}
