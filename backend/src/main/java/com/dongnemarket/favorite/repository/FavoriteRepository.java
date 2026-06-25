package com.dongnemarket.favorite.repository;

import com.dongnemarket.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    Optional<Favorite> findByMemberIdAndProductId(Long memberId, Long productId);

    List<Favorite> findAllByMemberId(Long memberId);
}
