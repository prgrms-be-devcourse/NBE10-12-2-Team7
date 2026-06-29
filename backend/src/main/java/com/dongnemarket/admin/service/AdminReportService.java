package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminReportResponse;
import com.dongnemarket.admin.repository.AdminReportRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.report.entity.Report;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminReportService {

    private final AdminReportRepository adminReportRepository;

    public AdminReportService(AdminReportRepository adminReportRepository) {
        this.adminReportRepository = adminReportRepository;
    }

    /** 전체 신고 목록 */
    public List<AdminReportResponse> getReports() {
        return adminReportRepository.findAll().stream()
                .map(AdminReportResponse::from)
                .toList();
    }

    /** 신고 단건 상세 */
    public AdminReportResponse getReport(Long reportId) {
        Report report = adminReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        return AdminReportResponse.from(report);
    }
}
