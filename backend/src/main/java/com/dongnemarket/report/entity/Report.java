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
}, uniqueConstraints = {
        // 동시 요청(레이스 컨디션)으로 애플리케이션 레벨의 existsBy... 검증을 함께 통과해도
        // DB 유니크 제약이 최종적으로 중복 신고 저장을 막는다. NULL 컬럼은 MySQL에서 유니크 검사 대상이 아니므로
        // (상품 신고는 target_member_id가, 회원 신고는 target_product_id가 항상 NULL) 서로 간섭하지 않는다.
        @UniqueConstraint(name = "uk_reports_reporter_target_product", columnNames = {"reporter_id", "target_product_id"}),
        @UniqueConstraint(name = "uk_reports_reporter_target_member",  columnNames = {"reporter_id", "target_member_id"})
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

    /** 신고 증빙 이미지(선택). 신고 작성 시점에만 첨부 가능하며 이후에는 수정할 수 없다. */
    @Column(name = "evidence_image_url", length = 500)
    private String evidenceImageUrl;

    protected Report() {}

    private Report(Member reporter, ReportType reportType, Product targetProduct,
                   Member targetMember, ReportReason reason, String content, String evidenceImageUrl) {
        this.reporter = reporter;
        this.reportType = reportType;
        this.targetProduct = targetProduct;
        this.targetMember = targetMember;
        this.reason = reason;
        this.content = content;
        this.evidenceImageUrl = evidenceImageUrl;
        this.status = ReportStatus.RECEIVED;
    }

    public static Report ofProduct(Member reporter, Product targetProduct,
                                   ReportReason reason, String content) {
        return ofProduct(reporter, targetProduct, reason, content, null);
    }

    public static Report ofProduct(Member reporter, Product targetProduct,
                                   ReportReason reason, String content, String evidenceImageUrl) {
        return new Report(reporter, ReportType.PRODUCT, targetProduct, null, reason, content, evidenceImageUrl);
    }

    public static Report ofMember(Member reporter, Member targetMember,
                                  ReportReason reason, String content) {
        return ofMember(reporter, targetMember, reason, content, null);
    }

    public static Report ofMember(Member reporter, Member targetMember,
                                  ReportReason reason, String content, String evidenceImageUrl) {
        return new Report(reporter, ReportType.MEMBER, null, targetMember, reason, content, evidenceImageUrl);
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
    public String getEvidenceImageUrl() { return evidenceImageUrl; }
}
