package com.dongnemarket.favorite.repository;

import com.dongnemarket.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMember_IdAndProduct_Id(Long memberId, Long productId);

    Optional<Favorite> findByMember_IdAndProduct_Id(Long memberId, Long productId);

    /**
     * 내 관심 목록을 상품과 함께(fetch join) 최근 등록순으로 조회한다.
     * 삭제(deleted_at)·숨김(hidden) 상품은 목록에서 제외한다(정책 A).
     */
    @Query("select f from Favorite f join fetch f.product p " +
            "where f.member.id = :memberId and p.deletedAt is null and p.hidden = false " +
            "order by f.createdAt desc, f.id desc")
    List<Favorite> findAllWithAccessibleProductByMember_Id(@Param("memberId") Long memberId);
}
