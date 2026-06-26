package com.dongnemarket.product.dto;

public class ProductUpdateRequest {

	private Long categoryId;
	private String title;
	private String description;
	private Integer price;
	private String region;

	protected ProductUpdateRequest() {
	}

	public ProductUpdateRequest(Long categoryId, String title, String description, Integer price, String region) {
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

	public Integer getPrice() {
		return price;
	}

	public String getRegion() {
		return region;
	}
}
