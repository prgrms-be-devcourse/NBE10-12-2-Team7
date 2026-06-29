package com.dongnemarket.admin.dto;

import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import com.dongnemarket.report.entity.ReportStatus;
import com.dongnemarket.report.entity.ReportType;

import java.time.LocalDateTime;

/**
 * 관리자용 신고 응답. 신고자·대상·사유·상태 등 관리에 필요한 정보를 모두 노출한다.
 */
public class AdminReportResponse {

    private final Long reportId;
    private final Long reporterId;
    private final Long targetMemberId;
    private final Long targetProductId;
    private final ReportType reportType;
    private final ReportReason reason;
    private final String content;
    private final ReportStatus status;
    private final LocalDateTime createdAt;

    private AdminReportResponse(Report report) {
        this.reportId = report.getId();
        this.reporterId = report.getReporterId();
        this.targetMemberId = report.getTargetMemberId();
        this.targetProductId = report.getTargetProductId();
        this.reportType = report.getReportType();
        this.reason = report.getReason();
        this.content = report.getContent();
        this.status = report.getStatus();
        this.createdAt = report.getCreatedAt();
    }

    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(report);
    }

    public Long getReportId() { return reportId; }
    public Long getReporterId() { return reporterId; }
    public Long getTargetMemberId() { return targetMemberId; }
    public Long getTargetProductId() { return targetProductId; }
    public ReportType getReportType() { return reportType; }
    public ReportReason getReason() { return reason; }
    public String getContent() { return content; }
    public ReportStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
