package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminDashboardResponse;
import com.dongnemarket.admin.repository.AdminCommentRepository;
import com.dongnemarket.admin.repository.AdminMemberRepository;
import com.dongnemarket.admin.repository.AdminProductRepository;
import com.dongnemarket.admin.repository.AdminReportRepository;
import com.dongnemarket.report.entity.ReportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminMemberRepository adminMemberRepository;
    private final AdminProductRepository adminProductRepository;
    private final AdminReportRepository adminReportRepository;
    private final AdminCommentRepository adminCommentRepository;

    public AdminDashboardService(AdminMemberRepository adminMemberRepository,
                                 AdminProductRepository adminProductRepository,
                                 AdminReportRepository adminReportRepository,
                                 AdminCommentRepository adminCommentRepository) {
        this.adminMemberRepository = adminMemberRepository;
        this.adminProductRepository = adminProductRepository;
        this.adminReportRepository = adminReportRepository;
        this.adminCommentRepository = adminCommentRepository;
    }

    /** 관리 대시보드 집계 (전체 회원·상품·신고·댓글 수 + 접수 대기 신고 수) */
    public AdminDashboardResponse getDashboard() {
        return AdminDashboardResponse.of(
                adminMemberRepository.count(),
                adminProductRepository.count(),
                adminReportRepository.count(),
                adminReportRepository.countByStatus(ReportStatus.RECEIVED),
                adminCommentRepository.count()
        );
    }
}
