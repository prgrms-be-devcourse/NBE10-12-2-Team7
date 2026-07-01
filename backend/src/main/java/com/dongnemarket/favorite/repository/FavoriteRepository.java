package com.dongnemarket.favorite.repository;

import com.dongnemarket.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMember_IdAndProduct_Id(Long memberId, Long productId);

    Optional<Favorite> findByMember_IdAndProduct_Id(Long memberId, Long productId);

    /**
     * 내 관심 목록을 최근 등록순으로 조회한다. 삭제(deleted_at)·숨김(hidden) 상품은 제외한다(정책 A).
     * 상품 연관은 지연 로딩되므로 목록 매핑 시 N+1이 발생한다.
     * 완화하려면 hibernate.default_batch_fetch_size 설정 필요(전역 설정 — 별도 진행).
     */
    List<Favorite> findAllByMember_IdAndProduct_DeletedAtIsNullAndProduct_HiddenFalseOrderByCreatedAtDescIdDesc(Long memberId);
}
