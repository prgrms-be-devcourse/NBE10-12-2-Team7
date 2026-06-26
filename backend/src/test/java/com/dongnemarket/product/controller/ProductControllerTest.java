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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
