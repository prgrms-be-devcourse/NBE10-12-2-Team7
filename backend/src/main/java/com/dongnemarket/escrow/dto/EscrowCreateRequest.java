package com.dongnemarket.escrow.dto;

import jakarta.validation.constraints.NotNull;

public class EscrowCreateRequest {

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    protected EscrowCreateRequest() {
    }

    public EscrowCreateRequest(Long productId) {
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
