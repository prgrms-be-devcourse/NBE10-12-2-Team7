package com.dongnemarket.product.controller;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.product.dto.ProductCreateRequest;
import com.dongnemarket.product.dto.ProductResponse;
import com.dongnemarket.product.dto.ProductSearchRequest;
import com.dongnemarket.product.dto.ProductStatusUpdateRequest;
import com.dongnemarket.product.dto.ProductSummaryResponse;
import com.dongnemarket.product.dto.ProductUpdateRequest;
import com.dongnemarket.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Product", description = "상품 API")
@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@Operation(summary = "상품 등록", description = "로그인한 사용자가 상품을 등록합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
			@AuthenticationPrincipal Long memberId,
			@Valid @RequestBody ProductCreateRequest request) {
		ProductResponse response = productService.createProduct(memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(HttpStatus.CREATED.value(), "상품이 등록되었습니다.", response));
	}

	@Operation(summary = "상품 목록 조회", description = "삭제되거나 숨김 처리되지 않은 상품 목록을 최신 등록순으로 조회합니다.")
	@GetMapping
	public ApiResponse<List<ProductSummaryResponse>> getProducts() {
		return ApiResponse.success(productService.getProducts());
	}

	@Operation(summary = "상품 검색", description = "상품을 키워드, 카테고리, 가격 범위, 거래 상태로 검색합니다.")
	@GetMapping("/search")
	public ApiResponse<List<ProductSummaryResponse>> searchProducts(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) String tradeStatus) {
		ProductSearchRequest request = new ProductSearchRequest(keyword, categoryId, minPrice, maxPrice, tradeStatus);
		return ApiResponse.success(productService.searchProducts(request));
	}

	@Operation(summary = "상품 상세 조회", description = "상품 상세 정보를 조회하고 조회수를 1 증가시킵니다.")
	@GetMapping("/{productId}")
	public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
		return ApiResponse.success(productService.getProduct(productId));
	}

	@Operation(summary = "상품 수정", description = "작성자가 상품 기본 정보를 수정합니다. 거래완료 상품은 수정할 수 없습니다.")
	@PatchMapping("/{productId}")
	public ApiResponse<ProductResponse> updateProduct(
			@AuthenticationPrincipal Long memberId,
			@PathVariable Long productId,
			@Valid @RequestBody ProductUpdateRequest request) {
		return ApiResponse.success(productService.updateProduct(memberId, productId, request));
	}

	@Operation(summary = "상품 삭제", description = "작성자가 상품을 논리 삭제합니다.")
	@DeleteMapping("/{productId}")
	public ApiResponse<Void> deleteProduct(
			@AuthenticationPrincipal Long memberId,
			@PathVariable Long productId) {
		productService.deleteProduct(memberId, productId);
		return ApiResponse.success();
	}

	@Operation(summary = "상품 거래 상태 변경", description = "작성자가 상품 거래 상태를 변경합니다. 거래완료 상품은 거래완료 상태만 유지할 수 있습니다.")
	@PatchMapping("/{productId}/status")
	public ApiResponse<ProductResponse> updateProductStatus(
			@AuthenticationPrincipal Long memberId,
			@PathVariable Long productId,
			@RequestBody ProductStatusUpdateRequest request) {
		return ApiResponse.success(productService.updateProductStatus(memberId, productId, request));
	}
}
