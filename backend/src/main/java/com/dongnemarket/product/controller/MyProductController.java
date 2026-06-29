package com.dongnemarket.product.controller;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.product.dto.ProductSummaryResponse;
import com.dongnemarket.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Product", description = "상품 API")
@RestController
@RequestMapping("/api/products/me")
public class MyProductController {

	private final ProductService productService;

	public MyProductController(ProductService productService) {
		this.productService = productService;
	}

	@Operation(summary = "내 상품 목록 조회", description = "로그인한 사용자가 등록한 삭제되지 않은 상품 목록을 최신 등록순으로 조회합니다.")
	@GetMapping
	public ApiResponse<List<ProductSummaryResponse>> getMyProducts(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.success(productService.getMyProducts(memberId));
	}
}
