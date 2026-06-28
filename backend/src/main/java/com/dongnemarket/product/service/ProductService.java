package com.dongnemarket.product.service;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.dto.ProductCreateRequest;
import com.dongnemarket.product.dto.ProductResponse;
import com.dongnemarket.product.dto.ProductStatusUpdateRequest;
import com.dongnemarket.product.dto.ProductSummaryResponse;
import com.dongnemarket.product.dto.ProductUpdateRequest;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;
	private final CategoryRepository categoryRepository;

	public ProductService(ProductRepository productRepository,
						  MemberRepository memberRepository,
						  CategoryRepository categoryRepository) {
		this.productRepository = productRepository;
		this.memberRepository = memberRepository;
		this.categoryRepository = categoryRepository;
	}

	@Transactional
	public ProductResponse createProduct(Long memberId, ProductCreateRequest request) {
		validateRequest(request);
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		Category category = categoryRepository.findById(request.getCategoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

		Product product = Product.create(
				member,
				category,
				request.getTitle(),
				request.getDescription(),
				request.getPrice(),
				request.getRegion()
		);
		Product savedProduct = productRepository.save(product);
		return ProductResponse.from(savedProduct);
	}

	public List<ProductSummaryResponse> getProducts() {
		return productRepository.findAllByDeletedAtIsNullAndHiddenFalseOrderByIdDesc()
				.stream()
				.map(ProductSummaryResponse::from)
				.toList();
	}

	public List<ProductSummaryResponse> getProductsByCategory(Long categoryId) {
		if (!categoryRepository.existsById(categoryId)) {
			throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
		}

		return productRepository.findAllByCategoryIdAndDeletedAtIsNullAndHiddenFalseOrderByIdDesc(categoryId)
				.stream()
				.map(ProductSummaryResponse::from)
				.toList();
	}

	public List<ProductSummaryResponse> getMyProducts(Long memberId) {
		return productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(memberId)
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

		product.increaseViewCount();
		return ProductResponse.from(product);
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
		product.update(
				category,
				request.getTitle(),
				request.getDescription(),
				request.getPrice(),
				request.getRegion()
		);
		return ProductResponse.from(product);
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
		return ProductResponse.from(product);
	}

	public void validateAccessibleProduct(Long productId) {
		if (!productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(productId)) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
	}

	private void validateRequest(ProductCreateRequest request) {
		validateProductFields(request.getTitle(), request.getPrice());
	}

	private void validateRequest(ProductUpdateRequest request) {
		validateProductFields(request.getTitle(), request.getPrice());
	}

	private void validateProductFields(String title, Integer price) {
		if (!StringUtils.hasText(title)) {
			throw new BusinessException(ErrorCode.INVALID_PRODUCT_TITLE);
		}
		if (price == null || price < 0) {
			throw new BusinessException(ErrorCode.INVALID_PRODUCT_PRICE);
		}
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
}
