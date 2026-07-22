package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminReportResponse;
import com.dongnemarket.admin.dto.AdminReportStatusUpdateRequest;
import com.dongnemarket.admin.repository.AdminReportRepository;
import com.dongnemarket.global.common.event.ReportStatusChangedEvent;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.manner.service.MannerScoreService;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminReportService {

    private final AdminReportRepository adminReportRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MannerScoreService mannerScoreService;

    public AdminReportService(AdminReportRepository adminReportRepository,
                               ApplicationEventPublisher eventPublisher,
                               MannerScoreService mannerScoreService) {
        this.adminReportRepository = adminReportRepository;
        this.eventPublisher = eventPublisher;
        this.mannerScoreService = mannerScoreService;
    }

    /**
     * 전체 신고 목록. 미처리(RECEIVED/REVIEWING) 건을 먼저 보여주고, 그 안에서는
     * 신고 대상의 매너온도가 낮을수록(=이미 신뢰도가 떨어지는 회원일수록) 우선 노출되도록 가중 정렬한다.
     * 이미 처리된(COMPLETED/REJECTED) 건은 최신순으로 뒤에 배치한다.
     */
    public List<AdminReportResponse> getReports() {
        List<Report> reports = adminReportRepository.findAll();
        Map<Long, BigDecimal> targetScoreByMemberId = loadTargetTrustScores(reports);

        return reports.stream()
                .sorted(Comparator
                        .comparing((Report r) -> triageRank(r.getStatus()))
                        .thenComparing(r -> targetScoreByMemberId.get(r.resolveTargetMemberId()))
                        .thenComparing(Report::getCreatedAt, Comparator.reverseOrder()))
                .map(AdminReportResponse::from)
                .toList();
    }

    private Map<Long, BigDecimal> loadTargetTrustScores(List<Report> reports) {
        Set<Long> targetMemberIds = reports.stream()
                .map(Report::resolveTargetMemberId)
                .collect(Collectors.toSet());
        return mannerScoreService.getScoresByMemberIds(targetMemberIds);
    }

    /** 미처리 건(RECEIVED/REVIEWING)을 항상 먼저, 처리 완료 건(COMPLETED/REJECTED)을 뒤로 보낸다. */
    private int triageRank(ReportStatus status) {
        return switch (status) {
            case RECEIVED, REVIEWING -> 0;
            case COMPLETED, REJECTED -> 1;
        };
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
