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
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.service.ProductService;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    FavoriteRepository favoriteRepository;

    @Mock
    ProductService productService;

    @Mock
    EntityManager entityManager;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    FavoriteService favoriteService;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    private Favorite favoriteWithProduct(Long productId, String title) {
        Product product = mock(Product.class);
        given(product.getId()).willReturn(productId);
        given(product.getTitle()).willReturn(title);
        given(product.getPrice()).willReturn(BigDecimal.valueOf(1_000));
        given(product.getRegion()).willReturn("서울 강남구");
        given(product.getTradeStatus()).willReturn(TradeStatus.ON_SALE);
        return Favorite.of(mock(Member.class), product);
    }

    @Test
    @DisplayName("접근 가능한 상품이고 아직 관심 등록하지 않았으면 관심 등록에 성공한다")
    void add_success() {
        Product product = mock(Product.class);
        given(product.getId()).willReturn(PRODUCT_ID);
        given(favoriteRepository.existsByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID)).willReturn(false);
        given(entityManager.find(Member.class, MEMBER_ID)).willReturn(mock(Member.class));
        given(entityManager.find(Product.class, PRODUCT_ID)).willReturn(product);
        given(favoriteRepository.save(any(Favorite.class))).willAnswer(invocation -> invocation.getArgument(0));

        FavoriteResponse response = favoriteService.add(MEMBER_ID, PRODUCT_ID);

        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("관심 등록에 성공하면 해당 상품의 FavoriteAddedEvent를 발행한다")
    void add_success_publishesFavoriteAddedEvent() {
        Product product = mock(Product.class);
        given(product.getId()).willReturn(PRODUCT_ID);
        given(favoriteRepository.existsByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID)).willReturn(false);
        given(entityManager.find(Member.class, MEMBER_ID)).willReturn(mock(Member.class));
        given(entityManager.find(Product.class, PRODUCT_ID)).willReturn(product);
        given(favoriteRepository.save(any(Favorite.class))).willAnswer(invocation -> invocation.getArgument(0));

        favoriteService.add(MEMBER_ID, PRODUCT_ID);

        verify(eventPublisher).publishEvent(new FavoriteAddedEvent(PRODUCT_ID));
    }

    @Test
    @DisplayName("관심 등록 실패(중복) 시에는 FavoriteAddedEvent를 발행하지 않는다")
    void add_duplicate_doesNotPublishEvent() {
        given(favoriteRepository.existsByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID)).willReturn(true);

        assertThatThrownBy(() -> favoriteService.add(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("접근 불가(존재하지 않거나 삭제·숨김) 상품에 관심 등록하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void add_productNotAccessible_throwsException() {
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(productService).validateAccessibleProduct(PRODUCT_ID);

        assertThatThrownBy(() -> favoriteService.add(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 관심 등록한 상품이면 FAVORITE_ALREADY_EXISTS 예외가 발생한다")
    void add_duplicate_throwsException() {
        given(favoriteRepository.existsByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID)).willReturn(true);

        assertThatThrownBy(() -> favoriteService.add(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAVORITE_ALREADY_EXISTS);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복 체크 통과 후 save() 시점에 UNIQUE 제약을 위반하면(race condition) FAVORITE_ALREADY_EXISTS로 변환한다")
    void add_raceCondition_throwsFavoriteAlreadyExists() {
        given(favoriteRepository.existsByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID)).willReturn(false);
        given(entityManager.find(Member.class, MEMBER_ID)).willReturn(mock(Member.class));
        given(entityManager.find(Product.class, PRODUCT_ID)).willReturn(mock(Product.class));
        given(favoriteRepository.save(any(Favorite.class)))
                .willThrow(new DataIntegrityViolationException("duplicate entry"));

        assertThatThrownBy(() -> favoriteService.add(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAVORITE_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("내 관심 상품 목록을 상품 요약과 함께 최근 등록순으로 반환한다")
    void getMyFavorites_success() {
        // 스텁 진행 중 중첩 스텁을 피하기 위해 목록을 먼저 구성한다.
        List<Favorite> favorites = List.of(
                favoriteWithProduct(200L, "아이패드"),
                favoriteWithProduct(100L, "맥북 프로"));
        given(favoriteRepository
                .findAllByMember_IdAndProduct_DeletedAtIsNullAndProduct_HiddenFalseOrderByCreatedAtDescIdDesc(MEMBER_ID))
                .willReturn(favorites);

        List<MyFavoriteResponse> responses = favoriteService.getMyFavorites(MEMBER_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(r -> r.getProduct().getProductId())
                .containsExactly(200L, 100L);
        assertThat(responses).extracting(r -> r.getProduct().getTitle())
                .containsExactly("아이패드", "맥북 프로");
    }

    @Test
    @DisplayName("관심 상품이 없으면 빈 목록을 반환한다")
    void getMyFavorites_empty() {
        given(favoriteRepository
                .findAllByMember_IdAndProduct_DeletedAtIsNullAndProduct_HiddenFalseOrderByCreatedAtDescIdDesc(MEMBER_ID))
                .willReturn(List.of());

        List<MyFavoriteResponse> responses = favoriteService.getMyFavorites(MEMBER_ID);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("등록한 관심 상품이면 취소에 성공한다")
    void remove_success() {
        Favorite favorite = Favorite.of(mock(Member.class), mock(Product.class));
        given(favoriteRepository.findByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID))
                .willReturn(Optional.of(favorite));

        favoriteService.remove(MEMBER_ID, PRODUCT_ID);

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    @DisplayName("관심 취소에 성공하면 해당 상품의 FavoriteRemovedEvent를 발행한다")
    void remove_success_publishesFavoriteRemovedEvent() {
        Favorite favorite = Favorite.of(mock(Member.class), mock(Product.class));
        given(favoriteRepository.findByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID))
                .willReturn(Optional.of(favorite));

        favoriteService.remove(MEMBER_ID, PRODUCT_ID);

        verify(eventPublisher).publishEvent(new FavoriteRemovedEvent(PRODUCT_ID));
    }

    @Test
    @DisplayName("관심 취소 실패(미등록) 시에는 FavoriteRemovedEvent를 발행하지 않는다")
    void remove_notFound_doesNotPublishEvent() {
        given(favoriteRepository.findByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.remove(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("등록하지 않은 상품을 취소하면 FAVORITE_NOT_FOUND 예외가 발생한다")
    void remove_notFound_throwsException() {
        given(favoriteRepository.findByMember_IdAndProduct_Id(MEMBER_ID, PRODUCT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.remove(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAVORITE_NOT_FOUND);

        verify(favoriteRepository, never()).delete(any());
    }
}
