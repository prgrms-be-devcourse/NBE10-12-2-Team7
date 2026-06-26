package com.dongnemarket.product.controller;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	ProductRepository productRepository;

	@AfterEach
	void cleanUp() {
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증 없이 상품 등록 요청 시 401을 반환한다")
	void returnsUnauthorizedWithoutAuthentication() throws Exception {
		String body = """
				{
				  "categoryId": 1,
				  "title": "아이폰 15",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": 800000,
				  "region": "서울 강남구"
				}
				""";

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("인증된 사용자는 상품을 등록할 수 있다")
	void createsProductWithAuthentication() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리1"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "아이폰 15",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": 800000,
				  "region": "서울 강남구"
				}
				""".formatted(category.getId());

		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(201))
				.andExpect(jsonPath("$.data.productId").exists())
				.andExpect(jsonPath("$.data.memberId").value(member.getId()))
				.andExpect(jsonPath("$.data.categoryId").value(category.getId()))
				.andExpect(jsonPath("$.data.title").value("아이폰 15"))
				.andExpect(jsonPath("$.data.description").value("상태 좋은 아이폰입니다."))
				.andExpect(jsonPath("$.data.price").value(800000))
				.andExpect(jsonPath("$.data.tradeStatus").value("ON_SALE"))
				.andExpect(jsonPath("$.data.region").value("서울 강남구"))
				.andExpect(jsonPath("$.data.viewCount").value(0))
				.andExpect(jsonPath("$.data.hidden").value(false));
	}

	@Test
	@DisplayName("존재하지 않는 카테고리로 상품 등록 시 CATEGORY_NOT_FOUND를 반환한다")
	void returnsCategoryNotFoundWhenCategoryDoesNotExist() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": 999,
				  "title": "아이폰 15",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": 800000,
				  "region": "서울 강남구"
				}
				""";

		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("CATEGORY_NOT_FOUND"));
	}

	@Test
	@DisplayName("제목이 비어 있으면 INVALID_PRODUCT_TITLE을 반환한다")
	void returnsInvalidProductTitleWhenTitleIsBlank() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리2"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": " ",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": 800000,
				  "region": "서울 강남구"
				}
				""".formatted(category.getId());

		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PRODUCT_TITLE"));
	}

	@Test
	@DisplayName("가격이 음수이면 INVALID_PRODUCT_PRICE를 반환한다")
	void returnsInvalidProductPriceWhenPriceIsNegative() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리3"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "아이폰 15",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": -1,
				  "region": "서울 강남구"
				}
				""".formatted(category.getId());

		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PRODUCT_PRICE"));
	}

	@Test
	@DisplayName("상품 목록은 인증 없이 최신 등록순으로 조회하고 숨김·삭제 상품은 제외한다")
	void getsProductsInLatestOrderWithoutAuthentication() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리4"));
		Product oldProduct = productRepository.save(Product.create(
				member,
				category,
				"오래된 상품",
				"오래된 상품 설명",
				10000,
				"서울 강남구"
		));
		Product newProduct = productRepository.save(Product.create(
				member,
				category,
				"최신 상품",
				"최신 상품 설명",
				20000,
				"서울 서초구"
		));
		Product hiddenProduct = Product.create(member, category, "숨김 상품", "숨김 상품 설명", 30000, "서울 송파구");
		hiddenProduct.hide();
		productRepository.save(hiddenProduct);
		Product deletedProduct = Product.create(member, category, "삭제 상품", "삭제 상품 설명", 40000, "서울 마포구");
		deletedProduct.softDelete();
		productRepository.saveAndFlush(deletedProduct);

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].productId").value(newProduct.getId()))
				.andExpect(jsonPath("$.data[0].title").value("최신 상품"))
				.andExpect(jsonPath("$.data[0].description").doesNotExist())
				.andExpect(jsonPath("$.data[0].price").value(20000))
				.andExpect(jsonPath("$.data[0].tradeStatus").value("ON_SALE"))
				.andExpect(jsonPath("$.data[0].region").value("서울 서초구"))
				.andExpect(jsonPath("$.data[0].viewCount").value(0))
				.andExpect(jsonPath("$.data[0].hidden").value(false))
				.andExpect(jsonPath("$.data[1].productId").value(oldProduct.getId()))
				.andExpect(jsonPath("$.data[1].title").value("오래된 상품"));
	}

	@Test
	@DisplayName("상품 상세는 인증 없이 조회할 수 있고 조회수가 1 증가한다")
	void getsProductDetailWithoutAuthentication() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리5"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				800000,
				"서울 강남구"
		));

		mockMvc.perform(get("/api/products/{productId}", product.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.productId").value(product.getId()))
				.andExpect(jsonPath("$.data.memberId").value(member.getId()))
				.andExpect(jsonPath("$.data.categoryId").value(category.getId()))
				.andExpect(jsonPath("$.data.title").value("아이폰 15"))
				.andExpect(jsonPath("$.data.description").value("상태 좋은 아이폰입니다."))
				.andExpect(jsonPath("$.data.price").value(800000))
				.andExpect(jsonPath("$.data.tradeStatus").value("ON_SALE"))
				.andExpect(jsonPath("$.data.region").value("서울 강남구"))
				.andExpect(jsonPath("$.data.viewCount").value(1))
				.andExpect(jsonPath("$.data.hidden").value(false));
	}

	@Test
	@DisplayName("존재하지 않는 상품 상세 조회 시 PRODUCT_NOT_FOUND를 반환한다")
	void returnsProductNotFoundWhenProductDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/products/{productId}", 999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	@DisplayName("삭제된 상품 상세 조회 시 DELETED_PRODUCT를 반환한다")
	void returnsDeletedProductWhenProductIsDeleted() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리6"));
		Product product = Product.create(member, category, "삭제 상품", "삭제 상품 설명", 40000, "서울 마포구");
		product.softDelete();
		Product savedProduct = productRepository.saveAndFlush(product);

		mockMvc.perform(get("/api/products/{productId}", savedProduct.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("DELETED_PRODUCT"));
	}

	@Test
	@DisplayName("숨김 상품 상세 조회 시 HIDDEN_PRODUCT를 반환한다")
	void returnsHiddenProductWhenProductIsHidden() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리7"));
		Product product = Product.create(member, category, "숨김 상품", "숨김 상품 설명", 30000, "서울 송파구");
		product.hide();
		Product savedProduct = productRepository.saveAndFlush(product);

		mockMvc.perform(get("/api/products/{productId}", savedProduct.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("HIDDEN_PRODUCT"));
	}

	@Test
	@DisplayName("작성자는 상품 정보를 수정할 수 있다")
	void updatesProductByOwner() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category oldCategory = categoryRepository.save(new Category("테스트카테고리8"));
		Category newCategory = categoryRepository.save(new Category("테스트카테고리9"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				oldCategory,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				800000,
				"서울 강남구"
		));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "맥북 프로",
				  "description": "수정된 상품 설명입니다.",
				  "price": 1500000,
				  "region": "서울 서초구"
				}
				""".formatted(newCategory.getId());

		mockMvc.perform(patch("/api/products/{productId}", product.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.productId").value(product.getId()))
				.andExpect(jsonPath("$.data.categoryId").value(newCategory.getId()))
				.andExpect(jsonPath("$.data.title").value("맥북 프로"))
				.andExpect(jsonPath("$.data.description").value("수정된 상품 설명입니다."))
				.andExpect(jsonPath("$.data.price").value(1500000))
				.andExpect(jsonPath("$.data.region").value("서울 서초구"));
	}

	@Test
	@DisplayName("인증 없이 상품 수정 요청 시 401을 반환한다")
	void returnsUnauthorizedWhenUpdatingWithoutAuthentication() throws Exception {
		String body = """
				{
				  "categoryId": 1,
				  "title": "맥북 프로",
				  "description": "수정된 상품 설명입니다.",
				  "price": 1500000,
				  "region": "서울 서초구"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("작성자가 아니면 상품 수정 시 PRODUCT_OWNER_ONLY를 반환한다")
	void returnsProductOwnerOnlyWhenUpdatingByNonOwner() throws Exception {
		Member owner = memberRepository.save(Member.createUser("owner@example.com", "encodedPassword", "작성자"));
		Member other = memberRepository.save(Member.createUser("other@example.com", "encodedPassword", "다른사용자"));
		Category category = categoryRepository.save(new Category("테스트카테고리10"));
		Product product = productRepository.saveAndFlush(Product.create(
				owner,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				800000,
				"서울 강남구"
		));
		String token = jwtTokenProvider.createAccessToken(other.getId(), other.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "맥북 프로",
				  "description": "수정된 상품 설명입니다.",
				  "price": 1500000,
				  "region": "서울 서초구"
				}
				""".formatted(category.getId());

		mockMvc.perform(patch("/api/products/{productId}", product.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("PRODUCT_OWNER_ONLY"));
	}

	@Test
	@DisplayName("거래완료 상품 수정 시 CANNOT_UPDATE_COMPLETED_PRODUCT를 반환한다")
	void returnsCannotUpdateCompletedProductWhenUpdatingCompletedProduct() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리11"));
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", 800000, "서울 강남구");
		product.complete();
		Product savedProduct = productRepository.saveAndFlush(product);
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "맥북 프로",
				  "description": "수정된 상품 설명입니다.",
				  "price": 1500000,
				  "region": "서울 서초구"
				}
				""".formatted(category.getId());

		mockMvc.perform(patch("/api/products/{productId}", savedProduct.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CANNOT_UPDATE_COMPLETED_PRODUCT"));
	}

	@Test
	@DisplayName("작성자는 상품을 삭제할 수 있다")
	void deletesProductByOwner() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller-delete@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리12"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				800000,
				"서울 강남구"
		));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());

		mockMvc.perform(delete("/api/products/{productId}", product.getId())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

		mockMvc.perform(get("/api/products/{productId}", product.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("DELETED_PRODUCT"));
	}

	@Test
	@DisplayName("인증 없이 상품 삭제 요청 시 401을 반환한다")
	void returnsUnauthorizedWhenDeletingWithoutAuthentication() throws Exception {
		mockMvc.perform(delete("/api/products/{productId}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("작성자가 아니면 상품 삭제 시 PRODUCT_OWNER_ONLY를 반환한다")
	void returnsProductOwnerOnlyWhenDeletingByNonOwner() throws Exception {
		Member owner = memberRepository.save(Member.createUser("owner-delete@example.com", "encodedPassword", "작성자"));
		Member other = memberRepository.save(Member.createUser("other-delete@example.com", "encodedPassword", "다른사용자"));
		Category category = categoryRepository.save(new Category("테스트카테고리13"));
		Product product = productRepository.saveAndFlush(Product.create(
				owner,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				800000,
				"서울 강남구"
		));
		String token = jwtTokenProvider.createAccessToken(other.getId(), other.getRole().name());

		mockMvc.perform(delete("/api/products/{productId}", product.getId())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("PRODUCT_OWNER_ONLY"));
	}
}
