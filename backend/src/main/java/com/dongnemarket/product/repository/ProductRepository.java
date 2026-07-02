package com.dongnemarket.product.repository;

import com.dongnemarket.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	// 삭제되지 않고 숨김 처리되지 않은 상품을 최신 등록순으로 조회한다.
	List<Product> findAllByDeletedAtIsNullAndHiddenFalseOrderByIdDesc();

	// 삭제되지 않고 숨김 처리되지 않은 상품 존재 여부를 확인한다.
	boolean existsByIdAndDeletedAtIsNullAndHiddenFalse(Long id);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Product p set p.favoriteCount = p.favoriteCount + 1 where p.id = :id")
	void incrementFavoriteCount(@Param("id") Long id);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Product p set p.favoriteCount = p.favoriteCount - 1 where p.id = :id and p.favoriteCount > 0")
	void decrementFavoriteCount(@Param("id") Long id);

	// 특정 카테고리의 삭제되지 않고 숨김 처리되지 않은 상품을 최신 등록순으로 조회한다.
	List<Product> findAllByCategoryIdAndDeletedAtIsNullAndHiddenFalseOrderByIdDesc(Long categoryId);

	// 특정 회원의 삭제되지 않은 상품을 최신 등록순으로 조회한다.
	List<Product> findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(Long memberId);
}
