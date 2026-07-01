package com.dongnemarket.product.dto;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;

import java.math.BigDecimal;

public class ProductResponse {

	private final Long productId;
	private final Long memberId;
	private final Long categoryId;
	private final String title;
	private final String description;
	private final BigDecimal price;
	private final TradeStatus tradeStatus;
	private final String region;
	private final long viewCount;
	private final boolean hidden;

	private ProductResponse(Long productId, Long memberId, Long categoryId, String title, String description,
							BigDecimal price, TradeStatus tradeStatus, String region, long viewCount, boolean hidden) {
		this.productId = productId;
		this.memberId = memberId;
		this.categoryId = categoryId;
		this.title = title;
		this.description = description;
		this.price = price;
		this.tradeStatus = tradeStatus;
		this.region = region;
		this.viewCount = viewCount;
		this.hidden = hidden;
	}

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getMember().getId(),
				product.getCategory().getId(),
				product.getTitle(),
				product.getDescription(),
				product.getPrice(),
				product.getTradeStatus(),
				product.getRegion(),
				product.getViewCount(),
				product.isHidden()
		);
	}

	public Long getProductId() {
		return productId;
	}

	public Long getMemberId() {
		return memberId;
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

	public TradeStatus getTradeStatus() {
		return tradeStatus;
	}

	public String getRegion() {
		return region;
	}

	public long getViewCount() {
		return viewCount;
	}

	public boolean isHidden() {
		return hidden;
	}
}
