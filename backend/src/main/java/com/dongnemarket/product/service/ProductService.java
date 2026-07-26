package com.dongnemarket.product.service;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.common.event.ProductCompletedEvent;
import com.dongnemarket.global.common.event.ProductPriceChangedEvent;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.manner.entity.MannerScore;
import com.dongnemarket.manner.service.MannerScoreService;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.dto.ProductCreateRequest;
import com.dongnemarket.product.dto.ProductPageResponse;
import com.dongnemarket.product.dto.ProductResponse;
import com.dongnemarket.product.dto.ProductSearchRequest;
import com.dongnemarket.product.dto.ProductStatusUpdateRequest;
import com.dongnemarket.product.dto.ProductSummaryResponse;
import com.dongnemarket.product.dto.ProductUpdateRequest;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.ProductImage;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.ProductImageRepository;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.product.repository.spec.ProductSpecification;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private static final int DEFAULT_PAGE_SIZE = 30;
	private static final int MAX_PAGE_SIZE = 100;

	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final MemberRepository memberRepository;
	private final CategoryRepository categoryRepository;
	private final RegionRepository regionRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final MannerScoreService mannerScoreService;

	public ProductService(ProductRepository productRepository,
						  ProductImageRepository productImageRepository,
						  MemberRepository memberRepository,
						  CategoryRepository categoryRepository,
						  RegionRepository regionRepository,
						  ApplicationEventPublisher eventPublisher,
						  MannerScoreService mannerScoreService) {
		this.productRepository = productRepository;
		this.productImageRepository = productImageRepository;
		this.memberRepository = memberRepository;
		this.categoryRepository = categoryRepository;
		this.regionRepository = regionRepository;
		this.eventPublisher = eventPublisher;
		this.mannerScoreService = mannerScoreService;
	}

	@Transactional
	public ProductResponse createProduct(Long memberId, ProductCreateRequest request) {
		validateRequest(request);
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		Category category = categoryRepository.findById(request.getCategoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
		Region region = resolveLevel3Region(request.getRegionId());

		Product product = Product.create(
				member,
				category,
				request.getTitle(),
				request.getDescription(),
				request.getPrice(),
				region
		);
		Product savedProduct = productRepository.save(product);
		saveProductImages(savedProduct, request.getImageUrls(), request.getThumbnailIndex());
		return ProductResponse.from(savedProduct, request.getImageUrls());
	}

	public ProductPageResponse getProducts() {
		return getProducts(null, null, DEFAULT_PAGE_SIZE);
	}

	public ProductPageResponse getProducts(Long regionId) {
		return getProducts(regionId, null, DEFAULT_PAGE_SIZE);
	}

	public ProductPageResponse getProducts(Long regionId, Long cursor, int size) {
		int limit = clampPageSize(size);
		String regionCodePrefix = regionCodePrefixOrEmpty(regionId);
		// 지역이 지정됐는데 존재하지 않으면 매칭 대상이 없다 → 빈 페이지.
		if (regionId != null && regionCodePrefix == null) {
			return ProductPageResponse.of(List.of(), null, false);
		}

		List<Product> rows = productRepository.findBy(
				ProductSpecification.list(regionCodePrefix, cursor),
				query -> query
						.sortBy(Sort.by(Sort.Direction.DESC, "id"))
						.limit(limit + 1)
						.all()
		);
		boolean hasNext = rows.size() > limit;
		List<Product> page = hasNext ? rows.subList(0, limit) : rows;
		Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
		// 커서(다음 페이지 기준)는 항상 id 내림차순 조회 결과 그대로 계산한다 — 신뢰도 하락 정렬은
		// 여기서 확정된 페이지 내부의 노출 순서만 바꿀 뿐, 페이지네이션 자체에는 영향을 주지 않는다.
		List<ProductSummaryResponse> items = demoteLowTrustSellers(page)
					.stream()
					.map(ProductSummaryResponse::from)
					.toList();
		return ProductPageResponse.of(items, nextCursor, hasNext);
	}

	public List<ProductSummaryResponse> getProductsByCategory(Long categoryId) {
		if (!categoryRepository.existsById(categoryId)) {
			throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
		}

		List<Product> products = productRepository.findAll(
				ProductSpecification.categoryList(categoryId),
				Sort.by(Sort.Direction.DESC, "id")
		);
		return demoteLowTrustSellers(products)
				.stream()
				.map(ProductSummaryResponse::from)
				.toList();
	}

	public List<ProductSummaryResponse> getMyProducts(Long memberId) {
		validateAuthenticatedMember(memberId);
		return productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(memberId)
				.stream()
				.map(ProductSummaryResponse::from)
				.toList();
	}

	public List<ProductSummaryResponse> searchProducts(ProductSearchRequest request) {
		ProductSearchRequest searchRequest = normalizeSearchRequest(request);
		validateSearchPrice(searchRequest.getMinPrice(), searchRequest.getMaxPrice());
		TradeStatus tradeStatus = parseSearchTradeStatus(searchRequest.getTradeStatus());

		String regionCodePrefix = regionCodePrefixOrEmpty(searchRequest.getRegionId());
		// 지역이 지정됐는데 존재하지 않으면 매칭 대상이 없다 → 빈 결과.
		if (searchRequest.getRegionId() != null && regionCodePrefix == null) {
			return List.of();
		}

		List<Product> products = productRepository.findAll(
				ProductSpecification.search(
						searchRequest.getKeyword(),
						searchRequest.getCategoryId(),
						searchRequest.getMinPrice(),
						searchRequest.getMaxPrice(),
						tradeStatus,
						regionCodePrefix
				),
				Sort.by(Sort.Direction.DESC, "id")
		);
		return demoteLowTrustSellers(products)
				.stream()
				.map(ProductSummaryResponse::from)
				.toList();
	}

	/**
	 * 판매자 매너온도가 저신뢰 임계치({@link MannerScore#LOW_TRUST_THRESHOLD}) 이하인 상품을
	 * 목록 뒤쪽으로 밀어낸다(신고 목록 신뢰도 가중 정렬과 대칭되는 "노출 우선순위 하락").
	 * 안정 정렬(stable sort)이라 각 그룹 내부의 원래 id 내림차순은 그대로 유지된다.
	 */
	private List<Product> demoteLowTrustSellers(List<Product> products) {
		if (products.isEmpty()) {
			return products;
		}
		Set<Long> sellerIds = products.stream()
				.map(product -> product.getMember().getId())
				.collect(Collectors.toSet());
		Map<Long, BigDecimal> trustScores = mannerScoreService.getScoresByMemberIds(sellerIds);
		return products.stream()
				.sorted(Comparator.comparing(product -> isLowTrustSeller(product, trustScores)))
				.toList();
	}

	private boolean isLowTrustSeller(Product product, Map<Long, BigDecimal> trustScores) {
		BigDecimal score = trustScores.getOrDefault(product.getMember().getId(), MannerScore.DEFAULT_SCORE);
		return score.compareTo(MannerScore.LOW_TRUST_THRESHOLD) <= 0;
	}

	@Transactional
	public ProductResponse getProduct(Long productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		if (product.isDeleted()) {
			throw new BusinessException(ErrorCode.DELETED_PRODUCT);
		}
		if (product.isHidden()) {
			throw new BusinessException(ErrorCode.HIDDEN_PRODUCT);
		}
		if (product.getMember().getStatus() != MemberStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		if (product.isCompleted()) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}

		product.increaseViewCount();
		List<String> imageUrls = getImageUrls(productId);
		return ProductResponse.from(product, imageUrls);
	}

	@Transactional
	public ProductResponse updateProduct(Long memberId, Long productId, ProductUpdateRequest request) {
		validateRequest(request);
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		if (product.isDeleted()) {
			throw new BusinessException(ErrorCode.DELETED_PRODUCT);
		}
		if (!product.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.PRODUCT_OWNER_ONLY);
		}
		if (product.isCompleted()) {
			throw new BusinessException(ErrorCode.CANNOT_UPDATE_COMPLETED_PRODUCT);
		}

		Category category = categoryRepository.findById(request.getCategoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
		Region region = resolveLevel3Region(request.getRegionId());
		BigDecimal oldPrice = product.getPrice(); // update() 로 덮이기 전에 캡처
		product.update(
				category,
				request.getTitle(),
				request.getDescription(),
				request.getPrice(),
				region
		);
		productImageRepository.deleteAllByProductId(productId);
		Product managedProduct = productRepository.findById(productId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		saveProductImages(managedProduct, request.getImageUrls(), request.getThumbnailIndex());

		// 가격이 실제로 바뀐 경우에만 알림 이벤트 발행. BigDecimal은 scale 민감이라 compareTo 로 비교한다(equals X).
		// 커밋 후(AFTER_COMMIT) 별도 트랜잭션에서 처리되어 알림 실패가 상품 수정을 롤백하지 않는다(best-effort).
		if (oldPrice.compareTo(request.getPrice()) != 0) {
			eventPublisher.publishEvent(new ProductPriceChangedEvent(
					productId, managedProduct.getTitle(), oldPrice, request.getPrice()));
		}
		return ProductResponse.from(managedProduct, request.getImageUrls());
	}

	@Transactional
	public void deleteProduct(Long memberId, Long productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		if (product.isDeleted()) {
			throw new BusinessException(ErrorCode.DELETED_PRODUCT);
		}
		if (!product.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.PRODUCT_OWNER_ONLY);
		}

		product.softDelete();
	}

	@Transactional
	public ProductResponse updateProductStatus(Long memberId, Long productId, ProductStatusUpdateRequest request) {
		TradeStatus requestedStatus = parseTradeStatus(request);
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		if (product.isDeleted()) {
			throw new BusinessException(ErrorCode.DELETED_PRODUCT);
		}
		if (!product.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.PRODUCT_OWNER_ONLY);
		}
		if (product.isCompleted() && requestedStatus != TradeStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.CANNOT_CHANGE_COMPLETED_PRODUCT);
		}

		if (product.getTradeStatus() != requestedStatus) {
			product.changeTradeStatus(requestedStatus);
			// 매너온도 반영은 커밋 후 별도 트랜잭션에서 처리(best-effort), 상품 상태 변경 자체를 막지 않는다.
			if (requestedStatus == TradeStatus.COMPLETED) {
				eventPublisher.publishEvent(new ProductCompletedEvent(productId, product.getMember().getId()));
			}
		}
		List<String> imageUrls = getImageUrls(productId);
		return ProductResponse.from(product, imageUrls);
	}

	public void validateAccessibleProduct(Long productId) {
		if (!productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(productId)) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
	}

	private void validateAuthenticatedMember(Long memberId) {
		if (memberId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}

	private void validateRequest(ProductCreateRequest request) {
		validateProductFields(request.getTitle(), request.getPrice());
		validateProductImages(request.getImageUrls(), request.getThumbnailIndex());
	}

	private void validateRequest(ProductUpdateRequest request) {
		validateProductFields(request.getTitle(), request.getPrice());
		validateProductImages(request.getImageUrls(), request.getThumbnailIndex());
	}

	private void validateProductFields(String title, BigDecimal price) {
		if (!StringUtils.hasText(title)) {
			throw new BusinessException(ErrorCode.INVALID_PRODUCT_TITLE);
		}
		if (price == null || price.signum() < 0) {
			throw new BusinessException(ErrorCode.INVALID_PRODUCT_PRICE);
		}
	}

	// 상품은 동(level3)에만 붙는다. 존재하지 않거나 동이 아니면 거부.
	private Region resolveLevel3Region(Long regionId) {
		if (regionId == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		Region region = regionRepository.findById(regionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
		if (region.getLevel() != 3) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		return region;
	}

	// 필터용: 선택 지역의 code prefix(시도=앞2, 시군구=앞5, 동=전체10)를 반환한다.
	// regionId가 null이면 필터 없음(null 반환). 지정됐으나 존재하지 않으면 null(호출측이 빈 결과 처리).
	private String regionCodePrefixOrEmpty(Long regionId) {
		if (regionId == null) {
			return null;
		}
		Region region = regionRepository.findById(regionId).orElse(null);
		if (region == null) {
			return null;
		}
		int prefixLength = switch (region.getLevel()) {
			case 1 -> 2;
			case 2 -> 5;
			default -> 10;
		};
		return region.getCode().substring(0, prefixLength);
	}

	private void validateProductImages(List<String> imageUrls, int thumbnailIndex) {
		if (imageUrls == null || imageUrls.isEmpty() || imageUrls.size() > 5) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		if (imageUrls.stream().anyMatch(imageUrl -> !StringUtils.hasText(imageUrl))) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		if (thumbnailIndex < 0 || thumbnailIndex >= imageUrls.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}

	private void saveProductImages(Product product, List<String> imageUrls, int thumbnailIndex) {
		product.changeThumbnailUrl(imageUrls.get(thumbnailIndex));
		List<ProductImage> productImages = java.util.stream.IntStream.range(0, imageUrls.size())
				.mapToObj(index -> ProductImage.create(
						product,
						imageUrls.get(index),
						index,
						index == thumbnailIndex
				))
				.toList();
		productImageRepository.saveAll(productImages);
	}

	private List<String> getImageUrls(Long productId) {
		return productImageRepository.findAllByProductIdOrderBySortOrderAsc(productId)
				.stream()
				.map(ProductImage::getImageUrl)
				.toList();
	}

	private TradeStatus parseTradeStatus(ProductStatusUpdateRequest request) {
		if (request == null || !StringUtils.hasText(request.getTradeStatus())) {
			throw new BusinessException(ErrorCode.INVALID_TRADE_STATUS);
		}
		try {
			return TradeStatus.valueOf(request.getTradeStatus());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_TRADE_STATUS);
		}
	}

	private ProductSearchRequest normalizeSearchRequest(ProductSearchRequest request) {
		if (request == null) {
			return new ProductSearchRequest(null, null, null, null, null, (Long) null);
		}
		return request;
	}

	private int clampPageSize(int size) {
		if (size <= 0) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(size, MAX_PAGE_SIZE);
	}

	private void validateSearchPrice(BigDecimal minPrice, BigDecimal maxPrice) {
		if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
			throw new BusinessException(ErrorCode.INVALID_SEARCH_CONDITION);
		}
		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new BusinessException(ErrorCode.INVALID_SEARCH_CONDITION);
		}
	}

	private TradeStatus parseSearchTradeStatus(String tradeStatus) {
		if (tradeStatus == null) {
			return null;
		}
		if (!StringUtils.hasText(tradeStatus)) {
			throw new BusinessException(ErrorCode.INVALID_TRADE_STATUS);
		}
		try {
			return TradeStatus.valueOf(tradeStatus);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_TRADE_STATUS);
		}
	}
}
