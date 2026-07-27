package com.dongnemarket.chat.dto;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;

import java.math.BigDecimal;

/** 채팅방 입장 화면에 표시할 상품 상세(대표사진·제목·상세설명·가격·거래상태·판매지역). */
public class ChatProductDetail {

    private final Long productId;
    private final String title;
    private final String description;
	    private final BigDecimal price;
	    private final TradeStatus tradeStatus;
	    private final String region;
	    private final String regionCode;
	    private final String regionName;
	    private final String regionFullName;
	    private final String thumbnailUrl;
	
	    private ChatProductDetail(Long productId, String title, String description, BigDecimal price,
	                              TradeStatus tradeStatus, String region, String regionCode, String regionName,
	                              String regionFullName, String thumbnailUrl) {
	        this.productId = productId;
	        this.title = title;
	        this.description = description;
	        this.price = price;
	        this.tradeStatus = tradeStatus;
	        this.region = region;
	        this.regionCode = regionCode;
	        this.regionName = regionName;
	        this.regionFullName = regionFullName;
	        this.thumbnailUrl = thumbnailUrl;
	    }

    public static ChatProductDetail from(Product product) {
        return new ChatProductDetail(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
	                product.getPrice(),
	                product.getTradeStatus(),
	                product.getRegion(),
	                product.getRegionCode(),
	                product.getRegionName(),
	                product.getRegionFullName(),
	                product.getThumbnailUrl()
	        );
    }

    public Long getProductId() { return productId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
	    public BigDecimal getPrice() { return price; }
	    public TradeStatus getTradeStatus() { return tradeStatus; }
	    public String getRegion() { return region; }
	    public String getRegionCode() { return regionCode; }
	    public String getRegionName() { return regionName; }
	    public String getRegionFullName() { return regionFullName; }
	    public String getThumbnailUrl() { return thumbnailUrl; }
	}
