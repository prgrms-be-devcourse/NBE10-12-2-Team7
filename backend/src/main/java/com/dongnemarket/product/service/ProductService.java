package com.dongnemarket.product.service;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.dto.ProductCreateRequest;
import com.dongnemarket.product.dto.ProductResponse;
import com.dongnemarket.product.dto.ProductSummaryResponse;
import com.dongnemarket.product.entity.Product;
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

	private void validateRequest(ProductCreateRequest request) {
		if (!StringUtils.hasText(request.getTitle())) {
			throw new BusinessException(ErrorCode.INVALID_PRODUCT_TITLE);
		}
		if (request.getPrice() == null || request.getPrice() < 0) {
			throw new BusinessException(ErrorCode.INVALID_PRODUCT_PRICE);
		}
	}
}
