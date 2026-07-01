package com.dongnemarket.favorite.dto;

import com.dongnemarket.favorite.entity.Favorite;

import java.time.LocalDateTime;

/** 내 관심 목록 응답. 관심 등록 정보 + 상품 요약을 함께 담는다. */
public class MyFavoriteResponse {

    private final Long favoriteId;
    private final LocalDateTime createdAt;
    private final FavoriteProductSummary product;

    private MyFavoriteResponse(Long favoriteId, LocalDateTime createdAt, FavoriteProductSummary product) {
        this.favoriteId = favoriteId;
        this.createdAt = createdAt;
        this.product = product;
    }

    public static MyFavoriteResponse from(Favorite favorite) {
        return new MyFavoriteResponse(
                favorite.getId(),
                favorite.getCreatedAt(),
                FavoriteProductSummary.from(favorite.getProduct())
        );
    }

    public Long getFavoriteId() { return favoriteId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public FavoriteProductSummary getProduct() { return product; }
}
