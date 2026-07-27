package com.dongnemarket.global.init.master;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CategorySeederTest {

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

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	CategorySeeder categorySeeder;

	@Test
	@DisplayName("애플리케이션 시작 시 기본 카테고리 8개가 저장된다")
	void savesEightDefaultCategories() {
		categorySeeder.seed();

		List<String> categoryNames = categoryRepository.findAll()
				.stream()
				.map(Category::getName)
				.toList();

		assertThat(categoryNames).containsExactlyInAnyOrderElementsOf(DEFAULT_CATEGORY_NAMES);
	}

	@Test
	@DisplayName("시더를 다시 실행해도 기본 카테고리가 중복 저장되지 않는다")
	void doesNotDuplicateDefaultCategories() {
		categorySeeder.seed();

		assertThat(categoryRepository.count()).isEqualTo(DEFAULT_CATEGORY_NAMES.size());
	}
}
