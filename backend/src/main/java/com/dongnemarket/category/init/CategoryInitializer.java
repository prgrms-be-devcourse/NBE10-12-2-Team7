package com.dongnemarket.category.init;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CategoryInitializer implements ApplicationRunner {

	private static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
			"디지털기기",
			"생활가전",
			"가구/인테리어",
			"의류",
			"도서",
			"스포츠/레저",
			"반려동물용품",
			"기타"
	);

	private final CategoryRepository categoryRepository;

	public CategoryInitializer(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		DEFAULT_CATEGORY_NAMES.stream()
				.filter(categoryName -> !categoryRepository.existsByName(categoryName))
				.map(Category::new)
				.forEach(categoryRepository::save);
	}
}
