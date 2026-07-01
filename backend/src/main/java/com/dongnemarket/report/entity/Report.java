package com.dongnemarket.report.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import jakarta.persistence.*;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_reporter_id",        columnList = "reporter_id"),
        @Index(name = "idx_reports_target_product_id",  columnList = "target_product_id"),
        @Index(name = "idx_reports_target_member_id",   columnList = "target_member_id")
})
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_member_id")
    private Member targetMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_product_id")
    private Product targetProduct;

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

    private Report(Member reporter, ReportType reportType, Product targetProduct,
                   Member targetMember, ReportReason reason, String content) {
        this.reporter = reporter;
        this.reportType = reportType;
        this.targetProduct = targetProduct;
        this.targetMember = targetMember;
        this.reason = reason;
        this.content = content;
        this.status = ReportStatus.RECEIVED;
    }

    public static Report ofProduct(Member reporter, Product targetProduct,
                                   ReportReason reason, String content) {
        return new Report(reporter, ReportType.PRODUCT, targetProduct, null, reason, content);
    }

    public static Report ofMember(Member reporter, Member targetMember,
                                  ReportReason reason, String content) {
        return new Report(reporter, ReportType.MEMBER, null, targetMember, reason, content);
    }

    /** 관리자에 의한 신고 상태 변경 */
    public void changeStatus(ReportStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Member getReporter() { return reporter; }
    public Member getTargetMember() { return targetMember; }
    public Product getTargetProduct() { return targetProduct; }
    public ReportType getReportType() { return reportType; }
    public ReportReason getReason() { return reason; }
    public String getContent() { return content; }
    public ReportStatus getStatus() { return status; }
}
