package com.dongnemarket.favorite.service;

import com.dongnemarket.favorite.dto.FavoriteResponse;
import com.dongnemarket.favorite.dto.MyFavoriteResponse;
import com.dongnemarket.favorite.entity.Favorite;
import com.dongnemarket.favorite.repository.FavoriteRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.service.ProductService;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductService productService;
    private final EntityManager entityManager;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductService productService,
                           EntityManager entityManager) {
        this.favoriteRepository = favoriteRepository;
        this.productService = productService;
        this.entityManager = entityManager;
    }

    /** 관심 상품 등록. 로그인 사용자가 특정 상품을 관심 목록에 추가한다. */
    @Transactional
    public FavoriteResponse add(Long memberId, Long productId) {
        validateFavoriteCreatable(memberId, productId);

        Member member = entityManager.find(Member.class, memberId);
        Product product = entityManager.find(Product.class, productId);
        try {
            Favorite saved = favoriteRepository.save(Favorite.of(member, product));
            return FavoriteResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // 중복 체크 통과 후 save() 사이의 race condition으로 UNIQUE 제약을 위반한 경우
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }
    }

    /**
     * 내 관심 상품 목록 조회. 로그인 사용자가 등록한 관심 상품을 상품 요약과 함께 최근 등록순으로 조회한다.
     * 삭제·숨김된 상품의 관심은 목록에서 제외한다(정책 A).
     */
    public List<MyFavoriteResponse> getMyFavorites(Long memberId) {
        return favoriteRepository.findAllWithAccessibleProductByMember_Id(memberId).stream()
                .map(MyFavoriteResponse::from)
                .toList();
    }

    /** 관심 상품 취소. 로그인 사용자가 자신이 등록한 관심 상품을 제거한다. */
    @Transactional
    public void remove(Long memberId, Long productId) {
        Favorite favorite = favoriteRepository.findByMember_IdAndProduct_Id(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    /**
     * 관심 등록 가능 여부 검증: 접근 가능한 상품(삭제·숨김 아님)이어야 하고,
     * 동일 사용자가 이미 등록하지 않았어야 한다.
     */
    private void validateFavoriteCreatable(Long memberId, Long productId) {
        productService.validateAccessibleProduct(productId);
        if (favoriteRepository.existsByMember_IdAndProduct_Id(memberId, productId)) {
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }
    }
}
