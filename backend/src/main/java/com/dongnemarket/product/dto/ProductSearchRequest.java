package com.dongnemarket.product.dto;

import java.math.BigDecimal;

public class ProductSearchRequest {

	private final String keyword;
	private final Long categoryId;
	private final BigDecimal minPrice;
	private final BigDecimal maxPrice;
	private final String tradeStatus;

	public ProductSearchRequest(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String tradeStatus) {
		this.keyword = keyword;
		this.categoryId = categoryId;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.tradeStatus = tradeStatus;
	}

	public String getKeyword() {
		return keyword;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public BigDecimal getMinPrice() {
		return minPrice;
	}

	public BigDecimal getMaxPrice() {
		return maxPrice;
	}

	public String getTradeStatus() {
		return tradeStatus;
	}

}
