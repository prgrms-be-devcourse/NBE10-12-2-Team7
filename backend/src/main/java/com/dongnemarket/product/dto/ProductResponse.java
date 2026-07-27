package com.dongnemarket.product.dto;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;

import java.math.BigDecimal;
import java.util.List;

public class ProductResponse {

	private final Long productId;
	private final Long memberId;
	private final String sellerNickname;
	private final Long categoryId;
	private final String title;
	private final String description;
	private final BigDecimal price;
	private final TradeStatus tradeStatus;
	private final String region;
	private final String regionCode;
	private final String regionName;
	private final String regionFullName;
	private final long viewCount;
	private final int favoriteCount;
	private final String thumbnailUrl;
	private final List<String> imageUrls;
	private final boolean hidden;

	private ProductResponse(Long productId, Long memberId, String sellerNickname, Long categoryId, String title,
							String description, BigDecimal price, TradeStatus tradeStatus, String region,
							String regionCode, String regionName, String regionFullName,
							long viewCount, int favoriteCount, String thumbnailUrl, List<String> imageUrls,
							boolean hidden) {
		this.productId = productId;
		this.memberId = memberId;
		this.sellerNickname = sellerNickname;
		this.categoryId = categoryId;
		this.title = title;
		this.description = description;
		this.price = price;
		this.tradeStatus = tradeStatus;
		this.region = region;
		this.regionCode = regionCode;
		this.regionName = regionName;
		this.regionFullName = regionFullName;
		this.viewCount = viewCount;
		this.favoriteCount = favoriteCount;
		this.thumbnailUrl = thumbnailUrl;
		this.imageUrls = imageUrls;
		this.hidden = hidden;
	}

	public static ProductResponse from(Product product) {
		return from(product, List.of());
	}

	public static ProductResponse from(Product product, List<String> imageUrls) {
		return new ProductResponse(
				product.getId(),
				product.getMember().getId(),
				product.getMember().getDisplayNickname(),
				product.getCategory().getId(),
				product.getTitle(),
				product.getDescription(),
				product.getPrice(),
				product.getTradeStatus(),
				product.getRegion(),
				product.getRegionCode(),
				product.getRegionName(),
				product.getRegionFullName(),
				product.getViewCount(),
				product.getFavoriteCount(),
				product.getThumbnailUrl(),
				imageUrls,
				product.isHidden()
		);
	}

	public Long getProductId() {
		return productId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getSellerNickname() {
		return sellerNickname;
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

	public String getRegionCode() {
		return regionCode;
	}

	public String getRegionName() {
		return regionName;
	}

	public String getRegionFullName() {
		return regionFullName;
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

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public boolean isHidden() {
		return hidden;
	}
}
