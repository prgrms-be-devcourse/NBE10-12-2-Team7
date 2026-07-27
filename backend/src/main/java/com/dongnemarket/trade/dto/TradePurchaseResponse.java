package com.dongnemarket.trade.dto;

import com.dongnemarket.chat.entity.ChatRoom;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 구매내역 한 건. 채팅방을 통해서만 "내가 산 상품"을 알 수 있어(스키마상 별도 구매자 필드 없음) ChatRoom 기준으로 만든다. */
public class TradePurchaseResponse {

    private final Long productId;
    private final String title;
    private final String thumbnailUrl;
    private final BigDecimal price;
    private final String sellerNickname;
    private final Long roomId;
    private final LocalDateTime completedAt;

    private TradePurchaseResponse(Long productId, String title, String thumbnailUrl, BigDecimal price,
                                  String sellerNickname, Long roomId, LocalDateTime completedAt) {
        this.productId = productId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.price = price;
        this.sellerNickname = sellerNickname;
        this.roomId = roomId;
        this.completedAt = completedAt;
    }

    public static TradePurchaseResponse from(ChatRoom room) {
        return new TradePurchaseResponse(
                room.getProduct().getId(),
                room.getProduct().getTitle(),
                room.getProduct().getThumbnailUrl(),
                room.getProduct().getPrice(),
                room.getSeller().getDisplayNickname(),
                room.getId(),
                room.getProduct().getCompletedAt()
        );
    }

    public Long getProductId() { return productId; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public BigDecimal getPrice() { return price; }
    public String getSellerNickname() { return sellerNickname; }
    public Long getRoomId() { return roomId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
