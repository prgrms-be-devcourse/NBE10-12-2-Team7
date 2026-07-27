package com.dongnemarket.manner.dto;

import com.dongnemarket.manner.entity.MannerScoreChangeReason;
import com.dongnemarket.manner.entity.MannerScoreHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 내정보 페이지의 매너온도 변화 이력 타임라인용 응답. */
public class MannerScoreHistoryResponse {

    private final Long historyId;
    private final BigDecimal changeAmount;
    private final MannerScoreChangeReason reason;
    private final Long relatedReportId;
    private final LocalDateTime createdAt;

    private MannerScoreHistoryResponse(Long historyId, BigDecimal changeAmount, MannerScoreChangeReason reason,
                                        Long relatedReportId, LocalDateTime createdAt) {
        this.historyId = historyId;
        this.changeAmount = changeAmount;
        this.reason = reason;
        this.relatedReportId = relatedReportId;
        this.createdAt = createdAt;
    }

    public static MannerScoreHistoryResponse from(MannerScoreHistory history) {
        return new MannerScoreHistoryResponse(
                history.getId(),
                history.getChangeAmount(),
                history.getReason(),
                history.getRelatedReportId(),
                history.getCreatedAt()
        );
    }

    public Long getHistoryId() { return historyId; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public MannerScoreChangeReason getReason() { return reason; }
    public Long getRelatedReportId() { return relatedReportId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
