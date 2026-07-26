package com.dongnemarket.product.dto;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;

import java.math.BigDecimal;

public class ProductSummaryResponse {

	private final Long productId;
	private final Long memberId;
	private final Long categoryId;
	private final String title;
	private final BigDecimal price;
	private final TradeStatus tradeStatus;
	private final Long regionId;
	private final String regionName;
	private final long viewCount;
	private final int favoriteCount;
	private final String thumbnailUrl;
	private final boolean hidden;

	private ProductSummaryResponse(Long productId, Long memberId, Long categoryId, String title,
								   BigDecimal price, TradeStatus tradeStatus, Long regionId, String regionName,
								   long viewCount, int favoriteCount, String thumbnailUrl, boolean hidden) {
		this.productId = productId;
		this.memberId = memberId;
		this.categoryId = categoryId;
		this.title = title;
		this.price = price;
		this.tradeStatus = tradeStatus;
		this.regionId = regionId;
		this.regionName = regionName;
		this.viewCount = viewCount;
		this.favoriteCount = favoriteCount;
		this.thumbnailUrl = thumbnailUrl;
		this.hidden = hidden;
	}

	public static ProductSummaryResponse from(Product product) {
		return new ProductSummaryResponse(
				product.getId(),
				product.getMember().getId(),
				product.getCategory().getId(),
				product.getTitle(),
				product.getPrice(),
				product.getTradeStatus(),
				product.getRegion().getId(),
				product.getRegion().getFullName(),
				product.getViewCount(),
				product.getFavoriteCount(),
				product.getThumbnailUrl(),
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

	public BigDecimal getPrice() {
		return price;
	}

	public TradeStatus getTradeStatus() {
		return tradeStatus;
	}

	public Long getRegionId() {
		return regionId;
	}

	public String getRegionName() {
		return regionName;
	}

	public long getViewCount() {
		return viewCount;
	}

	public int getFavoriteCount() {
		return favoriteCount;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public boolean isHidden() {
		return hidden;
	}
}
