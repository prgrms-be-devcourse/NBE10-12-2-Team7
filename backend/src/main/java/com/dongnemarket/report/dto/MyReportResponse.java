package com.dongnemarket.report.dto;

import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import com.dongnemarket.report.entity.ReportStatus;
import com.dongnemarket.report.entity.ReportType;

import java.time.LocalDateTime;

public class MyReportResponse {

    private final Long reportId;
    private final ReportType reportType;
    private final Long targetId;
    private final ReportReason reason;
    private final ReportStatus status;
    private final LocalDateTime createdAt;

    private MyReportResponse(Report report) {
        this.reportId = report.getId();
        this.reportType = report.getReportType();
        this.targetId = report.getReportType() == ReportType.PRODUCT
                ? report.getTargetProduct().getId()
                : report.getTargetMember().getId();
        this.reason = report.getReason();
        this.status = report.getStatus();
        this.createdAt = report.getCreatedAt();
    }

    public static MyReportResponse from(Report report) {
        return new MyReportResponse(report);
    }

    public Long getReportId() { return reportId; }
    public ReportType getReportType() { return reportType; }
    public Long getTargetId() { return targetId; }
    public ReportReason getReason() { return reason; }
    public ReportStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
