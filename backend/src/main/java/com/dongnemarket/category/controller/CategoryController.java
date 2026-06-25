package com.dongnemarket.category.controller;

import com.dongnemarket.category.dto.CategoryResponse;
import com.dongnemarket.category.service.CategoryService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Category", description = "카테고리 API")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@Operation(summary = "카테고리 목록 조회", description = "상품 등록 및 검색에 사용할 카테고리 목록을 조회합니다.")
	@GetMapping
	public ApiResponse<List<CategoryResponse>> getCategories() {
		return ApiResponse.success(categoryService.getCategories());
	}
}
