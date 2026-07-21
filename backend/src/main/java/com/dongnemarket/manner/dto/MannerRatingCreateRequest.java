package com.dongnemarket.manner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MannerRatingCreateRequest {

    @NotNull(message = "상품 id는 필수입니다.")
    private Long productId;

    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1~5 사이여야 합니다.")
    @Max(value = 5, message = "별점은 1~5 사이여야 합니다.")
    private Integer score;

    protected MannerRatingCreateRequest() {}

    public MannerRatingCreateRequest(Long productId, Integer score) {
        this.productId = productId;
        this.score = score;
    }

    public Long getProductId() { return productId; }
    public Integer getScore() { return score; }
}
