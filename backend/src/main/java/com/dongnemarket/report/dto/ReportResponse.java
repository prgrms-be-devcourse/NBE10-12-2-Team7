package com.dongnemarket.report.dto;

import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import com.dongnemarket.report.entity.ReportStatus;
import com.dongnemarket.report.entity.ReportType;

import java.time.LocalDateTime;

public class ReportResponse {

    private final Long reportId;
    private final ReportType reportType;
    private final ReportReason reason;
    private final ReportStatus status;
    private final String evidenceImageUrl;
    private final LocalDateTime createdAt;

    private ReportResponse(Report report) {
        this.reportId = report.getId();
        this.reportType = report.getReportType();
        this.reason = report.getReason();
        this.status = report.getStatus();
        this.evidenceImageUrl = report.getEvidenceImageUrl();
        this.createdAt = report.getCreatedAt();
    }

    public static ReportResponse from(Report report) {
        return new ReportResponse(report);
    }

    public Long getReportId() { return reportId; }
    public ReportType getReportType() { return reportType; }
    public ReportReason getReason() { return reason; }
    public ReportStatus getStatus() { return status; }
    public String getEvidenceImageUrl() { return evidenceImageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
