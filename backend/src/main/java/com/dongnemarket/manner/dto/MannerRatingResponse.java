package com.dongnemarket.manner.dto;

import com.dongnemarket.manner.entity.MannerRating;

public class MannerRatingResponse {

    private final Long ratingId;
    private final Long productId;
    private final Long rateeId;
    private final int score;

    private MannerRatingResponse(Long ratingId, Long productId, Long rateeId, int score) {
        this.ratingId = ratingId;
        this.productId = productId;
        this.rateeId = rateeId;
        this.score = score;
    }

    public static MannerRatingResponse from(MannerRating rating) {
        return new MannerRatingResponse(
                rating.getId(),
                rating.getProduct().getId(),
                rating.getRatee().getId(),
                rating.getScore()
        );
    }

    public Long getRatingId() { return ratingId; }
    public Long getProductId() { return productId; }
    public Long getRateeId() { return rateeId; }
    public int getScore() { return score; }
}
