package com.dongnemarket.product.dto;

public class ProductSearchRequest {

	private final String keyword;
	private final Long categoryId;
	private final Integer minPrice;
	private final Integer maxPrice;
	private final String tradeStatus;

	public ProductSearchRequest(String keyword, Long categoryId, Integer minPrice, Integer maxPrice, String tradeStatus) {
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

	public Integer getMinPrice() {
		return minPrice;
	}

	public Integer getMaxPrice() {
		return maxPrice;
	}

	public String getTradeStatus() {
		return tradeStatus;
	}
}
