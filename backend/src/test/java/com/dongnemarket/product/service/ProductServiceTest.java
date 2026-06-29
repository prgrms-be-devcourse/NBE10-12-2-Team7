package com.dongnemarket.product.service;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.dto.ProductCreateRequest;
import com.dongnemarket.product.dto.ProductResponse;
import com.dongnemarket.product.dto.ProductSearchRequest;
import com.dongnemarket.product.dto.ProductStatusUpdateRequest;
import com.dongnemarket.product.dto.ProductSummaryResponse;
import com.dongnemarket.product.dto.ProductUpdateRequest;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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

	@Test
	@DisplayName("상품 목록을 최신 등록순 요약 응답으로 조회한다")
	void getsProductsInLatestOrder() {
		Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
		Category category = new Category("디지털기기");
		Product oldProduct = Product.create(member, category, "오래된 상품", "오래된 상품 설명", 10000, "서울 강남구");
		Product newProduct = Product.create(member, category, "최신 상품", "최신 상품 설명", 20000, "서울 서초구");
		given(productRepository.findAllByDeletedAtIsNullAndHiddenFalseOrderByIdDesc())
				.willReturn(List.of(newProduct, oldProduct));

		List<ProductSummaryResponse> responses = productService.getProducts();

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).getTitle()).isEqualTo("최신 상품");
		assertThat(responses.get(1).getTitle()).isEqualTo("오래된 상품");
	}

	@Test
	@DisplayName("카테고리별 상품 목록을 최신 등록순 요약 응답으로 조회한다")
	void getsProductsByCategoryInLatestOrder() {
		Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
		Category category = new Category("디지털기기");
		Product oldProduct = Product.create(member, category, "오래된 상품", "오래된 상품 설명", 10000, "서울 강남구");
		Product newProduct = Product.create(member, category, "최신 상품", "최신 상품 설명", 20000, "서울 서초구");
		given(categoryRepository.existsById(1L)).willReturn(true);
		given(productRepository.findAllByCategoryIdAndDeletedAtIsNullAndHiddenFalseOrderByIdDesc(1L))
				.willReturn(List.of(newProduct, oldProduct));

		List<ProductSummaryResponse> responses = productService.getProductsByCategory(1L);

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).getTitle()).isEqualTo("최신 상품");
		assertThat(responses.get(1).getTitle()).isEqualTo("오래된 상품");
	}

	@Test
	@DisplayName("카테고리가 없으면 카테고리별 상품 목록 조회 시 CATEGORY_NOT_FOUND 예외가 발생한다")
	void throwsCategoryNotFoundWhenGettingProductsByMissingCategory() {
		given(categoryRepository.existsById(1L)).willReturn(false);

		assertThatThrownBy(() -> productService.getProductsByCategory(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

		verify(productRepository, never()).findAllByCategoryIdAndDeletedAtIsNullAndHiddenFalseOrderByIdDesc(any());
	}

	@Test
	@DisplayName("내 상품 목록을 최신 등록순 요약 응답으로 조회하고 숨김 상품도 포함한다")
	void getsMyProductsInLatestOrderIncludingHiddenProducts() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product oldProduct = Product.create(member, category, "오래된 내 상품", "오래된 내 상품 설명", 10000, "서울 강남구");
		Product hiddenProduct = Product.create(member, category, "숨김 내 상품", "숨김 내 상품 설명", 20000, "서울 서초구");
		hiddenProduct.hide();
		given(productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(1L))
				.willReturn(List.of(hiddenProduct, oldProduct));

		List<ProductSummaryResponse> responses = productService.getMyProducts(1L);

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).getTitle()).isEqualTo("숨김 내 상품");
		assertThat(responses.get(0).isHidden()).isTrue();
		assertThat(responses.get(1).getTitle()).isEqualTo("오래된 내 상품");
		assertThat(responses.get(1).isHidden()).isFalse();
	}

	@Test
	@DisplayName("내 상품이 없으면 빈 목록을 반환한다")
	void returnsEmptyListWhenMyProductsDoNotExist() {
		given(productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(1L))
				.willReturn(List.of());

		List<ProductSummaryResponse> responses = productService.getMyProducts(1L);

		assertThat(responses).isEmpty();
	}

	@Test
	@DisplayName("인증된 사용자 ID가 없으면 내 상품 목록 조회 시 UNAUTHORIZED 예외가 발생한다")
	void throwsUnauthorizedWhenGettingMyProductsWithoutMemberId() {
		assertThatThrownBy(() -> productService.getMyProducts(null))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

		verify(productRepository, never()).findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(any());
	}

	@Test
	@DisplayName("상품 검색 조건이 유효하면 요약 응답 목록을 반환한다")
	void searchesProducts() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "맥북 프로", "상태 좋은 맥북입니다.", 1200000, "서울 강남구");
		ProductSearchRequest request = new ProductSearchRequest("맥북", 1L, 1000000, 1500000, "ON_SALE");
		given(productRepository.findAll(anyProductSpecification(), any(org.springframework.data.domain.Sort.class)))
				.willReturn(List.of(product));

		List<ProductSummaryResponse> responses = productService.searchProducts(request);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getTitle()).isEqualTo("맥북 프로");
		ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
		verify(productRepository).findAll(anyProductSpecification(), sortCaptor.capture());
		assertThat(sortCaptor.getValue().getOrderFor("id")).isNotNull();
		assertThat(sortCaptor.getValue().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	@DisplayName("검색 최소 가격이 음수이면 INVALID_SEARCH_CONDITION 예외가 발생한다")
	void throwsInvalidSearchConditionWhenMinPriceIsNegative() {
		ProductSearchRequest request = new ProductSearchRequest(null, null, -1, null, null);

		assertThatThrownBy(() -> productService.searchProducts(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);

		verify(productRepository, never()).findAll(anyProductSpecification(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	@DisplayName("검색 최대 가격이 최소 가격보다 작으면 INVALID_SEARCH_CONDITION 예외가 발생한다")
	void throwsInvalidSearchConditionWhenMaxPriceIsLessThanMinPrice() {
		ProductSearchRequest request = new ProductSearchRequest(null, null, 20000, 10000, null);

		assertThatThrownBy(() -> productService.searchProducts(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);

		verify(productRepository, never()).findAll(anyProductSpecification(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	@DisplayName("검색 최대 가격이 음수이면 INVALID_SEARCH_CONDITION 예외가 발생한다")
	void throwsInvalidSearchConditionWhenMaxPriceIsNegative() {
		ProductSearchRequest request = new ProductSearchRequest(null, null, null, -1, null);

		assertThatThrownBy(() -> productService.searchProducts(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);

		verify(productRepository, never()).findAll(anyProductSpecification(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	@DisplayName("검색 거래 상태가 유효하지 않으면 INVALID_TRADE_STATUS 예외가 발생한다")
	void throwsInvalidTradeStatusWhenSearchingWithInvalidTradeStatus() {
		ProductSearchRequest request = new ProductSearchRequest(null, null, null, null, "INVALID");

		assertThatThrownBy(() -> productService.searchProducts(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_STATUS);

		verify(productRepository, never()).findAll(anyProductSpecification(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	@DisplayName("검색 거래 상태가 공백이면 INVALID_TRADE_STATUS 예외가 발생한다")
	void throwsInvalidTradeStatusWhenSearchingWithBlankTradeStatus() {
		ProductSearchRequest request = new ProductSearchRequest(null, null, null, null, " ");

		assertThatThrownBy(() -> productService.searchProducts(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_STATUS);

		verify(productRepository, never()).findAll(anyProductSpecification(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	@DisplayName("상품 상세 조회에 성공하면 조회수가 1 증가한다")
	void getsProductAndIncreasesViewCount() {
		Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		ProductResponse response = productService.getProduct(1L);

		assertThat(response.getTitle()).isEqualTo("아이폰 15");
		assertThat(response.getDescription()).isEqualTo("상태 좋은 아이폰입니다.");
		assertThat(response.getViewCount()).isEqualTo(1);
		assertThat(product.getViewCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("상품이 없으면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void throwsProductNotFoundWhenProductDoesNotExist() {
		given(productRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getProduct(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("삭제된 상품이면 DELETED_PRODUCT 예외가 발생한다")
	void throwsDeletedProductWhenProductIsDeleted() {
		Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.softDelete();
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.getProduct(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_PRODUCT);
	}

	@Test
	@DisplayName("숨김 상품이면 HIDDEN_PRODUCT 예외가 발생한다")
	void throwsHiddenProductWhenProductIsHidden() {
		Member member = Member.createUser("seller@example.com", "encodedPassword", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.hide();
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.getProduct(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.HIDDEN_PRODUCT);
	}

	@Test
	@DisplayName("작성자는 상품 정보를 수정할 수 있다")
	void updatesProductByOwner() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category oldCategory = new Category("디지털기기");
		Category newCategory = new Category("생활가전");
		Product product = Product.create(member, oldCategory, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		ProductUpdateRequest request = createUpdateRequest("맥북 프로", 1500000);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));
		given(categoryRepository.findById(request.getCategoryId())).willReturn(Optional.of(newCategory));

		ProductResponse response = productService.updateProduct(1L, 1L, request);

		assertThat(response.getTitle()).isEqualTo("맥북 프로");
		assertThat(response.getDescription()).isEqualTo("수정된 상품 설명입니다.");
		assertThat(response.getPrice()).isEqualTo(1500000);
		assertThat(response.getRegion()).isEqualTo("서울 서초구");
		assertThat(product.getCategory()).isEqualTo(newCategory);
	}

	@Test
	@DisplayName("수정할 상품이 없으면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void throwsProductNotFoundWhenUpdatingMissingProduct() {
		ProductUpdateRequest request = createUpdateRequest("맥북 프로", 1500000);
		given(productRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.updateProduct(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("삭제된 상품은 수정할 수 없다")
	void throwsDeletedProductWhenUpdatingDeletedProduct() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.softDelete();
		ProductUpdateRequest request = createUpdateRequest("맥북 프로", 1500000);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.updateProduct(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_PRODUCT);
	}

	@Test
	@DisplayName("작성자가 아니면 상품을 수정할 수 없다")
	void throwsProductOwnerOnlyWhenUpdatingByNonOwner() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		ProductUpdateRequest request = createUpdateRequest("맥북 프로", 1500000);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.updateProduct(2L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_OWNER_ONLY);
	}

	@Test
	@DisplayName("거래완료 상품은 수정할 수 없다")
	void throwsCannotUpdateCompletedProductWhenProductIsCompleted() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.complete();
		ProductUpdateRequest request = createUpdateRequest("맥북 프로", 1500000);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.updateProduct(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_UPDATE_COMPLETED_PRODUCT);
	}

	@Test
	@DisplayName("수정 요청 카테고리가 없으면 CATEGORY_NOT_FOUND 예외가 발생한다")
	void throwsCategoryNotFoundWhenUpdatingWithMissingCategory() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		ProductUpdateRequest request = createUpdateRequest("맥북 프로", 1500000);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));
		given(categoryRepository.findById(request.getCategoryId())).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.updateProduct(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
	}

	@Test
	@DisplayName("수정 제목이 비어 있으면 INVALID_PRODUCT_TITLE 예외가 발생한다")
	void throwsInvalidProductTitleWhenUpdatingWithBlankTitle() {
		ProductUpdateRequest request = createUpdateRequest(" ", 1500000);

		assertThatThrownBy(() -> productService.updateProduct(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRODUCT_TITLE);

		verify(productRepository, never()).findById(any());
	}

	@Test
	@DisplayName("수정 가격이 음수이면 INVALID_PRODUCT_PRICE 예외가 발생한다")
	void throwsInvalidProductPriceWhenUpdatingWithNegativePrice() {
		ProductUpdateRequest request = createUpdateRequest("맥북 프로", -1);

		assertThatThrownBy(() -> productService.updateProduct(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRODUCT_PRICE);

		verify(productRepository, never()).findById(any());
	}

	@Test
	@DisplayName("작성자는 상품 거래 상태를 변경할 수 있다")
	void updatesProductStatusByOwner() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("RESERVED");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		ProductResponse response = productService.updateProductStatus(1L, 1L, request);

		assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.RESERVED);
		assertThat(product.getTradeStatus()).isEqualTo(TradeStatus.RESERVED);
	}

	@Test
	@DisplayName("같은 거래 상태로 변경하면 현재 상태를 그대로 반환한다")
	void keepsProductStatusWhenRequestingSameStatus() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("ON_SALE");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		ProductResponse response = productService.updateProductStatus(1L, 1L, request);

		assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
		assertThat(product.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
	}

	@Test
	@DisplayName("거래완료 상품에 거래완료 상태를 다시 요청하면 현재 상태를 그대로 반환한다")
	void keepsCompletedProductStatusWhenRequestingCompletedAgain() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.complete();
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("COMPLETED");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		ProductResponse response = productService.updateProductStatus(1L, 1L, request);

		assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.COMPLETED);
		assertThat(product.getTradeStatus()).isEqualTo(TradeStatus.COMPLETED);
	}

	@Test
	@DisplayName("숨김 상품도 작성자라면 거래 상태를 변경할 수 있다")
	void updatesHiddenProductStatusByOwner() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.hide();
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("RESERVED");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		ProductResponse response = productService.updateProductStatus(1L, 1L, request);

		assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.RESERVED);
		assertThat(product.isHidden()).isTrue();
	}

	@Test
	@DisplayName("거래 상태를 변경할 상품이 없으면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void throwsProductNotFoundWhenUpdatingStatusOfMissingProduct() {
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("RESERVED");
		given(productRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.updateProductStatus(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("삭제된 상품은 거래 상태를 변경할 수 없다")
	void throwsDeletedProductWhenUpdatingStatusOfDeletedProduct() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.softDelete();
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("RESERVED");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.updateProductStatus(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_PRODUCT);
	}

	@Test
	@DisplayName("작성자가 아니면 상품 거래 상태를 변경할 수 없다")
	void throwsProductOwnerOnlyWhenUpdatingStatusByNonOwner() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("RESERVED");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.updateProductStatus(2L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_OWNER_ONLY);
	}

	@Test
	@DisplayName("거래완료 상품은 다른 거래 상태로 변경할 수 없다")
	void throwsCannotChangeCompletedProductWhenUpdatingCompletedStatus() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.complete();
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("ON_SALE");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.updateProductStatus(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_CHANGE_COMPLETED_PRODUCT);
	}

	@Test
	@DisplayName("거래 상태 값이 유효하지 않으면 INVALID_TRADE_STATUS 예외가 발생한다")
	void throwsInvalidTradeStatusWhenUpdatingWithInvalidStatus() {
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("INVALID");

		assertThatThrownBy(() -> productService.updateProductStatus(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_STATUS);

		verify(productRepository, never()).findById(any());
	}

	@Test
	@DisplayName("거래 상태 값이 비어 있으면 INVALID_TRADE_STATUS 예외가 발생한다")
	void throwsInvalidTradeStatusWhenUpdatingWithBlankStatus() {
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest(" ");

		assertThatThrownBy(() -> productService.updateProductStatus(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_STATUS);

		verify(productRepository, never()).findById(any());
	}

	@Test
	@DisplayName("거래 상태 값이 null이면 INVALID_TRADE_STATUS 예외가 발생한다")
	void throwsInvalidTradeStatusWhenUpdatingWithNullStatus() {
		ProductStatusUpdateRequest request = new ProductStatusUpdateRequest(null);

		assertThatThrownBy(() -> productService.updateProductStatus(1L, 1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_STATUS);

		verify(productRepository, never()).findById(any());
	}

	@Test
	@DisplayName("거래 상태 변경 요청 객체가 null이면 INVALID_TRADE_STATUS 예외가 발생한다")
	void throwsInvalidTradeStatusWhenUpdatingWithNullRequest() {
		assertThatThrownBy(() -> productService.updateProductStatus(1L, 1L, null))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_STATUS);

		verify(productRepository, never()).findById(any());
	}

	@Test
	@DisplayName("작성자는 상품을 논리 삭제할 수 있다")
	void deletesProductByOwner() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		productService.deleteProduct(1L, 1L);

		assertThat(product.isDeleted()).isTrue();
		assertThat(product.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("거래완료 상품도 작성자라면 논리 삭제할 수 있다")
	void deletesCompletedProductByOwner() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		ReflectionTestUtils.setField(product, "tradeStatus", TradeStatus.COMPLETED);
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		productService.deleteProduct(1L, 1L);

		assertThat(product.isDeleted()).isTrue();
		assertThat(product.getTradeStatus()).isEqualTo(TradeStatus.COMPLETED);
	}

	@Test
	@DisplayName("삭제할 상품이 없으면 PRODUCT_NOT_FOUND 예외가 발생한다")
	void throwsProductNotFoundWhenDeletingMissingProduct() {
		given(productRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.deleteProduct(1L, 1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("이미 삭제된 상품은 다시 삭제할 수 없다")
	void throwsDeletedProductWhenDeletingDeletedProduct() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.softDelete();
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.deleteProduct(1L, 1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_PRODUCT);
	}

	@Test
	@DisplayName("작성자가 아니면 상품을 삭제할 수 없다")
	void throwsProductOwnerOnlyWhenDeletingByNonOwner() {
		Member member = createMemberWithId(1L, "seller@example.com", "판매자");
		Category category = new Category("디지털기기");
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		given(productRepository.findById(1L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.deleteProduct(2L, 1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_OWNER_ONLY);
	}

	@Test
	@DisplayName("접근 가능한 상품이면 검증을 통과한다")
	void validatesAccessibleProduct() {
		given(productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(1L)).willReturn(true);

		productService.validateAccessibleProduct(1L);

		verify(productRepository).existsByIdAndDeletedAtIsNullAndHiddenFalse(1L);
	}

	@Test
	@DisplayName("존재하지 않는 상품이면 접근 가능한 상품 검증 시 PRODUCT_NOT_FOUND 예외가 발생한다")
	void throwsProductNotFoundWhenValidatingMissingProduct() {
		given(productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(1L)).willReturn(false);

		assertThatThrownBy(() -> productService.validateAccessibleProduct(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("삭제된 상품이면 접근 가능한 상품 검증 시 PRODUCT_NOT_FOUND 예외가 발생한다")
	void throwsProductNotFoundWhenValidatingDeletedProduct() {
		given(productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(1L)).willReturn(false);

		assertThatThrownBy(() -> productService.validateAccessibleProduct(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	@DisplayName("숨김 상품이면 접근 가능한 상품 검증 시 PRODUCT_NOT_FOUND 예외가 발생한다")
	void throwsProductNotFoundWhenValidatingHiddenProduct() {
		given(productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(1L)).willReturn(false);

		assertThatThrownBy(() -> productService.validateAccessibleProduct(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
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

	private ProductUpdateRequest createUpdateRequest(String title, Integer price) {
		return new ProductUpdateRequest(
				2L,
				title,
				"수정된 상품 설명입니다.",
				price,
				"서울 서초구"
		);
	}

	private Member createMemberWithId(Long id, String email, String nickname) {
		Member member = Member.createUser(email, "encodedPassword", nickname);
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	private Specification<Product> anyProductSpecification() {
		return org.mockito.ArgumentMatchers.any();
	}
}
