package com.dongnemarket.product.repository.spec;

import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.member.entity.MemberStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public class ProductSpecification {

	private ProductSpecification() {
	}

	public static Specification<Product> list(String regionCodePrefix, Long cursor) {
		return visibleProducts()
				.and(regionCodeStartsWith(regionCodePrefix))
				.and(idLessThan(cursor));
	}

	public static Specification<Product> search(String keyword, Long categoryId, BigDecimal minPrice,
												BigDecimal maxPrice, TradeStatus tradeStatus, String regionCodePrefix) {
		return visibleProducts()
				.and(keywordContains(keyword))
				.and(categoryEquals(categoryId))
				.and(priceGreaterThanOrEqualTo(minPrice))
				.and(priceLessThanOrEqualTo(maxPrice))
				.and(tradeStatusEquals(tradeStatus))
				.and(regionCodeStartsWith(regionCodePrefix));
	}

	public static Specification<Product> categoryList(Long categoryId) {
		return visibleProducts()
				.and(categoryEquals(categoryId));
	}

	private static Specification<Product> visibleProducts() {
		return notDeleted()
				.and(notHidden())
				.and(notCompleted())
				.and(activeSeller());
	}

	private static Specification<Product> notDeleted() {
		return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
	}

	private static Specification<Product> notHidden() {
		return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("hidden"));
	}

	private static Specification<Product> notCompleted() {
		return (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("tradeStatus"), TradeStatus.COMPLETED);
	}

	private static Specification<Product> activeSeller() {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("member").get("status"), MemberStatus.ACTIVE);
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

	private static Specification<Product> priceGreaterThanOrEqualTo(BigDecimal minPrice) {
		return (root, query, criteriaBuilder) -> {
			if (minPrice == null) {
				return null;
			}
			return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
		};
	}

	private static Specification<Product> priceLessThanOrEqualTo(BigDecimal maxPrice) {
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

	// 선택한 지역의 code prefix로 하위 전체를 매칭한다(시도=앞2, 시군구=앞5, 동=전체10).
	// 상품은 동(level3)에만 붙으므로 prefix로 하위 동 전체가 걸린다. 하강 깊이 무관(세종 포함).
	private static Specification<Product> regionCodeStartsWith(String regionCodePrefix) {
		return (root, query, criteriaBuilder) -> {
			if (!StringUtils.hasText(regionCodePrefix)) {
				return null;
			}
			return criteriaBuilder.like(root.get("region").get("code"), regionCodePrefix + "%");
		};
	}

	private static Specification<Product> idLessThan(Long cursor) {
		return (root, query, criteriaBuilder) -> {
			if (cursor == null) {
				return null;
			}
			return criteriaBuilder.lessThan(root.get("id"), cursor);
		};
	}

}
