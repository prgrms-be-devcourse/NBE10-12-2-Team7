package com.dongnemarket.product.service;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.dto.ProductCreateRequest;
import com.dongnemarket.product.dto.ProductResponse;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	MemberRepository memberRepository;

	@Mock
	CategoryRepository categoryRepository;

	@Mock
	ProductRepository productRepository;

	@InjectMocks
	ProductService productService;

	@Test
	@DisplayName("상품 등록에 성공하면 저장된 상품 응답을 반환한다")
	void createsProduct() {
		Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
		Category category = new Category("디지털기기");
		ProductCreateRequest request = new ProductCreateRequest(
				category.getId(),
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				800000,
				"서울 강남구"
		);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(categoryRepository.findById(request.getCategoryId())).willReturn(Optional.of(category));
		given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

		ProductResponse response = productService.createProduct(1L, request);

		assertThat(response.getTitle()).isEqualTo("아이폰 15");
		assertThat(response.getDescription()).isEqualTo("상태 좋은 아이폰입니다.");
		assertThat(response.getPrice()).isEqualTo(800000);
		assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
		assertThat(response.getRegion()).isEqualTo("서울 강남구");
		assertThat(response.getViewCount()).isZero();
		assertThat(response.isHidden()).isFalse();
	}

	@Test
	@DisplayName("회원이 없으면 MEMBER_NOT_FOUND 예외가 발생한다")
	void throwsMemberNotFoundWhenMemberDoesNotExist() {
		ProductCreateRequest request = createRequest("아이폰 15", 800000);
		given(memberRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.createProduct(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("카테고리가 없으면 CATEGORY_NOT_FOUND 예외가 발생한다")
	void throwsCategoryNotFoundWhenCategoryDoesNotExist() {
		Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
		ProductCreateRequest request = createRequest("아이폰 15", 800000);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(categoryRepository.findById(request.getCategoryId())).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.createProduct(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("제목이 비어 있으면 INVALID_PRODUCT_TITLE 예외가 발생한다")
	void throwsInvalidProductTitleWhenTitleIsBlank() {
		ProductCreateRequest request = createRequest(" ", 800000);

		assertThatThrownBy(() -> productService.createProduct(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRODUCT_TITLE);

		verify(memberRepository, never()).findById(any());
		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("가격이 음수이면 INVALID_PRODUCT_PRICE 예외가 발생한다")
	void throwsInvalidProductPriceWhenPriceIsNegative() {
		ProductCreateRequest request = createRequest("아이폰 15", -1);

		assertThatThrownBy(() -> productService.createProduct(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRODUCT_PRICE);

		verify(memberRepository, never()).findById(any());
		verify(productRepository, never()).save(any());
	}

	private ProductCreateRequest createRequest(String title, Integer price) {
		return new ProductCreateRequest(
				1L,
				title,
				"상태 좋은 아이폰입니다.",
				price,
				"서울 강남구"
		);
	}
}
