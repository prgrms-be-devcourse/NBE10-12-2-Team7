package com.dongnemarket.admin.service;

import java.math.BigDecimal;

import com.dongnemarket.admin.dto.AdminProductResponse;
import com.dongnemarket.admin.repository.AdminProductRepository;
import com.dongnemarket.category.entity.Category;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    AdminProductRepository adminProductRepository;

    @InjectMocks
    AdminProductService adminProductService;

    @Test
    @DisplayName("상품 목록을 조회하면 숨김·삭제와 무관하게 전체 상품을 반환한다")
    void getProducts_success() {
        Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
        Category category = new Category("디지털기기");
        Product visible = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰", BigDecimal.valueOf(800000), "서울 강남구");
        Product hidden = Product.create(member, category, "숨김 상품", "숨김 처리됨", BigDecimal.valueOf(5000), "서울 서초구");
        hidden.hide();
        given(adminProductRepository.findAll()).willReturn(List.of(visible, hidden));

        List<AdminProductResponse> responses = adminProductService.getProducts();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(AdminProductResponse::getTitle)
                .containsExactly("아이폰 15", "숨김 상품");
        assertThat(responses.get(1).isHidden()).isTrue();   // 숨김 상품도 그대로 노출
    }

    @Test
    @DisplayName("상품이 없으면 빈 목록을 반환한다")
    void getProducts_empty_returnsEmptyList() {
        given(adminProductRepository.findAll()).willReturn(List.of());

        List<AdminProductResponse> responses = adminProductService.getProducts();

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("존재하는 productId로 상세 조회하면 해당 상품을 반환한다")
    void getProduct_success() {
        Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
        Category category = new Category("디지털기기");
        Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰", BigDecimal.valueOf(800000), "서울 강남구");
        given(adminProductRepository.findById(1L)).willReturn(Optional.of(product));

        AdminProductResponse response = adminProductService.getProduct(1L);

        assertThat(response.getTitle()).isEqualTo("아이폰 15");
        assertThat(response.getPrice()).isEqualByComparingTo("800000");
        assertThat(response.getDescription()).isEqualTo("상태 좋은 아이폰");
    }

    @Test
    @DisplayName("존재하지 않는 productId로 상세 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void getProduct_notFound_throwsException() {
        given(adminProductRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminProductService.getProduct(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자가 상품을 숨김 처리하면 hidden이 true가 된다")
    void hideProduct_success() {
        Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
        Category category = new Category("디지털기기");
        Product product = Product.create(member, category, "아이폰 15", "설명", BigDecimal.valueOf(800000), "서울 강남구");
        given(adminProductRepository.findById(1L)).willReturn(Optional.of(product));

        adminProductService.hideProduct(1L);

        assertThat(product.isHidden()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상품을 숨김 처리하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void hideProduct_notFound_throwsException() {
        given(adminProductRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminProductService.hideProduct(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자가 상품을 삭제하면 소프트 삭제된다")
    void deleteProduct_success() {
        Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
        Category category = new Category("디지털기기");
        Product product = Product.create(member, category, "아이폰 15", "설명", BigDecimal.valueOf(800000), "서울 강남구");
        given(adminProductRepository.findById(1L)).willReturn(Optional.of(product));

        adminProductService.deleteProduct(1L);

        assertThat(product.isDeleted()).isTrue();
        assertThat(product.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 상품을 삭제하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void deleteProduct_notFound_throwsException() {
        given(adminProductRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminProductService.deleteProduct(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }
}
