package com.dongnemarket.product.dto;

public class ProductStatusUpdateRequest {

	private String tradeStatus;

	public ProductStatusUpdateRequest() {
	}

	public ProductStatusUpdateRequest(String tradeStatus) {
		this.tradeStatus = tradeStatus;
	}

	public String getTradeStatus() {
		return tradeStatus;
	}
}
