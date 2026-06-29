package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminDashboardResponse;
import com.dongnemarket.admin.repository.AdminCommentRepository;
import com.dongnemarket.admin.repository.AdminMemberRepository;
import com.dongnemarket.admin.repository.AdminProductRepository;
import com.dongnemarket.admin.repository.AdminReportRepository;
import com.dongnemarket.report.entity.ReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    AdminMemberRepository adminMemberRepository;

    @Mock
    AdminProductRepository adminProductRepository;

    @Mock
    AdminReportRepository adminReportRepository;

    @Mock
    AdminCommentRepository adminCommentRepository;

    @InjectMocks
    AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("대시보드를 조회하면 회원·상품·신고·댓글 집계를 반환한다")
    void getDashboard_success() {
        given(adminMemberRepository.count()).willReturn(10L);
        given(adminProductRepository.count()).willReturn(5L);
        given(adminReportRepository.count()).willReturn(3L);
        given(adminReportRepository.countByStatus(ReportStatus.RECEIVED)).willReturn(2L);
        given(adminCommentRepository.count()).willReturn(7L);

        AdminDashboardResponse response = adminDashboardService.getDashboard();

        assertThat(response.getTotalMembers()).isEqualTo(10L);
        assertThat(response.getTotalProducts()).isEqualTo(5L);
        assertThat(response.getTotalReports()).isEqualTo(3L);
        assertThat(response.getPendingReports()).isEqualTo(2L);
        assertThat(response.getTotalComments()).isEqualTo(7L);
    }
}
