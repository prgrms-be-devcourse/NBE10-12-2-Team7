package com.dongnemarket.manner.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * 회원별 매너온도 현재값. 회원당 1행(1:1)만 존재하며, 변화 이력은 {@link MannerScoreHistory}에 별도로 쌓는다.
 * <p>당근마켓의 매너온도(36.5도 시작)를 벤치마킹하되, 신고 도메인과 양방향으로 연동되도록 설계했다 —
 * 신고가 확정되면 피신고자 온도가 내려가고, 반대로 신고 자체가 무고성으로 판정되면 신고자 온도가 내려간다.
 */
@Entity
@Table(name = "manner_scores", uniqueConstraints = @UniqueConstraint(name = "uk_manner_scores_member", columnNames = "member_id"))
public class MannerScore extends BaseTimeEntity {

    public static final BigDecimal DEFAULT_SCORE = BigDecimal.valueOf(36.5);
    public static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    public static final BigDecimal MAX_SCORE = BigDecimal.valueOf(99.9);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal score;

    protected MannerScore() {}

    private MannerScore(Member member, BigDecimal score) {
        this.member = member;
        this.score = score;
    }

    public static MannerScore createDefault(Member member) {
        return new MannerScore(member, DEFAULT_SCORE);
    }

    /**
     * 온도를 delta만큼 변화시키고, {@link #MIN_SCORE}~{@link #MAX_SCORE} 범위로 clamp한 뒤
     * 실제로 반영된 변화량(clamp 이후 실제 변화분)을 반환한다.
     * <p>실제 반영분을 반환하는 이유: {@link MannerScoreHistory}에는 "의도한 delta"가 아니라
     * "실제로 적용된 변화량"을 기록해야 이력 합계와 현재 점수가 항상 일치한다.
     */
    public BigDecimal applyDelta(BigDecimal delta) {
        BigDecimal before = this.score;
        BigDecimal candidate = this.score.add(delta);
        if (candidate.compareTo(MIN_SCORE) < 0) {
            candidate = MIN_SCORE;
        } else if (candidate.compareTo(MAX_SCORE) > 0) {
            candidate = MAX_SCORE;
        }
        this.score = candidate;
        return candidate.subtract(before);
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public BigDecimal getScore() { return score; }
}
