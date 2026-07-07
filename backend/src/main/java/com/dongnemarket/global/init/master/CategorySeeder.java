package com.dongnemarket.global.init.master;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.init.DataSeeder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 기준데이터: 기본 카테고리 8종. 모든 환경에서 항상 실행(멱등). */
@Component
public class CategorySeeder implements DataSeeder {

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

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    @Transactional
    public void seed() {
        DEFAULT_CATEGORY_NAMES.stream()
                .filter(name -> !categoryRepository.existsByName(name))
                .map(Category::new)
                .forEach(categoryRepository::save);
    }
}
