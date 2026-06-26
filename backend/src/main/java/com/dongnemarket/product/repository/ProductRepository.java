package com.dongnemarket.product.repository;

import com.dongnemarket.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

	// 삭제되지 않고 숨김 처리되지 않은 상품을 최신 등록순으로 조회한다.
	List<Product> findAllByDeletedAtIsNullAndHiddenFalseOrderByIdDesc();


}
