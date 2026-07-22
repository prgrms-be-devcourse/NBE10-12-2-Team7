package com.dongnemarket.escrow.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "escrows")
public class Escrow extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Member buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EscrowStatus status;

    @Column
    private LocalDateTime closedAt;

    protected Escrow() {
    }

    private Escrow(Product product, Member buyer, Member seller, BigDecimal amount) {
        this.product = product;
        this.buyer = buyer;
        this.seller = seller;
        this.amount = amount;
        this.status = EscrowStatus.IN_ESCROW;
    }

    /** 거래 시작: 대금을 예치한 상태(IN_ESCROW)로 생성. amount 는 상품가 스냅샷(D3). */
    public static Escrow create(Product product, Member buyer, Member seller, BigDecimal amount) {
        return new Escrow(product, buyer, seller, amount);
    }

    /** 구매확정: 예치 상태에서만 DONE 으로 전이. */
    public void confirm() {
        if (this.status != EscrowStatus.IN_ESCROW) {
            throw new BusinessException(ErrorCode.ESCROW_NOT_IN_ESCROW);
        }
        this.status = EscrowStatus.DONE;
        this.closedAt = LocalDateTime.now();
    }

    /** 취소·환불: 예치 상태에서만 CANCELED 로 전이. */
    public void cancel() {
        if (this.status != EscrowStatus.IN_ESCROW) {
            throw new BusinessException(ErrorCode.ESCROW_NOT_CANCELABLE);
        }
        this.status = EscrowStatus.CANCELED;
        this.closedAt = LocalDateTime.now();
    }

    /** 이 거래의 구매자 본인인지 확인(확정·취소 권한 검증용). */
    public boolean isBuyer(Long memberId) {
        return this.buyer.getId().equals(memberId);
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Member getBuyer() {
        return buyer;
    }

    public Member getSeller() {
        return seller;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public EscrowStatus getStatus() {
        return status;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}