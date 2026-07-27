package com.dongnemarket.manner.scheduler;

import com.dongnemarket.manner.entity.MannerScore;
import com.dongnemarket.manner.service.MannerScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 매너온도 시간 경과 자동 회복 배치. 매일 새벽 실행되어, 기본값(36.5) 미만인 회원 중
 * 최근 30일간 신고 확정/무고성 페널티(감점 이력)가 없는 회원만 골라, 같은 기간 정상 거래
 * 완료 건수에 비례해 온도를 회복시킨다. 단순 "시간이 지나면 회복"이 아니라 "최근에도
 * 실제로 문제없이 거래했는가"를 반영한 회복이다.
 */
@Component
public class MannerScoreRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(MannerScoreRecoveryScheduler.class);
    private static final long LOOKBACK_DAYS = 30;

    private final MannerScoreService mannerScoreService;

    public MannerScoreRecoveryScheduler(MannerScoreService mannerScoreService) {
        this.mannerScoreService = mannerScoreService;
    }

    /** 매일 새벽 3시 실행 (cron: 초 분 시 일 월 요일) */
    @Scheduled(cron = "0 0 3 * * *")
    public void recoverEligibleMembers() {
        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<MannerScore> candidates = mannerScoreService.findRecoveryCandidates();

        int recovered = 0;
        for (MannerScore mannerScore : candidates) {
            Long memberId = mannerScore.getMember().getId();
            if (mannerScoreService.hasPenaltySince(memberId, since)) {
                continue; // 최근 30일 내 감점 이력이 있으면 이번 배치는 건너뛴다
            }
            long tradeCount = mannerScoreService.countCompletedTradesSince(memberId, since);
            if (tradeCount <= 0) {
                continue;
            }
            mannerScoreService.applyTimeRecoveryTowardDefault(memberId, tradeCount);
            recovered++;
        }
        log.info("매너온도 회복 배치 완료: 대상 {}명 중 {}명 회복 적용", candidates.size(), recovered);
    }
}
