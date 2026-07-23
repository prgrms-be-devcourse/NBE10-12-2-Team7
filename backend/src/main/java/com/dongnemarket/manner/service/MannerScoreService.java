package com.dongnemarket.manner.service;

import com.dongnemarket.manner.entity.MannerScore;
import com.dongnemarket.manner.entity.MannerScoreChangeReason;
import com.dongnemarket.manner.entity.MannerScoreHistory;
import com.dongnemarket.manner.repository.MannerScoreHistoryRepository;
import com.dongnemarket.manner.repository.MannerScoreRepository;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 매너온도 핵심 조정 로직. 모든 온도 변화(별점/거래완료/신고확정/무고성페널티/시간회복)는
 * 반드시 {@link #adjust}를 통해서만 반영되어, 현재값({@link MannerScore})과 변화 이력
 * ({@link MannerScoreHistory})이 항상 일치하도록 보장한다.
 */
@Service
@Transactional(readOnly = true)
public class MannerScoreService {

    /** 신고 사유별 확정 시 하락폭. 값이 클수록 심각한 사유로 간주한다. */
    private static final BigDecimal PENALTY_SEVERE = BigDecimal.valueOf(-1.0);
    private static final BigDecimal PENALTY_MODERATE = BigDecimal.valueOf(-0.5);
    private static final BigDecimal PENALTY_MINOR = BigDecimal.valueOf(-0.3);
    private static final BigDecimal FALSE_REPORT_PENALTY = BigDecimal.valueOf(-0.3);
    private static final BigDecimal TRADE_COMPLETED_BONUS = BigDecimal.valueOf(0.1);
    private static final BigDecimal DAILY_RECOVERY_STEP = BigDecimal.valueOf(0.1);

    private final MannerScoreRepository mannerScoreRepository;
    private final MannerScoreHistoryRepository mannerScoreHistoryRepository;
    private final MemberRepository memberRepository;

    public MannerScoreService(MannerScoreRepository mannerScoreRepository,
                               MannerScoreHistoryRepository mannerScoreHistoryRepository,
                               MemberRepository memberRepository) {
        this.mannerScoreRepository = mannerScoreRepository;
        this.mannerScoreHistoryRepository = mannerScoreHistoryRepository;
        this.memberRepository = memberRepository;
    }

    /** 회원의 매너온도를 조회한다. 아직 레코드가 없으면(가입 직후 등) 기본값(36.5)을 생성해서 반환한다. */
    @Transactional
    public MannerScore getOrCreate(Long memberId) {
        return mannerScoreRepository.findByMember_Id(memberId)
                .orElseGet(() -> {
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
                    return mannerScoreRepository.save(MannerScore.createDefault(member));
                });
    }

    /** 별점 반영: (별점-3)/10 정도의 완만한 가중치로 상승/하락. 3점(보통)은 변화 없음. */
    @Transactional
    public void applyRating(Long rateeId, int ratingScore) {
        BigDecimal delta = BigDecimal.valueOf(ratingScore - 3).movePointLeft(1); // 1~5 -> -0.2~+0.2
        adjust(rateeId, delta, MannerScoreChangeReason.RATING_RECEIVED, null);
    }

    /** 정상 거래 완료 보너스 */
    @Transactional
    public void applyTradeCompleted(Long sellerId) {
        adjust(sellerId, TRADE_COMPLETED_BONUS, MannerScoreChangeReason.TRADE_COMPLETED, null);
    }

    /** 신고 확정(COMPLETED) 시 피신고자 온도 하락. 사유가 심각할수록 더 크게 하락한다. */
    @Transactional
    public void applyReportConfirmed(Long targetMemberId, BigDecimal severity, Long reportId) {
        adjust(targetMemberId, severity.negate().abs().negate(), MannerScoreChangeReason.REPORT_CONFIRMED, reportId);
    }

    /** 무고성 신고(REJECTED) 판정 시 신고자 온도 하락 — 허위 신고 억제 */
    @Transactional
    public void applyFalseReportPenalty(Long reporterId, Long reportId) {
        adjust(reporterId, FALSE_REPORT_PENALTY, MannerScoreChangeReason.FALSE_REPORT_PENALTY, reportId);
    }

    /**
     * 시간 경과 자동 회복(스케줄링 배치 전용). 최근 거래 실적(tradeCount)에 비례해 회복시키되,
     * 순수 시간 경과 회복은 기본값(36.5)을 넘어서지 않도록 캡을 건다 — 그 이상은 실제 별점/거래완료
     * 보너스로만 올라가야 한다는 취지다.
     */
    @Transactional
    public void applyTimeRecoveryTowardDefault(Long memberId, long tradeCount) {
        if (tradeCount <= 0) {
            return;
        }
        MannerScore mannerScore = getOrCreate(memberId);
        BigDecimal remaining = MannerScore.DEFAULT_SCORE.subtract(mannerScore.getScore());
        if (remaining.signum() <= 0) {
            return; // 이미 기본값 이상이면 시간 경과 회복 대상이 아니다
        }
        BigDecimal requested = DAILY_RECOVERY_STEP.multiply(BigDecimal.valueOf(tradeCount));
        BigDecimal delta = requested.min(remaining);
        adjust(memberId, delta, MannerScoreChangeReason.TIME_RECOVERY, null);
    }

    /** 신고 사유(ReportReason 이름 문자열)에 따른 하락폭을 결정한다. */
    public BigDecimal severityOf(String reportReasonName) {
        return switch (reportReasonName) {
            case "FAKE_ITEM", "FRAUD_SUSPECTED" -> PENALTY_SEVERE;
            case "PROHIBITED_ITEM", "INAPPROPRIATE_CONTENT" -> PENALTY_MODERATE;
            default -> PENALTY_MINOR;
        };
    }

    /** 최근 90일 내 REPORT_CONFIRMED(정당 신고 확정) 건수 — 계정 자동 정지 판단에 사용 */
    public long countConfirmedReportsSince(Long memberId, LocalDateTime since) {
        return mannerScoreHistoryRepository.countByMemberAndReasonSince(memberId, MannerScoreChangeReason.REPORT_CONFIRMED, since);
    }

    /** 최근 N일 내 정상 거래 완료 건수 — 회복 배치의 회복량 계산 근거 */
    public long countCompletedTradesSince(Long memberId, LocalDateTime since) {
        return mannerScoreHistoryRepository.countByMemberAndReasonSince(memberId, MannerScoreChangeReason.TRADE_COMPLETED, since);
    }

    /** 최근 N일 내 감점 이력(신고확정/무고성페널티)이 있는지 — 있으면 회복 배치 대상에서 제외 */
    public boolean hasPenaltySince(Long memberId, LocalDateTime since) {
        return mannerScoreHistoryRepository.hasPenaltySince(memberId, since);
    }

    public List<MannerScoreHistory> getHistory(Long memberId) {
        return mannerScoreHistoryRepository.findAllByMember_IdOrderByCreatedAtDesc(memberId);
    }

    public List<MannerScore> findLowTrustMembers(BigDecimal threshold) {
        return mannerScoreRepository.findAllByScoreLessThanEqualOrderByScoreAsc(threshold);
    }

    /** 회복 배치 대상(기본값 36.5 미만) 조회 */
    public List<MannerScore> findRecoveryCandidates() {
        return mannerScoreRepository.findAllByScoreLessThan(MannerScore.DEFAULT_SCORE);
    }

    /** 회원의 계정 상태를 정지로 전환한다(계정 자동 정지 전용, 기존 SUSPENDED enum 재사용). */
    @Transactional
    public void suspend(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.changeStatus(MemberStatus.SUSPENDED);
    }

    private void adjust(Long memberId, BigDecimal delta, MannerScoreChangeReason reason, Long relatedReportId) {
        MannerScore mannerScore = getOrCreate(memberId);
        BigDecimal actualChange = mannerScore.applyDelta(delta);
        if (actualChange.compareTo(BigDecimal.ZERO) == 0) {
            return; // 이미 하한/상한이라 실제 변화가 없으면 이력도 남기지 않는다
        }
        MannerScoreHistory history = relatedReportId != null
                ? MannerScoreHistory.ofReport(mannerScore.getMember(), actualChange, reason, relatedReportId)
                : MannerScoreHistory.of(mannerScore.getMember(), actualChange, reason);
        mannerScoreHistoryRepository.save(history);
    }
}
