package com.dongnemarket.favorite.service;

import com.dongnemarket.favorite.dto.FavoriteResponse;
import com.dongnemarket.favorite.entity.Favorite;
import com.dongnemarket.favorite.repository.FavoriteRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.product.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    /** 관심 상품 등록. 로그인 사용자가 특정 상품을 관심 목록에 추가한다. */
    @Transactional
    public FavoriteResponse add(Long memberId, Long productId) {
        validateFavoriteCreatable(memberId, productId);

        try {
            Favorite saved = favoriteRepository.save(Favorite.of(memberId, productId));
            return FavoriteResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // 중복 체크 통과 후 save() 사이의 race condition으로 UNIQUE 제약을 위반한 경우
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }
    }

    /** 내 관심 상품 목록 조회. 로그인 사용자가 등록한 관심 상품을 최근 등록순으로 조회한다. */
    public List<FavoriteResponse> getMyFavorites(Long memberId) {
        return favoriteRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    /** 관심 상품 취소. 로그인 사용자가 자신이 등록한 관심 상품을 제거한다. */
    @Transactional
    public void remove(Long memberId, Long productId) {
        Favorite favorite = favoriteRepository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    /** 관심 등록 가능 여부 검증: 상품이 존재해야 하고, 동일 사용자가 이미 등록하지 않았어야 한다. */
    private void validateFavoriteCreatable(Long memberId, Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (favoriteRepository.existsByMemberIdAndProductId(memberId, productId)) {
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }
    }
}
