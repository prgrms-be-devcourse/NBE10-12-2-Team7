package com.dongnemarket.manner.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 매너온도 변화 이력(감사로그). 현재값은 {@link MannerScore} 한 행만 갖고 있어서 "왜 지금 이 점수인지"를
 * 알 수 없는데, 이 테이블이 그 근거를 시간순으로 남긴다. 관리자 모니터링 화면의 타임라인과,
 * 스케줄링 회복 로직의 "최근 30일 감점 이력 없음" 판단 근거로도 쓰인다.
 */
@Entity
@Table(name = "manner_score_histories", indexes = {
        @Index(name = "idx_manner_score_histories_member_id", columnList = "member_id"),
        @Index(name = "idx_manner_score_histories_member_reason_created", columnList = "member_id, reason, created_at")
})
public class MannerScoreHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 실제로 반영된 변화량(clamp 이후). {@link MannerScore#applyDelta}의 반환값을 그대로 저장한다. */
    @Column(name = "change_amount", nullable = false, precision = 4, scale = 1)
    private BigDecimal changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MannerScoreChangeReason reason;

    /** 신고 확정/무고성 페널티일 때만 채워지는 근거 신고 id. 그 외 사유는 null. */
    @Column(name = "related_report_id")
    private Long relatedReportId;

    protected MannerScoreHistory() {}

    private MannerScoreHistory(Member member, BigDecimal changeAmount, MannerScoreChangeReason reason, Long relatedReportId) {
        this.member = member;
        this.changeAmount = changeAmount;
        this.reason = reason;
        this.relatedReportId = relatedReportId;
    }

    public static MannerScoreHistory of(Member member, BigDecimal changeAmount, MannerScoreChangeReason reason) {
        return new MannerScoreHistory(member, changeAmount, reason, null);
    }

    public static MannerScoreHistory ofReport(Member member, BigDecimal changeAmount, MannerScoreChangeReason reason, Long reportId) {
        return new MannerScoreHistory(member, changeAmount, reason, reportId);
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public MannerScoreChangeReason getReason() { return reason; }
    public Long getRelatedReportId() { return relatedReportId; }
}
