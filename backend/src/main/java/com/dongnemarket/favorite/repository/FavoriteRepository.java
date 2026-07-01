package com.dongnemarket.favorite.repository;

import com.dongnemarket.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMember_IdAndProduct_Id(Long memberId, Long productId);

    Optional<Favorite> findByMember_IdAndProduct_Id(Long memberId, Long productId);

    List<Favorite> findAllByMember_IdOrderByCreatedAtDescIdDesc(Long memberId);
}
