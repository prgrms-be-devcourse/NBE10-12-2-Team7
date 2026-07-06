package com.dongnemarket.product.service;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
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
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private static final int MAX_REGION_FILTER_SIZE = 2;
	private static final int DEFAULT_PAGE_SIZE = 30;
	private static final int MAX_PAGE_SIZE = 100;

	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final MemberRepository memberRepository;
	private final CategoryRepository categoryRepository;
	private final RegionRepository regionRepository;

	public ProductService(ProductRepository productRepository,
						  ProductImageRepository productImageRepository,
						  MemberRepository memberRepository,
						  CategoryRepository categoryRepository,
						  RegionRepository regionRepository) {
		this.productRepository = productRepository;
		this.productImageRepository = productImageRepository;
		this.memberRepository = memberRepository;
		this.categoryRepository = categoryRepository;
		this.regionRepository = regionRepository;
	}

	@Transactional
	public ProductResponse createProduct(Long memberId, ProductCreateRequest request) {
		validateRequest(request);
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		Category category = categoryRepository.findById(request.getCategoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
		validateRegionExists(request.getRegion());

		Product product = Product.create(
				member,
				category,
				request.getTitle(),
				request.getDescription(),
				request.getPrice(),
				request.getRegion()
		);
		Product savedProduct = productRepository.save(product);
		saveProductImages(savedProduct, request.getImageUrls(), request.getThumbnailIndex());
		return ProductResponse.from(savedProduct, request.getImageUrls());
	}

	public ProductPageResponse getProducts() {
		return getProducts(null, null, DEFAULT_PAGE_SIZE);
	}

	public ProductPageResponse getProducts(List<String> regions) {
		return getProducts(regions, null, DEFAULT_PAGE_SIZE);
	}

	public ProductPageResponse getProducts(List<String> regions, Long cursor, int size) {
		List<String> normalizedRegions = normalizeRegions(regions);
		validateRegionFilterSize(normalizedRegions);
		int limit = clampPageSize(size);

		List<Product> rows = productRepository.findBy(
				ProductSpecification.list(normalizedRegions, cursor),
				query -> query
						.sortBy(Sort.by(Sort.Direction.DESC, "id"))
						.limit(limit + 1)
						.all()
		);
		boolean hasNext = rows.size() > limit;
		List<Product> page = hasNext ? rows.subList(0, limit) : rows;
		Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
		List<ProductSummaryResponse> items = page
					.stream()
					.map(ProductSummaryResponse::from)
					.toList();
		return ProductPageResponse.of(items, nextCursor, hasNext);
	}

	public List<ProductSummaryResponse> getProductsByCategory(Long categoryId) {
		if (!categoryRepository.existsById(categoryId)) {
			throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
		}

		return productRepository.findAll(
						ProductSpecification.categoryList(categoryId),
						Sort.by(Sort.Direction.DESC, "id")
				)
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
		List<String> regions = normalizeRegions(searchRequest.getRegions());
		validateRegionFilterSize(regions);
		validateSearchPrice(searchRequest.getMinPrice(), searchRequest.getMaxPrice());
		TradeStatus tradeStatus = parseSearchTradeStatus(searchRequest.getTradeStatus());

		return productRepository.findAll(
						ProductSpecification.search(
								searchRequest.getKeyword(),
								searchRequest.getCategoryId(),
								searchRequest.getMinPrice(),
								searchRequest.getMaxPrice(),
								tradeStatus,
								regions
						),
						Sort.by(Sort.Direction.DESC, "id")
				)
				.stream()
				.map(ProductSummaryResponse::from)
				.toList();
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
		validateRegionExists(request.getRegion());
		product.update(
				category,
				request.getTitle(),
				request.getDescription(),
				request.getPrice(),
				request.getRegion()
		);
		productImageRepository.deleteAllByProductId(productId);
		saveProductImages(product, request.getImageUrls(), request.getThumbnailIndex());
		return ProductResponse.from(product, request.getImageUrls());
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

	private void validateRegionExists(String region) {
		if (!StringUtils.hasText(region) || !regionRepository.existsByName(region)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
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
			return new ProductSearchRequest(null, null, (BigDecimal) null, null, null, null);
		}
		return request;
	}

	private List<String> normalizeRegions(List<String> regions) {
		if (regions == null) {
			return List.of();
		}
		return regions.stream()
				.filter(StringUtils::hasText)
				.toList();
	}

	private void validateRegionFilterSize(List<String> regions) {
		if (regions.size() > MAX_REGION_FILTER_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
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
