package com.dongnemarket.product.dto;

import java.math.BigDecimal;

public class ProductSearchRequest {

	private final String keyword;
	private final Long categoryId;
	private final BigDecimal minPrice;
	private final BigDecimal maxPrice;
	private final String tradeStatus;
	// 선택한 지역 id. 해당 지역 하위 전체 동을 code prefix로 매칭한다.
	private final Long regionId;

	public ProductSearchRequest(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String tradeStatus) {
		this(keyword, categoryId, minPrice, maxPrice, tradeStatus, null);
	}

	public ProductSearchRequest(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
								String tradeStatus, Long regionId) {
		this.keyword = keyword;
		this.categoryId = categoryId;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.tradeStatus = tradeStatus;
		this.regionId = regionId;
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

	public Long getRegionId() {
		return regionId;
	}

}
