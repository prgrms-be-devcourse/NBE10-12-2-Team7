package com.dongnemarket.product.dto;

import java.math.BigDecimal;

public class ProductCreateRequest {

	private Long categoryId;
	private String title;
	private String description;
	private BigDecimal price;
	private String region;

	protected ProductCreateRequest() {
	}

	public ProductCreateRequest(Long categoryId, String title, String description, BigDecimal price, String region) {
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
