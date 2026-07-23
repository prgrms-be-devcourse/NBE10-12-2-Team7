package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminReportResponse;
import com.dongnemarket.admin.dto.AdminReportStatusUpdateRequest;
import com.dongnemarket.admin.repository.AdminReportRepository;
import com.dongnemarket.global.common.event.ReportStatusChangedEvent;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminReportService {

    private final AdminReportRepository adminReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminReportService(AdminReportRepository adminReportRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.adminReportRepository = adminReportRepository;
        this.eventPublisher = eventPublisher;
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

    /** 신고 상태 변경 (RECEIVED/REVIEWING/COMPLETED/REJECTED) */
    @Transactional
    public AdminReportResponse changeReportStatus(Long reportId, AdminReportStatusUpdateRequest request) {
        Report report = adminReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        ReportStatus newStatus = parseStatus(request);
        if (report.getStatus() != newStatus) {
            report.changeStatus(newStatus);
            eventPublisher.publishEvent(new ReportStatusChangedEvent(report.getId(), newStatus));
        }
        return AdminReportResponse.from(report);
    }

    private ReportStatus parseStatus(AdminReportStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_STATUS);
        }
        try {
            return ReportStatus.valueOf(request.getStatus().trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_STATUS);
        }
    }
}
