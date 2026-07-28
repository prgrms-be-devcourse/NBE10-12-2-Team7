package com.dongnemarket.product.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class ProductCreateRequest {

	private Long categoryId;
	private String title;
	private String description;
	private BigDecimal price;
	private String regionCode;
	@NotEmpty(message = "상품 이미지는 1장 이상 등록해야 합니다.")
	@Size(max = 5, message = "상품 이미지는 최대 5장까지 등록할 수 있습니다.")
	private List<@NotBlank(message = "상품 이미지 URL은 공백일 수 없습니다.") String> imageUrls;
	private int thumbnailIndex;

	protected ProductCreateRequest() {
	}

	public ProductCreateRequest(Long categoryId, String title, String description, BigDecimal price, String regionCode,
									List<String> imageUrls, int thumbnailIndex) {
		this.categoryId = categoryId;
		this.title = title;
		this.description = description;
		this.price = price;
		this.regionCode = regionCode;
		this.imageUrls = imageUrls;
		this.thumbnailIndex = thumbnailIndex;
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

	public String getRegionCode() {
		return regionCode;
	}

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public int getThumbnailIndex() {
		return thumbnailIndex;
	}

}
