package com.dongnemarket.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	private static final Long SELLER_ID = 1L;
	private static final Long OTHER_MEMBER_ID = 2L;
	private static final Long PRODUCT_ID = 1L;
	private static final Long CATEGORY_ID = 1L;
	private static final Long UPDATE_CATEGORY_ID = 2L;

	@Mock
	MemberRepository memberRepository;

	@Mock
	CategoryRepository categoryRepository;

	@Mock
	ProductRepository productRepository;

	@InjectMocks
	ProductService productService;

	@Nested
	@DisplayName("상품 등록")
	class CreateProduct {

		@Test
		@DisplayName("정상 요청이면 상품을 등록하고 기본 상태를 반환한다")
		void createsProduct() {
			Member member = seller();
			Category category = category("디지털기기");
			ProductCreateRequest request = createRequest("아이폰 15", BigDecimal.valueOf(800000));
			given(memberRepository.findById(SELLER_ID)).willReturn(Optional.of(member));
			given(categoryRepository.findById(request.getCategoryId())).willReturn(Optional.of(category));
			given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

			ProductResponse response = productService.createProduct(SELLER_ID, request);

			assertThat(response.getTitle()).isEqualTo("아이폰 15");
			assertThat(response.getDescription()).isEqualTo("상태 좋은 아이폰입니다.");
			assertThat(response.getPrice()).isEqualByComparingTo("800000");
			assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
			assertThat(response.getRegion()).isEqualTo("서울 강남구");
			assertThat(response.getViewCount()).isZero();
			assertThat(response.isHidden()).isFalse();
		}

		@Test
		@DisplayName("회원이 없으면 상품을 등록할 수 없다")
		void throwsMemberNotFoundWhenMemberDoesNotExist() {
			ProductCreateRequest request = createRequest("아이폰 15", BigDecimal.valueOf(800000));
			given(memberRepository.findById(SELLER_ID)).willReturn(Optional.empty());

			assertBusinessException(
					() -> productService.createProduct(SELLER_ID, request),
					ErrorCode.MEMBER_NOT_FOUND
			);

			verify(productRepository, never()).save(any());
		}

		@Test
		@DisplayName("카테고리가 없으면 상품을 등록할 수 없다")
		void throwsCategoryNotFoundWhenCategoryDoesNotExist() {
			ProductCreateRequest request = createRequest("아이폰 15", BigDecimal.valueOf(800000));
			given(memberRepository.findById(SELLER_ID)).willReturn(Optional.of(seller()));
			given(categoryRepository.findById(request.getCategoryId())).willReturn(Optional.empty());

			assertBusinessException(
					() -> productService.createProduct(SELLER_ID, request),
					ErrorCode.CATEGORY_NOT_FOUND
			);

			verify(productRepository, never()).save(any());
		}

		@Test
		@DisplayName("제목이 공백이면 상품을 등록할 수 없다")
		void throwsInvalidProductTitleWhenTitleIsBlank() {
			ProductCreateRequest request = createRequest(" ", BigDecimal.valueOf(800000));

			assertBusinessException(
					() -> productService.createProduct(SELLER_ID, request),
					ErrorCode.INVALID_PRODUCT_TITLE
			);

			verify(memberRepository, never()).findById(any());
			verify(productRepository, never()).save(any());
		}

		@Test
		@DisplayName("가격이 음수이면 상품을 등록할 수 없다")
		void throwsInvalidProductPriceWhenPriceIsNegative() {
			ProductCreateRequest request = createRequest("아이폰 15", BigDecimal.valueOf(-1));

			assertBusinessException(
					() -> productService.createProduct(SELLER_ID, request),
					ErrorCode.INVALID_PRODUCT_PRICE
			);

			verify(memberRepository, never()).findById(any());
			verify(productRepository, never()).save(any());
		}

		@Test
		@DisplayName("가격이 null이면 상품을 등록할 수 없다")
		void throwsInvalidProductPriceWhenPriceIsNull() {
			ProductCreateRequest request = createRequest("아이폰 15", null);

			assertBusinessException(
					() -> productService.createProduct(SELLER_ID, request),
					ErrorCode.INVALID_PRODUCT_PRICE
			);

			verify(memberRepository, never()).findById(any());
			verify(productRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("상품 목록 조회")
	class GetProducts {

		@Test
		@DisplayName("전체 상품 목록을 최신 등록순으로 조회한다")
		void getsProductsInLatestOrder() {
			Product oldProduct = product("오래된 상품", BigDecimal.valueOf(10000));
			Product newProduct = product("최신 상품", BigDecimal.valueOf(20000));
			given(productRepository.findAllByDeletedAtIsNullAndHiddenFalseOrderByIdDesc())
					.willReturn(List.of(newProduct, oldProduct));

			List<ProductSummaryResponse> responses = productService.getProducts();

			assertThat(responses).extracting(ProductSummaryResponse::getTitle)
					.containsExactly("최신 상품", "오래된 상품");
		}

		@Test
		@DisplayName("카테고리별 상품 목록을 최신 등록순으로 조회한다")
		void getsProductsByCategoryInLatestOrder() {
			Product oldProduct = product("오래된 상품", BigDecimal.valueOf(10000));
			Product newProduct = product("최신 상품", BigDecimal.valueOf(20000));
			given(categoryRepository.existsById(CATEGORY_ID)).willReturn(true);
			given(productRepository.findAllByCategoryIdAndDeletedAtIsNullAndHiddenFalseOrderByIdDesc(CATEGORY_ID))
					.willReturn(List.of(newProduct, oldProduct));

			List<ProductSummaryResponse> responses = productService.getProductsByCategory(CATEGORY_ID);

			assertThat(responses).extracting(ProductSummaryResponse::getTitle)
					.containsExactly("최신 상품", "오래된 상품");
		}

		@Test
		@DisplayName("카테고리가 없으면 카테고리별 상품 목록을 조회할 수 없다")
		void throwsCategoryNotFoundWhenGettingProductsByMissingCategory() {
			given(categoryRepository.existsById(CATEGORY_ID)).willReturn(false);

			assertBusinessException(
					() -> productService.getProductsByCategory(CATEGORY_ID),
					ErrorCode.CATEGORY_NOT_FOUND
			);

			verify(productRepository, never()).findAllByCategoryIdAndDeletedAtIsNullAndHiddenFalseOrderByIdDesc(any());
		}
	}

	@Nested
	@DisplayName("내 상품 조회")
	class GetMyProducts {

		@Test
		@DisplayName("내 상품 목록은 숨김 상품을 포함해서 최신 등록순으로 조회한다")
		void getsMyProductsInLatestOrderIncludingHiddenProducts() {
			Product oldProduct = product("오래된 내 상품", BigDecimal.valueOf(10000));
			Product hiddenProduct = hiddenProduct("숨김 내 상품");
			given(productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(SELLER_ID))
					.willReturn(List.of(hiddenProduct, oldProduct));

			List<ProductSummaryResponse> responses = productService.getMyProducts(SELLER_ID);

			assertThat(responses).hasSize(2);
			assertThat(responses.get(0).getTitle()).isEqualTo("숨김 내 상품");
			assertThat(responses.get(0).isHidden()).isTrue();
			assertThat(responses.get(1).getTitle()).isEqualTo("오래된 내 상품");
			assertThat(responses.get(1).isHidden()).isFalse();
		}

		@Test
		@DisplayName("내 상품이 없으면 빈 목록을 반환한다")
		void returnsEmptyListWhenMyProductsDoNotExist() {
			given(productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(SELLER_ID))
					.willReturn(List.of());

			List<ProductSummaryResponse> responses = productService.getMyProducts(SELLER_ID);

			assertThat(responses).isEmpty();
		}

		@Test
		@DisplayName("인증된 사용자 ID가 없으면 내 상품 목록을 조회할 수 없다")
		void throwsUnauthorizedWhenGettingMyProductsWithoutMemberId() {
			assertBusinessException(
					() -> productService.getMyProducts(null),
					ErrorCode.UNAUTHORIZED
			);

			verify(productRepository, never()).findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(any());
		}
	}

	@Nested
	@DisplayName("상품 검색")
	class SearchProducts {

		@Test
		@DisplayName("검색 조건이 유효하면 최신 등록순 요약 응답을 반환한다")
		void searchesProducts() {
			Product product = product("맥북 프로", BigDecimal.valueOf(1200000));
			ProductSearchRequest request = new ProductSearchRequest(
					"맥북",
					CATEGORY_ID,
					BigDecimal.valueOf(1000000),
					BigDecimal.valueOf(1500000),
					"ON_SALE"
			);
			given(productRepository.findAll(anyProductSpecification(), any(Sort.class))).willReturn(List.of(product));

			List<ProductSummaryResponse> responses = productService.searchProducts(request);

			assertThat(responses).hasSize(1);
			assertThat(responses.get(0).getTitle()).isEqualTo("맥북 프로");

			ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
			verify(productRepository).findAll(anyProductSpecification(), sortCaptor.capture());
			Sort.Order idOrder = sortCaptor.getValue().getOrderFor("id");
			assertThat(idOrder).isNotNull();
			assertThat(idOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
		}

		@Test
		@DisplayName("검색 최소 가격이 음수이면 검색할 수 없다")
		void throwsInvalidSearchConditionWhenMinPriceIsNegative() {
			ProductSearchRequest request = new ProductSearchRequest(null, null, BigDecimal.valueOf(-1), null, null);

			assertInvalidSearchCondition(request);
		}

		@Test
		@DisplayName("검색 최대 가격이 음수이면 검색할 수 없다")
		void throwsInvalidSearchConditionWhenMaxPriceIsNegative() {
			ProductSearchRequest request = new ProductSearchRequest(null, null, null, BigDecimal.valueOf(-1), null);

			assertInvalidSearchCondition(request);
		}

		@Test
		@DisplayName("검색 최대 가격이 최소 가격보다 작으면 검색할 수 없다")
		void throwsInvalidSearchConditionWhenMaxPriceIsLessThanMinPrice() {
			ProductSearchRequest request = new ProductSearchRequest(
					null,
					null,
					BigDecimal.valueOf(20000),
					BigDecimal.valueOf(10000),
					null
			);

			assertInvalidSearchCondition(request);
		}

		@Test
		@DisplayName("검색 거래 상태가 유효하지 않으면 검색할 수 없다")
		void throwsInvalidTradeStatusWhenSearchingWithInvalidTradeStatus() {
			ProductSearchRequest request = new ProductSearchRequest(null, null, null, null, "INVALID");

			assertInvalidTradeStatus(() -> productService.searchProducts(request));
		}

		@Test
		@DisplayName("검색 거래 상태가 공백이면 검색할 수 없다")
		void throwsInvalidTradeStatusWhenSearchingWithBlankTradeStatus() {
			ProductSearchRequest request = new ProductSearchRequest(null, null, null, null, " ");

			assertInvalidTradeStatus(() -> productService.searchProducts(request));
		}
	}

	@Nested
	@DisplayName("상품 상세 조회")
	class GetProduct {

		@Test
		@DisplayName("상세 조회에 성공하면 조회수가 1 증가한다")
		void getsProductAndIncreasesViewCount() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			ProductResponse response = productService.getProduct(PRODUCT_ID);

			assertThat(response.getTitle()).isEqualTo("아이폰 15");
			assertThat(response.getDescription()).isEqualTo("상품 설명입니다.");
			assertThat(response.getViewCount()).isEqualTo(1);
			assertThat(product.getViewCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("상품이 없으면 상세 조회를 할 수 없다")
		void throwsProductNotFoundWhenProductDoesNotExist() {
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

			assertBusinessException(
					() -> productService.getProduct(PRODUCT_ID),
					ErrorCode.PRODUCT_NOT_FOUND
			);
		}

		@Test
		@DisplayName("삭제된 상품이면 상세 조회를 할 수 없다")
		void throwsDeletedProductWhenProductIsDeleted() {
			Product product = deletedProduct("아이폰 15");
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.getProduct(PRODUCT_ID),
					ErrorCode.DELETED_PRODUCT
			);
		}

		@Test
		@DisplayName("숨김 상품이면 상세 조회를 할 수 없다")
		void throwsHiddenProductWhenProductIsHidden() {
			Product product = hiddenProduct("아이폰 15");
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.getProduct(PRODUCT_ID),
					ErrorCode.HIDDEN_PRODUCT
			);
		}
	}

	@Nested
	@DisplayName("상품 수정")
	class UpdateProduct {

		@Test
		@DisplayName("작성자는 상품 정보를 수정할 수 있다")
		void updatesProductByOwner() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			Category newCategory = category("생활가전");
			ProductUpdateRequest request = updateRequest("맥북 프로", BigDecimal.valueOf(1500000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
			given(categoryRepository.findById(request.getCategoryId())).willReturn(Optional.of(newCategory));

			ProductResponse response = productService.updateProduct(SELLER_ID, PRODUCT_ID, request);

			assertThat(response.getTitle()).isEqualTo("맥북 프로");
			assertThat(response.getDescription()).isEqualTo("수정된 상품 설명입니다.");
			assertThat(response.getPrice()).isEqualByComparingTo("1500000");
			assertThat(response.getRegion()).isEqualTo("서울 서초구");
			assertThat(product.getCategory()).isEqualTo(newCategory);
		}

		@Test
		@DisplayName("수정할 상품이 없으면 수정할 수 없다")
		void throwsProductNotFoundWhenUpdatingMissingProduct() {
			ProductUpdateRequest request = updateRequest("맥북 프로", BigDecimal.valueOf(1500000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

			assertBusinessException(
					() -> productService.updateProduct(SELLER_ID, PRODUCT_ID, request),
					ErrorCode.PRODUCT_NOT_FOUND
			);
		}

		@Test
		@DisplayName("삭제된 상품은 수정할 수 없다")
		void throwsDeletedProductWhenUpdatingDeletedProduct() {
			Product product = deletedProduct("아이폰 15");
			ProductUpdateRequest request = updateRequest("맥북 프로", BigDecimal.valueOf(1500000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.updateProduct(SELLER_ID, PRODUCT_ID, request),
					ErrorCode.DELETED_PRODUCT
			);
		}

		@Test
		@DisplayName("작성자가 아니면 수정할 수 없다")
		void throwsProductOwnerOnlyWhenUpdatingByNonOwner() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			ProductUpdateRequest request = updateRequest("맥북 프로", BigDecimal.valueOf(1500000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.updateProduct(OTHER_MEMBER_ID, PRODUCT_ID, request),
					ErrorCode.PRODUCT_OWNER_ONLY
			);
		}

		@Test
		@DisplayName("거래완료 상품은 수정할 수 없다")
		void throwsCannotUpdateCompletedProductWhenProductIsCompleted() {
			Product product = completedProduct("아이폰 15");
			ProductUpdateRequest request = updateRequest("맥북 프로", BigDecimal.valueOf(1500000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.updateProduct(SELLER_ID, PRODUCT_ID, request),
					ErrorCode.CANNOT_UPDATE_COMPLETED_PRODUCT
			);
		}

		@Test
		@DisplayName("수정 요청 카테고리가 없으면 수정할 수 없다")
		void throwsCategoryNotFoundWhenUpdatingWithMissingCategory() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			ProductUpdateRequest request = updateRequest("맥북 프로", BigDecimal.valueOf(1500000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
			given(categoryRepository.findById(request.getCategoryId())).willReturn(Optional.empty());

			assertBusinessException(
					() -> productService.updateProduct(SELLER_ID, PRODUCT_ID, request),
					ErrorCode.CATEGORY_NOT_FOUND
			);
		}

		@Test
		@DisplayName("수정 제목이 공백이면 수정할 수 없다")
		void throwsInvalidProductTitleWhenUpdatingWithBlankTitle() {
			ProductUpdateRequest request = updateRequest(" ", BigDecimal.valueOf(1500000));

			assertBusinessException(
					() -> productService.updateProduct(SELLER_ID, PRODUCT_ID, request),
					ErrorCode.INVALID_PRODUCT_TITLE
			);

			verify(productRepository, never()).findById(any());
		}

		@Test
		@DisplayName("수정 가격이 음수이면 수정할 수 없다")
		void throwsInvalidProductPriceWhenUpdatingWithNegativePrice() {
			ProductUpdateRequest request = updateRequest("맥북 프로", BigDecimal.valueOf(-1));

			assertBusinessException(
					() -> productService.updateProduct(SELLER_ID, PRODUCT_ID, request),
					ErrorCode.INVALID_PRODUCT_PRICE
			);

			verify(productRepository, never()).findById(any());
		}

		@Test
		@DisplayName("수정 가격이 null이면 수정할 수 없다")
		void throwsInvalidProductPriceWhenUpdatingWithNullPrice() {
			ProductUpdateRequest request = updateRequest("맥북 프로", null);

			assertBusinessException(
					() -> productService.updateProduct(SELLER_ID, PRODUCT_ID, request),
					ErrorCode.INVALID_PRODUCT_PRICE
			);

			verify(productRepository, never()).findById(any());
		}
	}

	@Nested
	@DisplayName("거래 상태 변경")
	class UpdateProductStatus {

		@Test
		@DisplayName("작성자는 거래 상태를 변경할 수 있다")
		void updatesProductStatusByOwner() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			ProductResponse response = productService.updateProductStatus(
					SELLER_ID,
					PRODUCT_ID,
					statusRequest("RESERVED")
			);

			assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.RESERVED);
			assertThat(product.getTradeStatus()).isEqualTo(TradeStatus.RESERVED);
		}

		@Test
		@DisplayName("같은 거래 상태를 요청하면 현재 상태를 그대로 반환한다")
		void keepsProductStatusWhenRequestingSameStatus() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			ProductResponse response = productService.updateProductStatus(
					SELLER_ID,
					PRODUCT_ID,
					statusRequest("ON_SALE")
			);

			assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
			assertThat(product.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
		}

		@Test
		@DisplayName("거래완료 상품에 거래완료를 다시 요청하면 현재 상태를 그대로 반환한다")
		void keepsCompletedProductStatusWhenRequestingCompletedAgain() {
			Product product = completedProduct("아이폰 15");
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			ProductResponse response = productService.updateProductStatus(
					SELLER_ID,
					PRODUCT_ID,
					statusRequest("COMPLETED")
			);

			assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.COMPLETED);
			assertThat(product.getTradeStatus()).isEqualTo(TradeStatus.COMPLETED);
		}

		@Test
		@DisplayName("숨김 상품도 작성자라면 거래 상태를 변경할 수 있다")
		void updatesHiddenProductStatusByOwner() {
			Product product = hiddenProduct("아이폰 15");
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			ProductResponse response = productService.updateProductStatus(
					SELLER_ID,
					PRODUCT_ID,
					statusRequest("RESERVED")
			);

			assertThat(response.getTradeStatus()).isEqualTo(TradeStatus.RESERVED);
			assertThat(product.isHidden()).isTrue();
		}

		@Test
		@DisplayName("거래 상태를 변경할 상품이 없으면 변경할 수 없다")
		void throwsProductNotFoundWhenUpdatingStatusOfMissingProduct() {
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

			assertBusinessException(
					() -> productService.updateProductStatus(SELLER_ID, PRODUCT_ID, statusRequest("RESERVED")),
					ErrorCode.PRODUCT_NOT_FOUND
			);
		}

		@Test
		@DisplayName("삭제된 상품은 거래 상태를 변경할 수 없다")
		void throwsDeletedProductWhenUpdatingStatusOfDeletedProduct() {
			Product product = deletedProduct("아이폰 15");
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.updateProductStatus(SELLER_ID, PRODUCT_ID, statusRequest("RESERVED")),
					ErrorCode.DELETED_PRODUCT
			);
		}

		@Test
		@DisplayName("작성자가 아니면 거래 상태를 변경할 수 없다")
		void throwsProductOwnerOnlyWhenUpdatingStatusByNonOwner() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.updateProductStatus(OTHER_MEMBER_ID, PRODUCT_ID, statusRequest("RESERVED")),
					ErrorCode.PRODUCT_OWNER_ONLY
			);
		}

		@Test
		@DisplayName("거래완료 상품은 다른 거래 상태로 되돌릴 수 없다")
		void throwsCannotChangeCompletedProductWhenUpdatingCompletedStatus() {
			Product product = completedProduct("아이폰 15");
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.updateProductStatus(SELLER_ID, PRODUCT_ID, statusRequest("ON_SALE")),
					ErrorCode.CANNOT_CHANGE_COMPLETED_PRODUCT
			);
		}

		@Test
		@DisplayName("거래 상태 값이 유효하지 않으면 변경할 수 없다")
		void throwsInvalidTradeStatusWhenUpdatingWithInvalidStatus() {
			assertInvalidTradeStatus(
					() -> productService.updateProductStatus(SELLER_ID, PRODUCT_ID, statusRequest("INVALID"))
			);
		}

		@Test
		@DisplayName("거래 상태 값이 공백이면 변경할 수 없다")
		void throwsInvalidTradeStatusWhenUpdatingWithBlankStatus() {
			assertInvalidTradeStatus(
					() -> productService.updateProductStatus(SELLER_ID, PRODUCT_ID, statusRequest(" "))
			);
		}

		@Test
		@DisplayName("거래 상태 값이 null이면 변경할 수 없다")
		void throwsInvalidTradeStatusWhenUpdatingWithNullStatus() {
			assertInvalidTradeStatus(
					() -> productService.updateProductStatus(SELLER_ID, PRODUCT_ID, statusRequest(null))
			);
		}

		@Test
		@DisplayName("거래 상태 변경 요청 객체가 null이면 변경할 수 없다")
		void throwsInvalidTradeStatusWhenUpdatingWithNullRequest() {
			assertInvalidTradeStatus(
					() -> productService.updateProductStatus(SELLER_ID, PRODUCT_ID, null)
			);
		}
	}

	@Nested
	@DisplayName("상품 삭제")
	class DeleteProduct {

		@Test
		@DisplayName("작성자는 상품을 논리 삭제할 수 있다")
		void deletesProductByOwner() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			productService.deleteProduct(SELLER_ID, PRODUCT_ID);

			assertThat(product.isDeleted()).isTrue();
			assertThat(product.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("거래완료 상품도 작성자라면 논리 삭제할 수 있다")
		void deletesCompletedProductByOwner() {
			Product product = completedProduct("아이폰 15");
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			productService.deleteProduct(SELLER_ID, PRODUCT_ID);

			assertThat(product.isDeleted()).isTrue();
			assertThat(product.getTradeStatus()).isEqualTo(TradeStatus.COMPLETED);
		}

		@Test
		@DisplayName("삭제할 상품이 없으면 삭제할 수 없다")
		void throwsProductNotFoundWhenDeletingMissingProduct() {
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

			assertBusinessException(
					() -> productService.deleteProduct(SELLER_ID, PRODUCT_ID),
					ErrorCode.PRODUCT_NOT_FOUND
			);
		}

		@Test
		@DisplayName("이미 삭제된 상품은 다시 삭제할 수 없다")
		void throwsDeletedProductWhenDeletingDeletedProduct() {
			Product product = deletedProduct("아이폰 15");
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.deleteProduct(SELLER_ID, PRODUCT_ID),
					ErrorCode.DELETED_PRODUCT
			);
		}

		@Test
		@DisplayName("작성자가 아니면 삭제할 수 없다")
		void throwsProductOwnerOnlyWhenDeletingByNonOwner() {
			Product product = product("아이폰 15", BigDecimal.valueOf(800000));
			given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

			assertBusinessException(
					() -> productService.deleteProduct(OTHER_MEMBER_ID, PRODUCT_ID),
					ErrorCode.PRODUCT_OWNER_ONLY
			);
		}
	}

	@Nested
	@DisplayName("접근 가능한 상품 검증")
	class ValidateAccessibleProduct {

		@Test
		@DisplayName("접근 가능한 상품이면 검증을 통과한다")
		void validatesAccessibleProduct() {
			given(productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(PRODUCT_ID)).willReturn(true);

			productService.validateAccessibleProduct(PRODUCT_ID);

			verify(productRepository).existsByIdAndDeletedAtIsNullAndHiddenFalse(PRODUCT_ID);
		}

		@Test
		@DisplayName("존재하지 않는 상품이면 접근 가능한 상품으로 인정하지 않는다")
		void throwsProductNotFoundWhenValidatingMissingProduct() {
			given(productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(PRODUCT_ID)).willReturn(false);

			assertInaccessibleProduct();
		}

		@Test
		@DisplayName("삭제된 상품이면 접근 가능한 상품으로 인정하지 않는다")
		void throwsProductNotFoundWhenValidatingDeletedProduct() {
			given(productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(PRODUCT_ID)).willReturn(false);

			assertInaccessibleProduct();
		}

		@Test
		@DisplayName("숨김 상품이면 접근 가능한 상품으로 인정하지 않는다")
		void throwsProductNotFoundWhenValidatingHiddenProduct() {
			given(productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(PRODUCT_ID)).willReturn(false);

			assertInaccessibleProduct();
		}
	}

	private Member seller() {
		return member(SELLER_ID, "seller@example.com", "판매자");
	}

	private Member member(Long id, String email, String nickname) {
		Member member = Member.createUser(email, "encodedPassword", nickname);
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	private Category category(String name) {
		return new Category(name);
	}

	private Product product(String title, BigDecimal price) {
		return Product.create(
				seller(),
				category("디지털기기"),
				title,
				"상품 설명입니다.",
				price,
				"서울 강남구"
		);
	}

	private Product hiddenProduct(String title) {
		Product product = product(title, BigDecimal.valueOf(800000));
		product.hide();
		return product;
	}

	private Product deletedProduct(String title) {
		Product product = product(title, BigDecimal.valueOf(800000));
		product.softDelete();
		return product;
	}

	private Product completedProduct(String title) {
		Product product = product(title, BigDecimal.valueOf(800000));
		product.complete();
		return product;
	}

	private ProductCreateRequest createRequest(String title, BigDecimal price) {
		return new ProductCreateRequest(
				CATEGORY_ID,
				title,
				"상태 좋은 아이폰입니다.",
				price,
				"서울 강남구"
		);
	}

	private ProductUpdateRequest updateRequest(String title, BigDecimal price) {
		return new ProductUpdateRequest(
				UPDATE_CATEGORY_ID,
				title,
				"수정된 상품 설명입니다.",
				price,
				"서울 서초구"
		);
	}

	private ProductStatusUpdateRequest statusRequest(String status) {
		return new ProductStatusUpdateRequest(status);
	}

	private void assertInvalidSearchCondition(ProductSearchRequest request) {
		assertBusinessException(
				() -> productService.searchProducts(request),
				ErrorCode.INVALID_SEARCH_CONDITION
		);

		verify(productRepository, never()).findAll(anyProductSpecification(), any(Sort.class));
	}

	private void assertInvalidTradeStatus(ThrowingCallable callable) {
		assertBusinessException(callable, ErrorCode.INVALID_TRADE_STATUS);

		verify(productRepository, never()).findById(any());
	}

	private void assertInaccessibleProduct() {
		assertBusinessException(
				() -> productService.validateAccessibleProduct(PRODUCT_ID),
				ErrorCode.PRODUCT_NOT_FOUND
		);
	}

	private void assertBusinessException(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", errorCode);
	}

	private Specification<Product> anyProductSpecification() {
		return org.mockito.ArgumentMatchers.any();
	}
}
