package com.dongnemarket.favorite.dto;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;

import java.math.BigDecimal;

/** 관심 목록에 동봉되는 상품 요약(대표사진·이름·가격·판매장소·판매상태). 필요한 필드만 담는 슬림 DTO. */
public class FavoriteProductSummary {

    private final Long productId;
	    private final Long categoryId;
		    private final String title;
		    private final BigDecimal price;
		    private final String regionCode;
	    private final String regionName;
	    private final String regionFullName;
	    private final TradeStatus tradeStatus;
	    private final String thumbnailUrl;
		
		    private FavoriteProductSummary(Long productId, Long categoryId, String title, BigDecimal price,
		                                   String regionCode, String regionName, String regionFullName,
		                                   TradeStatus tradeStatus, String thumbnailUrl) {
	        this.productId = productId;
		        this.categoryId = categoryId;
		        this.title = title;
		        this.price = price;
		        this.regionCode = regionCode;
	        this.regionName = regionName;
	        this.regionFullName = regionFullName;
	        this.tradeStatus = tradeStatus;
	        this.thumbnailUrl = thumbnailUrl;
	    }

    public static FavoriteProductSummary from(Product product) {
        return new FavoriteProductSummary(
                product.getId(),
		                product.getCategory().getId(),
		                product.getTitle(),
		                product.getPrice(),
		                product.getRegionCode(),
	                product.getRegionName(),
	                product.getRegionFullName(),
	                product.getTradeStatus(),
	                product.getThumbnailUrl()
	        );
    }

    public Long getProductId() { return productId; }
	    public Long getCategoryId() { return categoryId; }
		    public String getTitle() { return title; }
		    public BigDecimal getPrice() { return price; }
		    public String getRegionCode() { return regionCode; }
	    public String getRegionName() { return regionName; }
	    public String getRegionFullName() { return regionFullName; }
	    public TradeStatus getTradeStatus() { return tradeStatus; }
	    public String getThumbnailUrl() { return thumbnailUrl; }
	}
