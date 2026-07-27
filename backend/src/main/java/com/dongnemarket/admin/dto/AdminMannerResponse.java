package com.dongnemarket.admin.dto;

import com.dongnemarket.manner.entity.MannerScore;

import java.math.BigDecimal;

/** 관리자 저신뢰 회원 모니터링 응답. */
public class AdminMannerResponse {

    private final Long memberId;
    private final String nickname;
    private final String email;
    private final BigDecimal score;

    private AdminMannerResponse(Long memberId, String nickname, String email, BigDecimal score) {
        this.memberId = memberId;
        this.nickname = nickname;
        this.email = email;
        this.score = score;
    }

    public static AdminMannerResponse from(MannerScore mannerScore) {
        return new AdminMannerResponse(
                mannerScore.getMember().getId(),
                mannerScore.getMember().getNickname(),
                mannerScore.getMember().getEmail(),
                mannerScore.getScore()
        );
    }

    public Long getMemberId() { return memberId; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public BigDecimal getScore() { return score; }
}
