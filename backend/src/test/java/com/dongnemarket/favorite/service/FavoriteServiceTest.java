package com.dongnemarket.favorite.service;

import com.dongnemarket.favorite.dto.FavoriteResponse;
import com.dongnemarket.favorite.entity.Favorite;
import com.dongnemarket.favorite.repository.FavoriteRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    FavoriteRepository favoriteRepository;

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    FavoriteService favoriteService;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @Test
    @DisplayName("상품이 존재하고 아직 관심 등록하지 않았으면 관심 등록에 성공한다")
    void add_success() {
        given(productRepository.existsById(PRODUCT_ID)).willReturn(true);
        given(favoriteRepository.existsByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).willReturn(false);
        given(favoriteRepository.save(any(Favorite.class))).willAnswer(invocation -> invocation.getArgument(0));

        FavoriteResponse response = favoriteService.add(MEMBER_ID, PRODUCT_ID);

        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("존재하지 않는 상품에 관심 등록하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void add_productNotFound_throwsException() {
        given(productRepository.existsById(PRODUCT_ID)).willReturn(false);

        assertThatThrownBy(() -> favoriteService.add(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 관심 등록한 상품이면 FAVORITE_ALREADY_EXISTS 예외가 발생한다")
    void add_duplicate_throwsException() {
        given(productRepository.existsById(PRODUCT_ID)).willReturn(true);
        given(favoriteRepository.existsByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).willReturn(true);

        assertThatThrownBy(() -> favoriteService.add(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAVORITE_ALREADY_EXISTS);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복 체크 통과 후 save() 시점에 UNIQUE 제약을 위반하면(race condition) FAVORITE_ALREADY_EXISTS로 변환한다")
    void add_raceCondition_throwsFavoriteAlreadyExists() {
        given(productRepository.existsById(PRODUCT_ID)).willReturn(true);
        given(favoriteRepository.existsByMemberIdAndProductId(MEMBER_ID, PRODUCT_ID)).willReturn(false);
        given(favoriteRepository.save(any(Favorite.class)))
                .willThrow(new DataIntegrityViolationException("duplicate entry"));

        assertThatThrownBy(() -> favoriteService.add(MEMBER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAVORITE_ALREADY_EXISTS);
    }
}
