package com.dongnemarket.category.service;

import com.dongnemarket.category.dto.CategoryResponse;
import com.dongnemarket.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	public List<CategoryResponse> getCategories() {
		return categoryRepository.findAllByOrderByIdAsc()
				.stream()
				.map(CategoryResponse::from)
				.toList();
	}
}
