package com.dongnemarket.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dongnemarket.product.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

	List<ProductImage> findAllByProductIdOrderBySortOrderAsc(Long productId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from ProductImage pi where pi.product.id = :productId")
	void deleteAllByProductId(@Param("productId") Long productId);
}
