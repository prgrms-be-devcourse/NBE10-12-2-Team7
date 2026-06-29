package com.dongnemarket.admin.dto;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;

import java.time.LocalDateTime;

/**
 * 관리자용 상품 응답. 관리 목적상 hidden·deletedAt·createdAt 까지 포함한다.
 */
public class AdminProductResponse {

    private final Long productId;
    private final Long memberId;
    private final Long categoryId;
    private final String title;
    private final String description;
    private final Integer price;
    private final TradeStatus tradeStatus;
    private final String region;
    private final long viewCount;
    private final boolean hidden;
    private final LocalDateTime deletedAt;
    private final LocalDateTime createdAt;

    private AdminProductResponse(Long productId, Long memberId, Long categoryId, String title, String description,
                                 Integer price, TradeStatus tradeStatus, String region, long viewCount,
                                 boolean hidden, LocalDateTime deletedAt, LocalDateTime createdAt) {
        this.productId = productId;
        this.memberId = memberId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.tradeStatus = tradeStatus;
        this.region = region;
        this.viewCount = viewCount;
        this.hidden = hidden;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
    }

    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(
                product.getId(),
                product.getMember().getId(),
                product.getCategory().getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getTradeStatus(),
                product.getRegion(),
                product.getViewCount(),
                product.isHidden(),
                product.getDeletedAt(),
                product.getCreatedAt()
        );
    }

    public Long getProductId() { return productId; }
    public Long getMemberId() { return memberId; }
    public Long getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getPrice() { return price; }
    public TradeStatus getTradeStatus() { return tradeStatus; }
    public String getRegion() { return region; }
    public long getViewCount() { return viewCount; }
    public boolean isHidden() { return hidden; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}