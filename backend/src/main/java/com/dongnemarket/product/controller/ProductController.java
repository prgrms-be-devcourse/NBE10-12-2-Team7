package com.dongnemarket.product.controller;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.product.dto.ProductCreateRequest;
import com.dongnemarket.product.dto.ProductResponse;
import com.dongnemarket.product.dto.ProductSummaryResponse;
import com.dongnemarket.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
			@RequestBody ProductCreateRequest request) {
		ProductResponse response = productService.createProduct(memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(HttpStatus.CREATED.value(), "상품이 등록되었습니다.", response));
	}

	@Operation(summary = "상품 목록 조회", description = "삭제되거나 숨김 처리되지 않은 상품 목록을 최신 등록순으로 조회합니다.")
	@GetMapping
	public ApiResponse<List<ProductSummaryResponse>> getProducts() {
		return ApiResponse.success(productService.getProducts());
	}
}
