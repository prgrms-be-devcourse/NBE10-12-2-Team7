package com.dongnemarket.product.dto;

import java.util.List;

public class ProductPageResponse {

	private final List<ProductSummaryResponse> items;
	private final Long nextCursor;
	private final boolean hasNext;

	private ProductPageResponse(List<ProductSummaryResponse> items, Long nextCursor, boolean hasNext) {
		this.items = items;
		this.nextCursor = nextCursor;
		this.hasNext = hasNext;
	}

	public static ProductPageResponse of(List<ProductSummaryResponse> items, Long nextCursor, boolean hasNext) {
		return new ProductPageResponse(items, nextCursor, hasNext);
	}

	public List<ProductSummaryResponse> getItems() {
		return items;
	}

	public Long getNextCursor() {
		return nextCursor;
	}

	public boolean isHasNext() {
		return hasNext;
	}
}
