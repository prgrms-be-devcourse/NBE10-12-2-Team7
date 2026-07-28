package com.dongnemarket.trade.dto;

import com.dongnemarket.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 판매내역 한 건. 거래완료된 내 상품 기준이라 구매자 정보는 담지 않는다(스키마상 별도 구매자 필드가 없음). */
public class TradeSaleResponse {

    private final Long productId;
    private final String title;
	    private final String thumbnailUrl;
	    private final BigDecimal price;
	    private final String region;
	    private final String regionCode;
	    private final String regionName;
	    private final String regionFullName;
	    private final LocalDateTime completedAt;
	
	    private TradeSaleResponse(Long productId, String title, String thumbnailUrl, BigDecimal price,
	                              String region, String regionCode, String regionName, String regionFullName,
	                              LocalDateTime completedAt) {
	        this.productId = productId;
	        this.title = title;
	        this.thumbnailUrl = thumbnailUrl;
	        this.price = price;
	        this.region = region;
	        this.regionCode = regionCode;
	        this.regionName = regionName;
	        this.regionFullName = regionFullName;
	        this.completedAt = completedAt;
	    }

    public static TradeSaleResponse from(Product product) {
        return new TradeSaleResponse(
                product.getId(),
                product.getTitle(),
	                product.getThumbnailUrl(),
	                product.getPrice(),
	                product.getRegion(),
	                product.getRegionCode(),
	                product.getRegionName(),
	                product.getRegionFullName(),
	                product.getCompletedAt()
	        );
    }

    public Long getProductId() { return productId; }
    public String getTitle() { return title; }
	    public String getThumbnailUrl() { return thumbnailUrl; }
	    public BigDecimal getPrice() { return price; }
	    public String getRegion() { return region; }
	    public String getRegionCode() { return regionCode; }
	    public String getRegionName() { return regionName; }
	    public String getRegionFullName() { return regionFullName; }
	    public LocalDateTime getCompletedAt() { return completedAt; }
	}
