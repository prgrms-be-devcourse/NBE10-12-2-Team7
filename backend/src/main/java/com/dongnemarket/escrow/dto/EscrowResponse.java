package com.dongnemarket.escrow.dto;

import com.dongnemarket.escrow.entity.Escrow;
import com.dongnemarket.escrow.entity.EscrowStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EscrowResponse {

    private final Long escrowId;
    private final Long productId;
    private final Long buyerId;
    private final Long sellerId;
    private final BigDecimal amount;
    private final EscrowStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime closedAt;

    private EscrowResponse(Long escrowId, Long productId, Long buyerId, Long sellerId,
                           BigDecimal amount, EscrowStatus status,
                           LocalDateTime createdAt, LocalDateTime closedAt) {
        this.escrowId = escrowId;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
    }

    public static EscrowResponse from(Escrow escrow) {
        return new EscrowResponse(
                escrow.getId(),
                escrow.getProduct().getId(),
                escrow.getBuyer().getId(),
                escrow.getSeller().getId(),
                escrow.getAmount(),
                escrow.getStatus(),
                escrow.getCreatedAt(),
                escrow.getClosedAt()
        );
    }

    public Long getEscrowId() {
        return escrowId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public EscrowStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}
