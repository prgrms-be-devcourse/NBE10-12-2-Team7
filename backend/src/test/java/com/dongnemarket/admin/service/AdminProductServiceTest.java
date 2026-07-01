package com.dongnemarket.admin.service;

import com.dongnemarket.admin.repository.AdminProductRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * [단위] AdminProductService — 두 변이(hide/delete)와 공유 NOT_FOUND만 검증.
 *  - 제외: getProducts/getProduct 위임(통합), "이미 삭제·숨김 상품 재처리"(AD-27 갭)는 findById 동작(리포지토리/통합).
 *  - member·category는 hide/delete와 무관하므로 null 픽스처 사용.
 */
@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    AdminProductRepository adminProductRepository;

    @InjectMocks
    AdminProductService adminProductService;

    private Product existingProduct() {
        return Product.create(null, null, "부적절 상품", "설명", 10000, "서울시 강남구");
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("상품을 숨기면 hidden=true가 된다")
        void hideProduct_success() {
            Product product = existingProduct();
            given(adminProductRepository.findById(1L)).willReturn(Optional.of(product));

            adminProductService.hideProduct(1L);

            assertThat(product.isHidden()).isTrue();
        }

        @Test
        @DisplayName("상품을 삭제하면 softDelete 되어 deletedAt이 기록된다")
        void deleteProduct_success() {
            Product product = existingProduct();
            given(adminProductRepository.findById(1L)).willReturn(Optional.of(product));

            adminProductService.deleteProduct(1L);

            assertThat(product.isDeleted()).isTrue();
            assertThat(product.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("없는 상품을 숨기거나 삭제하면 PRODUCT_NOT_FOUND 예외가 발생한다")
        void notFound_throwsException() {
            given(adminProductRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminProductService.hideProduct(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}