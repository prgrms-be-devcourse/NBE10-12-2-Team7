package com.dongnemarket.product.dto;

import java.util.List;

public class ProductImageUploadResponse {

	private final List<String> imageUrls;

	private ProductImageUploadResponse(List<String> imageUrls) {
		this.imageUrls = imageUrls;
	}

	public static ProductImageUploadResponse of(List<String> imageUrls) {
		return new ProductImageUploadResponse(imageUrls);
	}

	public List<String> getImageUrls() {
		return imageUrls;
	}
}
