package com.dongnemarket.report.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "reports")
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "target_member_id")
    private Long targetMemberId;

    @Column(name = "target_product_id")
    private Long targetProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportReason reason;

    @Column(length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status;

    protected Report() {}

    private Report(Long reporterId, ReportType reportType, Long targetProductId,
                   Long targetMemberId, ReportReason reason, String content) {
        this.reporterId = reporterId;
        this.reportType = reportType;
        this.targetProductId = targetProductId;
        this.targetMemberId = targetMemberId;
        this.reason = reason;
        this.content = content;
        this.status = ReportStatus.RECEIVED;
    }

    public static Report ofProduct(Long reporterId, Long targetProductId,
                                   ReportReason reason, String content) {
        return new Report(reporterId, ReportType.PRODUCT, targetProductId, null, reason, content);
    }

    public static Report ofMember(Long reporterId, Long targetMemberId,
                                  ReportReason reason, String content) {
        return new Report(reporterId, ReportType.MEMBER, null, targetMemberId, reason, content);
    }

    public Long getId() { return id; }
    public Long getReporterId() { return reporterId; }
    public Long getTargetMemberId() { return targetMemberId; }
    public Long getTargetProductId() { return targetProductId; }
    public ReportType getReportType() { return reportType; }
    public ReportReason getReason() { return reason; }
    public String getContent() { return content; }
    public ReportStatus getStatus() { return status; }
}
