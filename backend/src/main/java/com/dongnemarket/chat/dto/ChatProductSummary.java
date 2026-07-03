package com.dongnemarket.chat.dto;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;

import java.math.BigDecimal;

/** 채팅방 목록에 표시할 상품 요약(대표사진·제목·가격·거래상태). viewCount 등 목록에 불필요한 필드는 담지 않는다. */
public class ChatProductSummary {

    private final Long productId;
    private final String title;
    private final BigDecimal price;
    private final TradeStatus tradeStatus;
    private final String thumbnailUrl;

    private ChatProductSummary(Long productId, String title, BigDecimal price,
                               TradeStatus tradeStatus, String thumbnailUrl) {
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.tradeStatus = tradeStatus;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static ChatProductSummary from(Product product) {
        return new ChatProductSummary(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getTradeStatus(),
                product.getThumbnailUrl()
        );
    }

    public Long getProductId() { return productId; }
    public String getTitle() { return title; }
    public BigDecimal getPrice() { return price; }
    public TradeStatus getTradeStatus() { return tradeStatus; }
    public String getThumbnailUrl() { return thumbnailUrl; }
}
