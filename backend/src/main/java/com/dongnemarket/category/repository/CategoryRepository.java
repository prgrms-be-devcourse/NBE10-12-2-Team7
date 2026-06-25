package com.dongnemarket.category.repository;

import com.dongnemarket.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByName(String name);

	List<Category> findAllByOrderByIdAsc();
}
