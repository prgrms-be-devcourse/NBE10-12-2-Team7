package com.dongnemarket.product.dto;

import java.math.BigDecimal;

public class ProductUpdateRequest {

	private Long categoryId;
	private String title;
	private String description;
	private BigDecimal price;
	private String region;

	protected ProductUpdateRequest() {
	}

	public ProductUpdateRequest(Long categoryId, String title, String description, BigDecimal price, String region) {
		this.categoryId = categoryId;
		this.title = title;
		this.description = description;
		this.price = price;
		this.region = region;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public String getRegion() {
		return region;
	}

}
