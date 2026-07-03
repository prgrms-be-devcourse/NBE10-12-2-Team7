package com.dongnemarket.favorite.service;

import com.dongnemarket.favorite.dto.FavoriteResponse;
import com.dongnemarket.favorite.dto.MyFavoriteResponse;
import com.dongnemarket.favorite.entity.Favorite;
import com.dongnemarket.favorite.repository.FavoriteRepository;
import com.dongnemarket.global.common.event.FavoriteAddedEvent;
import com.dongnemarket.global.common.event.FavoriteRemovedEvent;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.service.ProductService;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FavoriteService {

    /** 관심 목록 조회 상한. 개인 목록은 자연히 바운드되지만, 비정상 폭주 시 payload·메모리를 캡한다(최근순 기준). */
    private static final Limit MY_FAVORITES_LIMIT = Limit.of(200);

    private final FavoriteRepository favoriteRepository;
    private final ProductService productService;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductService productService,
                           EntityManager entityManager,
                           ApplicationEventPublisher eventPublisher) {
        this.favoriteRepository = favoriteRepository;
        this.productService = productService;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    /** 관심 상품 등록. 로그인 사용자가 특정 상품을 관심 목록에 추가한다. */
    @Transactional
    public FavoriteResponse add(Long memberId, Long productId) {
        validateFavoriteCreatable(memberId, productId);

        Member member = entityManager.find(Member.class, memberId);
        Product product = entityManager.find(Product.class, productId);
        Favorite saved;
        try {
            saved = favoriteRepository.save(Favorite.of(member, product));
        } catch (DataIntegrityViolationException e) {
            // 중복 체크 통과 후 save() 사이의 race condition으로 UNIQUE 제약을 위반한 경우
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }
        // 같은 트랜잭션 내 동기 리스너가 Product.favoriteCount를 1 증가시킨다(무결성 보장).
        // 발행은 catch 밖에 두어, 리스너 예외가 race condition으로 오분류되지 않게 한다.
        eventPublisher.publishEvent(new FavoriteAddedEvent(productId));
        return FavoriteResponse.from(saved);
    }

    /**
     * 내 관심 상품 목록 조회. 로그인 사용자가 등록한 관심 상품을 상품 요약과 함께 최근 등록순으로 조회한다.
     * 삭제·숨김된 상품의 관심은 목록에서 제외한다(정책 A).
     * <p>상품을 fetch join으로 함께 로딩해 N+1을 제거하고, {@link #MY_FAVORITES_LIMIT}로 상한을 둔다.
     * 페이지네이션은 클라이언트에서 처리한다(개인 목록이라 바운드됨).
     */
    public List<MyFavoriteResponse> getMyFavorites(Long memberId) {
        return favoriteRepository
                .findMyFavoritesWithProduct(memberId, MY_FAVORITES_LIMIT)
                .stream()
                .map(MyFavoriteResponse::from)
                .toList();
    }

    /** 관심 상품 취소. 로그인 사용자가 자신이 등록한 관심 상품을 제거한다. */
    @Transactional
    public void remove(Long memberId, Long productId) {
        Favorite favorite = favoriteRepository.findByMember_IdAndProduct_Id(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
        // 같은 트랜잭션 내 동기 리스너가 Product.favoriteCount를 1 감소시킨다(무결성 보장).
        eventPublisher.publishEvent(new FavoriteRemovedEvent(productId));
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
