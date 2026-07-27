package com.dongnemarket.manner.dto;

import com.dongnemarket.manner.entity.MannerScore;

import java.math.BigDecimal;

/** 상품 상세·채팅방 등에서 노출하는 공개 매너온도 요약. */
public class MannerScoreResponse {

    private final Long memberId;
    private final BigDecimal score;

    private MannerScoreResponse(Long memberId, BigDecimal score) {
        this.memberId = memberId;
        this.score = score;
    }

    public static MannerScoreResponse from(MannerScore mannerScore) {
        return new MannerScoreResponse(mannerScore.getMember().getId(), mannerScore.getScore());
    }

    public Long getMemberId() { return memberId; }
    public BigDecimal getScore() { return score; }
}
