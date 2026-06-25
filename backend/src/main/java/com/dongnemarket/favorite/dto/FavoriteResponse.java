package com.dongnemarket.favorite.dto;

import com.dongnemarket.favorite.entity.Favorite;

import java.time.LocalDateTime;

public class FavoriteResponse {

    private final Long id;
    private final Long productId;
    private final LocalDateTime createdAt;

    private FavoriteResponse(Long id, Long productId, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getProductId(),
                favorite.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
