package com.dongnemarket.product.repository.spec;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class ProductSpecification {

	private ProductSpecification() {
	}

	public static Specification<Product> search(String keyword, Long categoryId, Integer minPrice,
												Integer maxPrice, TradeStatus tradeStatus) {
		return notDeleted()
				.and(notHidden())
				.and(keywordContains(keyword))
				.and(categoryEquals(categoryId))
				.and(priceGreaterThanOrEqualTo(minPrice))
				.and(priceLessThanOrEqualTo(maxPrice))
				.and(tradeStatusEquals(tradeStatus));
	}

	private static Specification<Product> notDeleted() {
		return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
	}

	private static Specification<Product> notHidden() {
		return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("hidden"));
	}

	private static Specification<Product> keywordContains(String keyword) {
		return (root, query, criteriaBuilder) -> {
			if (!StringUtils.hasText(keyword)) {
				return null;
			}
			String keywordPattern = "%" + keyword.toLowerCase() + "%";
			return criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), keywordPattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), keywordPattern)
			);
		};
	}

	private static Specification<Product> categoryEquals(Long categoryId) {
		return (root, query, criteriaBuilder) -> {
			if (categoryId == null) {
				return null;
			}
			return criteriaBuilder.equal(root.get("category").get("id"), categoryId);
		};
	}

	private static Specification<Product> priceGreaterThanOrEqualTo(Integer minPrice) {
		return (root, query, criteriaBuilder) -> {
			if (minPrice == null) {
				return null;
			}
			return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
		};
	}

	private static Specification<Product> priceLessThanOrEqualTo(Integer maxPrice) {
		return (root, query, criteriaBuilder) -> {
			if (maxPrice == null) {
				return null;
			}
			return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
		};
	}

	private static Specification<Product> tradeStatusEquals(TradeStatus tradeStatus) {
		return (root, query, criteriaBuilder) -> {
			if (tradeStatus == null) {
				return null;
			}
			return criteriaBuilder.equal(root.get("tradeStatus"), tradeStatus);
		};
	}
}
