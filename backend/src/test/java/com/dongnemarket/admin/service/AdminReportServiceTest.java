package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminReportResponse;
import com.dongnemarket.admin.repository.AdminReportRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock
    AdminReportRepository adminReportRepository;

    @InjectMocks
    AdminReportService adminReportService;

    @Test
    @DisplayName("신고 목록을 조회하면 전체 신고를 반환한다")
    void getReports_success() {
        Report r1 = Report.ofProduct(1L, 10L, ReportReason.FAKE_ITEM, "가짜 상품");
        Report r2 = Report.ofMember(2L, 20L, ReportReason.FRAUD_SUSPECTED, "사기 의심");
        given(adminReportRepository.findAll()).willReturn(List.of(r1, r2));

        List<AdminReportResponse> responses = adminReportService.getReports();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(AdminReportResponse::getReason)
                .containsExactly(ReportReason.FAKE_ITEM, ReportReason.FRAUD_SUSPECTED);
    }

    @Test
    @DisplayName("신고가 없으면 빈 목록을 반환한다")
    void getReports_empty_returnsEmptyList() {
        given(adminReportRepository.findAll()).willReturn(List.of());

        assertThat(adminReportService.getReports()).isEmpty();
    }

    @Test
    @DisplayName("존재하는 reportId로 상세 조회하면 해당 신고를 반환한다")
    void getReport_success() {
        Report report = Report.ofProduct(1L, 10L, ReportReason.PROHIBITED_ITEM, "금지 품목");
        given(adminReportRepository.findById(1L)).willReturn(Optional.of(report));

        AdminReportResponse response = adminReportService.getReport(1L);

        assertThat(response.getReason()).isEqualTo(ReportReason.PROHIBITED_ITEM);
        assertThat(response.getContent()).isEqualTo("금지 품목");
    }

    @Test
    @DisplayName("존재하지 않는 reportId로 상세 조회하면 REPORT_NOT_FOUND 예외가 발생한다")
    void getReport_notFound_throwsException() {
        given(adminReportRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminReportService.getReport(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_NOT_FOUND);
    }
}
