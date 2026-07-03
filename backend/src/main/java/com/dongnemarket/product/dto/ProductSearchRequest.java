package com.dongnemarket.product.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Size;

public class ProductSearchRequest {

	private final String keyword;
	private final Long categoryId;
	private final BigDecimal minPrice;
	private final BigDecimal maxPrice;
	private final String tradeStatus;
	@Size(max = 2, message = "지역 필터는 최대 2개까지 선택할 수 있습니다.")
	private final List<String> regions;

	public ProductSearchRequest(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String tradeStatus) {
		this(keyword, categoryId, minPrice, maxPrice, tradeStatus, null);
	}

	public ProductSearchRequest(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
								String tradeStatus, List<String> regions) {
		this.keyword = keyword;
		this.categoryId = categoryId;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.tradeStatus = tradeStatus;
		this.regions = regions;
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

	public List<String> getRegions() {
		return regions;
	}

}
