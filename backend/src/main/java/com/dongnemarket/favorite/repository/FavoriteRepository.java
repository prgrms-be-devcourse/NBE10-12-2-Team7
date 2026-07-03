package com.dongnemarket.favorite.repository;

import com.dongnemarket.favorite.entity.Favorite;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMember_IdAndProduct_Id(Long memberId, Long productId);

    Optional<Favorite> findByMember_IdAndProduct_Id(Long memberId, Long productId);

    /**
     * 내 관심 목록을 상품과 함께 최근 등록순으로 조회한다. 삭제(deleted_at)·숨김(hidden) 상품은 제외한다(정책 A).
     * <p>{@code JOIN FETCH}로 상품을 한 번의 쿼리에 함께 로딩해, 목록 매핑 시 발생하던 N+1을 제거한다.
     * 상품은 {@code @ManyToOne} 단건 연관이라 행 증식이 없어, {@code limit}이 SQL 레벨에서 적용된다
     * (컬렉션 fetch join의 메모리 페이징 문제 HHH000104 해당 없음).
     */
    @Query("SELECT f FROM Favorite f " +
            "JOIN FETCH f.product p " +
            "WHERE f.member.id = :memberId " +
            "AND p.deletedAt IS NULL AND p.hidden = false " +
            "ORDER BY f.createdAt DESC, f.id DESC")
    List<Favorite> findMyFavoritesWithProduct(@Param("memberId") Long memberId, Limit limit);
}
