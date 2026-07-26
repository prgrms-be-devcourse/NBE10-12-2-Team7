package com.dongnemarket.product.controller;

import java.math.BigDecimal;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.ProductImage;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.ProductImageRepository;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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

	@Autowired
	ProductImageRepository productImageRepository;

	@Autowired
	RegionRepository regionRepository;

	@Autowired
	EntityManager entityManager;

	// 지역 마스터는 RegionSeeder가 주입한다(@AfterEach에서 삭제하지 않음). 필터는 단일 regionId 기준.
	private Region regionA() {
		return regionRepository.findFirstByLevelOrderByCodeAsc(3).orElseThrow();
	}

	private Region regionB() {
		Long aId = regionA().getId();
		return regionRepository.findAll().stream()
				.filter(region -> region.getLevel() == 3 && !region.getId().equals(aId))
				.findFirst()
				.orElseThrow();
	}

	@AfterEach
	void cleanUp() {
		productImageRepository.deleteAll();
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
				  "regionId": 1
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
		Region region = regionA();
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "아이폰 15",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": 800000,
				  "regionId": %d,
				  "imageUrls": ["https://example.com/product-1.jpg"],
				  "thumbnailIndex": 0
				}
				""".formatted(category.getId(), region.getId());

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
				.andExpect(jsonPath("$.data.regionId").value(region.getId()))
				.andExpect(jsonPath("$.data.regionName").value(region.getFullName()))
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
				  "regionId": 1,
				  "imageUrls": ["https://example.com/product-1.jpg"],
				  "thumbnailIndex": 0
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
				  "regionId": 1,
				  "imageUrls": ["https://example.com/product-1.jpg"],
				  "thumbnailIndex": 0
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
				  "regionId": 1,
				  "imageUrls": ["https://example.com/product-1.jpg"],
				  "thumbnailIndex": 0
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
	@DisplayName("이미지 URL이 공백이면 INVALID_INPUT_VALUE를 반환한다")
	void returnsInvalidInputValueWhenImageUrlIsBlank() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리이미지"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "아이폰 15",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": 800000,
				  "regionId": 1,
				  "imageUrls": ["https://example.com/1.jpg", " "],
				  "thumbnailIndex": 0
				}
				""".formatted(category.getId());

		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("지역 마스터에 없는 지역으로 상품 등록 시 INVALID_INPUT_VALUE를 반환한다")
	void returnsInvalidInputValueWhenCreatingWithUnknownRegion() throws Exception {
		Member member = memberRepository.save(Member.createUser("unknown-region-seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리미등록지역"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "아이폰 15",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": 800000,
				  "regionId": 999999999,
				  "imageUrls": ["https://example.com/product-1.jpg"],
				  "thumbnailIndex": 0
				}
				""".formatted(category.getId());

		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
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
				BigDecimal.valueOf(10000),
				regionA()
		));
		Product newProduct = productRepository.save(Product.create(
				member,
				category,
				"최신 상품",
				"최신 상품 설명",
				BigDecimal.valueOf(20000),
				regionA()
		));
		Product hiddenProduct = Product.create(member, category, "숨김 상품", "숨김 상품 설명", BigDecimal.valueOf(30000), regionA());
		hiddenProduct.hide();
		productRepository.save(hiddenProduct);
		Product deletedProduct = Product.create(member, category, "삭제 상품", "삭제 상품 설명", BigDecimal.valueOf(40000), regionA());
		deletedProduct.softDelete();
		productRepository.saveAndFlush(deletedProduct);

			mockMvc.perform(get("/api/products"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value(200))
					.andExpect(jsonPath("$.data.items.length()").value(2))
					.andExpect(jsonPath("$.data.items[0].productId").value(newProduct.getId()))
					.andExpect(jsonPath("$.data.items[0].title").value("최신 상품"))
					.andExpect(jsonPath("$.data.items[0].description").doesNotExist())
					.andExpect(jsonPath("$.data.items[0].price").value(20000))
					.andExpect(jsonPath("$.data.items[0].tradeStatus").value("ON_SALE"))
					.andExpect(jsonPath("$.data.items[0].regionName").value(regionA().getFullName()))
					.andExpect(jsonPath("$.data.items[0].viewCount").value(0))
					.andExpect(jsonPath("$.data.items[0].hidden").value(false))
					.andExpect(jsonPath("$.data.items[1].productId").value(oldProduct.getId()))
					.andExpect(jsonPath("$.data.items[1].title").value("오래된 상품"))
					.andExpect(jsonPath("$.data.hasNext").value(false))
				.andExpect(jsonPath("$.data.nextCursor").doesNotExist());
			}

	@Test
	@DisplayName("상품 목록은 탈퇴·정지 판매자 상품과 거래완료 상품을 제외한다")
	void excludesInactiveSellerProductsAndCompletedProductsFromProductList() throws Exception {
		Member activeMember = memberRepository.save(Member.createUser("list-visible-active@example.com", "encodedPassword", "활성판매자"));
		Member deletedMember = Member.createUser("list-visible-deleted@example.com", "encodedPassword", "탈퇴판매자");
		deletedMember.changeStatus(MemberStatus.DELETED);
		deletedMember = memberRepository.save(deletedMember);
		Member suspendedMember = Member.createUser("list-visible-suspended@example.com", "encodedPassword", "정지판매자");
		suspendedMember.changeStatus(MemberStatus.SUSPENDED);
		suspendedMember = memberRepository.save(suspendedMember);
		Category category = categoryRepository.save(new Category("목록공개정책"));
		Product onSaleProduct = productRepository.save(Product.create(
				activeMember,
				category,
				"판매중 공개 상품",
				"판매중 공개 상품 설명",
				BigDecimal.valueOf(10000),
				regionA()
		));
		Product reservedProduct = Product.create(activeMember, category, "예약중 공개 상품", "예약중 공개 상품 설명", BigDecimal.valueOf(20000), regionA());
		reservedProduct.changeTradeStatus(TradeStatus.RESERVED);
		Product savedReservedProduct = productRepository.save(reservedProduct);
		Product completedProduct = Product.create(activeMember, category, "거래완료 상품", "거래완료 상품 설명", BigDecimal.valueOf(30000), regionA());
		completedProduct.complete();
		productRepository.save(completedProduct);
		productRepository.save(Product.create(deletedMember, category, "탈퇴 판매자 상품", "탈퇴 판매자 상품 설명", BigDecimal.valueOf(40000), regionA()));
		productRepository.saveAndFlush(Product.create(suspendedMember, category, "정지 판매자 상품", "정지 판매자 상품 설명", BigDecimal.valueOf(50000), regionA()));

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].productId").value(savedReservedProduct.getId()))
				.andExpect(jsonPath("$.data.items[1].productId").value(onSaleProduct.getId()))
				.andExpect(jsonPath("$.data.items[?(@.productId == " + completedProduct.getId() + ")]").isEmpty());
	}

		@Test
		@DisplayName("상품 목록은 커서로 다음 페이지를 이어 조회한다")
		void getsProductsWithCursorPagination() throws Exception {
			Member member = memberRepository.save(Member.createUser("cursor-list-controller@example.com", "encodedPassword", "판매자"));
			Category category = categoryRepository.save(new Category("테스트카테고리커서목록"));
			Product firstProduct = productRepository.save(Product.create(
					member,
					category,
					"첫번째 상품",
					"첫번째 상품 설명",
					BigDecimal.valueOf(10000),
					regionA()
			));
			Product secondProduct = productRepository.save(Product.create(
					member,
					category,
					"두번째 상품",
					"두번째 상품 설명",
					BigDecimal.valueOf(20000),
					regionA()
			));
			Product thirdProduct = productRepository.saveAndFlush(Product.create(
					member,
					category,
					"세번째 상품",
					"세번째 상품 설명",
					BigDecimal.valueOf(30000),
					regionA()
			));

			mockMvc.perform(get("/api/products")
							.param("size", "2"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.items.length()").value(2))
					.andExpect(jsonPath("$.data.items[0].productId").value(thirdProduct.getId()))
					.andExpect(jsonPath("$.data.items[1].productId").value(secondProduct.getId()))
					.andExpect(jsonPath("$.data.hasNext").value(true))
					.andExpect(jsonPath("$.data.nextCursor").value(secondProduct.getId()));

			mockMvc.perform(get("/api/products")
							.param("size", "2")
							.param("cursor", String.valueOf(secondProduct.getId())))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.items.length()").value(1))
					.andExpect(jsonPath("$.data.items[0].productId").value(firstProduct.getId()))
					.andExpect(jsonPath("$.data.hasNext").value(false))
					.andExpect(jsonPath("$.data.nextCursor").doesNotExist());
		}

		@Test
		@DisplayName("상품 목록은 지역 필터와 커서를 함께 적용한다")
		void getsProductsWithRegionsAndCursor() throws Exception {
			Member member = memberRepository.save(Member.createUser("cursor-region-list-controller@example.com", "encodedPassword", "판매자"));
			Category category = categoryRepository.save(new Category("테스트카테고리커서지역목록"));
			Region regionA = regionA();
			Region regionB = regionB();
			Product gangnamOldProduct = productRepository.save(Product.create(
					member,
					category,
					"강남 오래된 상품",
					"강남 오래된 상품 설명",
					BigDecimal.valueOf(10000),
					regionA
			));
			Product mapoProduct = productRepository.save(Product.create(
					member,
					category,
					"마포 상품",
					"마포 상품 설명",
					BigDecimal.valueOf(20000),
					regionB
			));
			productRepository.save(Product.create(
					member,
					category,
					"송파 상품",
					"송파 상품 설명",
					BigDecimal.valueOf(30000),
					regionB
			));
			Product gangnamNewProduct = productRepository.saveAndFlush(Product.create(
					member,
					category,
					"강남 최신 상품",
					"강남 최신 상품 설명",
					BigDecimal.valueOf(40000),
					regionA
			));

			mockMvc.perform(get("/api/products")
							.param("regionId", String.valueOf(regionA.getId()))
							.param("cursor", String.valueOf(gangnamNewProduct.getId()))
							.param("size", "2"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.items.length()").value(1))
					.andExpect(jsonPath("$.data.items[0].productId").value(gangnamOldProduct.getId()))
					.andExpect(jsonPath("$.data.hasNext").value(false))
					.andExpect(jsonPath("$.data.nextCursor").doesNotExist())
					.andExpect(jsonPath("$.data.items[?(@.productId == " + mapoProduct.getId() + ")]").isEmpty());
		}

		@Test
		@DisplayName("상품 목록 size가 0 이하이면 기본 크기로 조회한다")
		void getsProductsWithDefaultSizeWhenSizeIsNotPositive() throws Exception {
			Member member = memberRepository.save(Member.createUser("cursor-default-size-controller@example.com", "encodedPassword", "판매자"));
			Category category = categoryRepository.save(new Category("테스트카테고리기본크기"));
			for (int i = 1; i <= 31; i++) {
				productRepository.save(Product.create(
						member,
						category,
						"상품 " + i,
						"상품 설명 " + i,
						BigDecimal.valueOf(i * 1000L),
						regionA()
				));
			}
			productRepository.flush();

			mockMvc.perform(get("/api/products")
							.param("size", "0"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.items.length()").value(30))
					.andExpect(jsonPath("$.data.hasNext").value(true));
		}

		@Test
		@DisplayName("상품 목록 size가 100보다 크면 최대 100개로 제한한다")
		void getsProductsWithMaxSizeWhenSizeIsOverLimit() throws Exception {
			Member member = memberRepository.save(Member.createUser("cursor-max-size-controller@example.com", "encodedPassword", "판매자"));
			Category category = categoryRepository.save(new Category("테스트카테고리최대크기"));
			for (int i = 1; i <= 101; i++) {
				productRepository.save(Product.create(
						member,
						category,
						"상품 " + i,
						"상품 설명 " + i,
						BigDecimal.valueOf(i * 1000L),
						regionA()
				));
			}
			productRepository.flush();

			mockMvc.perform(get("/api/products")
							.param("size", "101"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.items.length()").value(100))
					.andExpect(jsonPath("$.data.hasNext").value(true));
		}

	@Test
	@DisplayName("상품 검색은 인증 없이 조건에 맞는 상품을 최신 등록순으로 조회한다")
	void searchesProductsWithoutAuthentication() throws Exception {
		Member member = memberRepository.save(Member.createUser("search-controller@example.com", "encodedPassword", "판매자"));
		Category targetCategory = categoryRepository.save(new Category("검색카테고리1"));
		Category otherCategory = categoryRepository.save(new Category("검색카테고리2"));
		Product oldProduct = Product.create(
				member,
				targetCategory,
				"맥북 에어",
				"가벼운 맥북입니다.",
				BigDecimal.valueOf(1000000),
				regionA()
		);
		oldProduct.changeTradeStatus(com.dongnemarket.product.entity.TradeStatus.RESERVED);
		Product savedOldProduct = productRepository.save(oldProduct);
		Product newProduct = Product.create(
				member,
				targetCategory,
				"맥북 프로",
				"성능 좋은 맥북입니다.",
				BigDecimal.valueOf(1500000),
				regionA()
		);
		newProduct.changeTradeStatus(com.dongnemarket.product.entity.TradeStatus.RESERVED);
		Product savedNewProduct = productRepository.save(newProduct);
		productRepository.save(Product.create(member, otherCategory, "맥북 관련 책", "맥북 설명서입니다.", BigDecimal.valueOf(20000), regionA()));
		Product hiddenProduct = Product.create(member, targetCategory, "숨김 맥북", "숨김 상품입니다.", BigDecimal.valueOf(1200000), regionA());
		hiddenProduct.hide();
		productRepository.save(hiddenProduct);
		Product deletedProduct = Product.create(member, targetCategory, "삭제 맥북", "삭제 상품입니다.", BigDecimal.valueOf(1300000), regionA());
		deletedProduct.softDelete();
		productRepository.saveAndFlush(deletedProduct);

		mockMvc.perform(get("/api/products/search")
						.param("keyword", "맥북")
						.param("categoryId", targetCategory.getId().toString())
						.param("minPrice", "900000")
						.param("maxPrice", "1600000")
						.param("tradeStatus", "RESERVED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].productId").value(savedNewProduct.getId()))
				.andExpect(jsonPath("$.data[0].title").value("맥북 프로"))
				.andExpect(jsonPath("$.data[0].tradeStatus").value("RESERVED"))
				.andExpect(jsonPath("$.data[0].hidden").value(false))
				.andExpect(jsonPath("$.data[1].productId").value(savedOldProduct.getId()))
				.andExpect(jsonPath("$.data[1].title").value("맥북 에어"));
	}

	@Test
	@DisplayName("상품 검색은 키워드와 지역 필터를 함께 적용한다")
	void searchesProductsWithKeywordAndRegions() throws Exception {
		Member member = memberRepository.save(Member.createUser("search-region-controller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("검색지역카테고리"));
		Region regionA = regionA();
		Region regionB = regionB();
		Product matchedProduct = productRepository.save(Product.create(
				member,
				category,
				"맥북 프로",
				"상태 좋은 노트북입니다.",
				BigDecimal.valueOf(1500000),
				regionA
		));
		productRepository.save(Product.create(
				member,
				category,
				"맥북 에어",
				"가벼운 노트북입니다.",
				BigDecimal.valueOf(1000000),
				regionB
		));
		productRepository.saveAndFlush(Product.create(
				member,
				category,
				"아이패드",
				"상태 좋은 태블릿입니다.",
				BigDecimal.valueOf(700000),
				regionA
		));

		mockMvc.perform(get("/api/products/search")
						.param("keyword", "맥북")
						.param("regionId", String.valueOf(regionA.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].productId").value(matchedProduct.getId()));
	}

	@Test
	@DisplayName("상품 검색은 탈퇴·정지 판매자 상품과 거래완료 상품을 제외한다")
	void excludesInactiveSellerProductsAndCompletedProductsFromSearch() throws Exception {
		Member activeMember = memberRepository.save(Member.createUser("search-visible-active-controller@example.com", "encodedPassword", "활성검색판매자"));
		Member deletedMember = Member.createUser("search-visible-deleted-controller@example.com", "encodedPassword", "탈퇴검색판매자");
		deletedMember.changeStatus(MemberStatus.DELETED);
		deletedMember = memberRepository.save(deletedMember);
		Member suspendedMember = Member.createUser("search-visible-suspended-controller@example.com", "encodedPassword", "정지검색판매자");
		suspendedMember.changeStatus(MemberStatus.SUSPENDED);
		suspendedMember = memberRepository.save(suspendedMember);
		Category category = categoryRepository.save(new Category("검색공개정책"));
		Product visibleProduct = productRepository.save(Product.create(
				activeMember,
				category,
				"정책 맥북",
				"정책 검색 상품입니다.",
				BigDecimal.valueOf(1000000),
				regionA()
		));
		Product completedProduct = Product.create(activeMember, category, "정책 완료 맥북", "거래완료 검색 상품입니다.", BigDecimal.valueOf(900000), regionA());
		completedProduct.complete();
		productRepository.save(completedProduct);
		productRepository.save(Product.create(deletedMember, category, "정책 탈퇴 맥북", "탈퇴 판매자 검색 상품입니다.", BigDecimal.valueOf(800000), regionA()));
		productRepository.saveAndFlush(Product.create(suspendedMember, category, "정책 정지 맥북", "정지 판매자 검색 상품입니다.", BigDecimal.valueOf(700000), regionA()));

		mockMvc.perform(get("/api/products/search")
						.param("keyword", "정책")
						.param("regionId", String.valueOf(regionA().getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].productId").value(visibleProduct.getId()))
				.andExpect(jsonPath("$.data[0].title").value("정책 맥북"));
	}

	@Test
	@DisplayName("상품 검색은 거래완료 상태를 요청하면 빈 목록을 반환한다")
	void returnsEmptyListWhenSearchingCompletedProducts() throws Exception {
		Member member = memberRepository.save(Member.createUser("search-completed-controller@example.com", "encodedPassword", "완료검색판매자"));
		Category category = categoryRepository.save(new Category("검색거래완료"));
		Product completedProduct = Product.create(
				member,
				category,
				"거래완료 맥북",
				"거래완료 검색 상품입니다.",
				BigDecimal.valueOf(1000000),
				regionA()
		);
		completedProduct.complete();
		productRepository.saveAndFlush(completedProduct);

		mockMvc.perform(get("/api/products/search")
						.param("keyword", "맥북")
						.param("tradeStatus", "COMPLETED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("상품 검색 결과가 없으면 빈 목록을 반환한다")
	void returnsEmptyListWhenSearchingProductsWithoutResult() throws Exception {
		mockMvc.perform(get("/api/products/search")
						.param("keyword", "없는상품"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("상품 검색 가격 조건이 잘못되면 INVALID_SEARCH_CONDITION을 반환한다")
	void returnsInvalidSearchConditionWhenSearchingWithInvalidPriceRange() throws Exception {
		mockMvc.perform(get("/api/products/search")
						.param("minPrice", "20000")
						.param("maxPrice", "10000"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_SEARCH_CONDITION"));
	}

	@Test
	@DisplayName("상품 검색 거래 상태가 잘못되면 INVALID_TRADE_STATUS를 반환한다")
	void returnsInvalidTradeStatusWhenSearchingWithInvalidTradeStatus() throws Exception {
		mockMvc.perform(get("/api/products/search")
						.param("tradeStatus", "INVALID"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_TRADE_STATUS"));
	}

	@Test
	@DisplayName("상품 검색 거래 상태가 공백이면 INVALID_TRADE_STATUS를 반환한다")
	void returnsInvalidTradeStatusWhenSearchingWithBlankTradeStatus() throws Exception {
		mockMvc.perform(get("/api/products/search")
						.param("tradeStatus", " "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_TRADE_STATUS"));
	}

	@Test
	@DisplayName("인증된 사용자는 내 상품 목록을 최신 등록순으로 조회하고 숨김 상품도 확인할 수 있다")
	void getsMyProductsWithAuthentication() throws Exception {
		Member member = memberRepository.save(Member.createUser("my-products@example.com", "encodedPassword", "판매자"));
		Member otherMember = memberRepository.save(Member.createUser("other-products@example.com", "encodedPassword", "다른판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리5"));
		Product oldProduct = productRepository.save(Product.create(
				member,
				category,
				"오래된 내 상품",
				"오래된 내 상품 설명",
				BigDecimal.valueOf(10000),
				regionA()
		));
		Product hiddenProduct = Product.create(member, category, "숨김 내 상품", "숨김 내 상품 설명", BigDecimal.valueOf(20000), regionA());
		hiddenProduct.hide();
		Product savedHiddenProduct = productRepository.save(hiddenProduct);
		productRepository.save(Product.create(
				otherMember,
				category,
				"다른 회원 상품",
				"다른 회원 상품 설명",
				BigDecimal.valueOf(30000),
				regionA()
		));
		Product deletedProduct = Product.create(member, category, "삭제 내 상품", "삭제 내 상품 설명", BigDecimal.valueOf(40000), regionA());
		deletedProduct.softDelete();
		productRepository.saveAndFlush(deletedProduct);
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());

		mockMvc.perform(get("/api/products/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].productId").value(savedHiddenProduct.getId()))
				.andExpect(jsonPath("$.data[0].memberId").value(member.getId()))
				.andExpect(jsonPath("$.data[0].categoryId").value(category.getId()))
				.andExpect(jsonPath("$.data[0].title").value("숨김 내 상품"))
				.andExpect(jsonPath("$.data[0].price").value(20000))
				.andExpect(jsonPath("$.data[0].tradeStatus").value("ON_SALE"))
				.andExpect(jsonPath("$.data[0].regionName").value(regionA().getFullName()))
				.andExpect(jsonPath("$.data[0].viewCount").value(0))
				.andExpect(jsonPath("$.data[0].hidden").value(true))
				.andExpect(jsonPath("$.data[1].productId").value(oldProduct.getId()))
				.andExpect(jsonPath("$.data[1].title").value("오래된 내 상품"))
				.andExpect(jsonPath("$.data[1].hidden").value(false));
	}

	@Test
	@DisplayName("내 상품이 없으면 빈 목록을 반환한다")
	void getsEmptyMyProductsWithAuthentication() throws Exception {
		Member member = memberRepository.save(Member.createUser("empty-products@example.com", "encodedPassword", "판매자"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());

		mockMvc.perform(get("/api/products/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("인증 없이 내 상품 목록 조회 요청 시 401을 반환한다")
	void returnsUnauthorizedWhenGettingMyProductsWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/products/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("상품 상세는 인증 없이 조회할 수 있고 조회수가 1 증가한다")
	void getsProductDetailWithoutAuthentication() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리6"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				BigDecimal.valueOf(800000),
				regionA()
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
				.andExpect(jsonPath("$.data.regionName").value(regionA().getFullName()))
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
		Category category = categoryRepository.save(new Category("테스트카테고리7"));
		Product product = Product.create(member, category, "삭제 상품", "삭제 상품 설명", BigDecimal.valueOf(40000), regionA());
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
		Category category = categoryRepository.save(new Category("테스트카테고리8"));
		Product product = Product.create(member, category, "숨김 상품", "숨김 상품 설명", BigDecimal.valueOf(30000), regionA());
		product.hide();
		Product savedProduct = productRepository.saveAndFlush(product);

		mockMvc.perform(get("/api/products/{productId}", savedProduct.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("HIDDEN_PRODUCT"));
	}

	@Test
	@DisplayName("탈퇴 판매자 상품 상세 조회 시 PRODUCT_NOT_FOUND를 반환한다")
	void returnsProductNotFoundWhenSellerIsDeleted() throws Exception {
		Member member = Member.createUser("detail-deleted-seller@example.com", "encodedPassword", "탈퇴판매자");
		member.changeStatus(MemberStatus.DELETED);
		member = memberRepository.save(member);
		Category category = categoryRepository.save(new Category("상세탈퇴판매자"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"탈퇴 판매자 상품",
				"탈퇴 판매자 상품 설명",
				BigDecimal.valueOf(10000),
				regionA()
		));

		mockMvc.perform(get("/api/products/{productId}", product.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	@DisplayName("정지 판매자 상품 상세 조회 시 PRODUCT_NOT_FOUND를 반환한다")
	void returnsProductNotFoundWhenSellerIsSuspended() throws Exception {
		Member member = Member.createUser("detail-suspended-seller@example.com", "encodedPassword", "정지판매자");
		member.changeStatus(MemberStatus.SUSPENDED);
		member = memberRepository.save(member);
		Category category = categoryRepository.save(new Category("상세정지판매자"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"정지 판매자 상품",
				"정지 판매자 상품 설명",
				BigDecimal.valueOf(10000),
				regionA()
		));

		mockMvc.perform(get("/api/products/{productId}", product.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	@DisplayName("거래완료 상품 상세 조회 시 PRODUCT_NOT_FOUND를 반환한다")
	void returnsProductNotFoundWhenProductIsCompleted() throws Exception {
		Member member = memberRepository.save(Member.createUser("detail-completed-seller@example.com", "encodedPassword", "완료판매자"));
		Category category = categoryRepository.save(new Category("상세거래완료"));
		Product product = Product.create(
				member,
				category,
				"거래완료 상품",
				"거래완료 상품 설명",
				BigDecimal.valueOf(10000),
				regionA()
		);
		product.complete();
		Product savedProduct = productRepository.saveAndFlush(product);

		mockMvc.perform(get("/api/products/{productId}", savedProduct.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	@DisplayName("작성자는 상품 정보를 수정할 수 있다")
	void updatesProductByOwner() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category oldCategory = categoryRepository.save(new Category("테스트카테고리9"));
		Category newCategory = categoryRepository.save(new Category("테스트카테고리10"));
		Region region = regionA();
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				oldCategory,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				BigDecimal.valueOf(800000),
				region
		));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "맥북 프로",
				  "description": "수정된 상품 설명입니다.",
				  "price": 1500000,
				  "regionId": %d,
				  "imageUrls": ["https://example.com/update-1.jpg"],
				  "thumbnailIndex": 0
				}
				""".formatted(newCategory.getId(), region.getId());

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
				.andExpect(jsonPath("$.data.regionName").value(region.getFullName()));
	}

	@Test
	@DisplayName("상품 수정 시 대표 이미지를 변경하면 DB의 thumbnailUrl도 변경된다")
	void updatesThumbnailUrlInDatabaseWhenReselectingRepresentativeImage() throws Exception {
		Member member = memberRepository.save(Member.createUser("thumbnail-update@example.com", "encodedPassword", "대표변경판매자"));
		Category category = categoryRepository.save(new Category("대표이미지수정"));
		Region region = regionA();
		Product product = Product.create(
				member,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				BigDecimal.valueOf(800000),
				region
		);
		product.changeThumbnailUrl("https://example.com/old-1.jpg");
		Product savedProduct = productRepository.saveAndFlush(product);
		productImageRepository.save(ProductImage.create(savedProduct, "https://example.com/old-1.jpg", 0, true));
		productImageRepository.saveAndFlush(ProductImage.create(savedProduct, "https://example.com/old-2.jpg", 1, false));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "아이폰 15",
				  "description": "상태 좋은 아이폰입니다.",
				  "price": 800000,
				  "regionId": %d,
				  "imageUrls": ["https://example.com/old-1.jpg", "https://example.com/old-2.jpg"],
				  "thumbnailIndex": 1
				}
				""".formatted(category.getId(), region.getId());

		mockMvc.perform(patch("/api/products/{productId}", savedProduct.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.thumbnailUrl").value("https://example.com/old-2.jpg"));
		productRepository.flush();
		entityManager.clear();

		Product foundProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
		assertThat(foundProduct.getThumbnailUrl()).isEqualTo("https://example.com/old-2.jpg");
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
				  "regionId": 1,
				  "imageUrls": ["https://example.com/update-1.jpg"],
				  "thumbnailIndex": 0
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
		Category category = categoryRepository.save(new Category("테스트카테고리11"));
		Product product = productRepository.saveAndFlush(Product.create(
				owner,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				BigDecimal.valueOf(800000),
				regionA()
		));
		String token = jwtTokenProvider.createAccessToken(other.getId(), other.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "맥북 프로",
				  "description": "수정된 상품 설명입니다.",
				  "price": 1500000,
				  "regionId": 1,
				  "imageUrls": ["https://example.com/update-1.jpg"],
				  "thumbnailIndex": 0
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
		Category category = categoryRepository.save(new Category("테스트카테고리12"));
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", BigDecimal.valueOf(800000), regionA());
		product.complete();
		Product savedProduct = productRepository.saveAndFlush(product);
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "categoryId": %d,
				  "title": "맥북 프로",
				  "description": "수정된 상품 설명입니다.",
				  "price": 1500000,
				  "regionId": 1,
				  "imageUrls": ["https://example.com/update-1.jpg"],
				  "thumbnailIndex": 0
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
	@DisplayName("작성자는 상품 거래 상태를 변경할 수 있다")
	void updatesProductStatusByOwner() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller-status@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리13"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				BigDecimal.valueOf(800000),
				regionA()
		));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "tradeStatus": "RESERVED"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", product.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.productId").value(product.getId()))
				.andExpect(jsonPath("$.data.tradeStatus").value("RESERVED"));
	}

	@Test
	@DisplayName("숨김 상품도 작성자라면 상품 거래 상태를 변경할 수 있다")
	void updatesHiddenProductStatusByOwner() throws Exception {
		Member member = memberRepository.save(Member.createUser("hidden-status@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리14"));
		Product product = Product.create(member, category, "숨김 상품", "숨김 상품 설명", BigDecimal.valueOf(30000), regionA());
		product.hide();
		Product savedProduct = productRepository.saveAndFlush(product);
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "tradeStatus": "RESERVED"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", savedProduct.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tradeStatus").value("RESERVED"))
				.andExpect(jsonPath("$.data.hidden").value(true));
	}

	@Test
	@DisplayName("거래완료 상품에 거래완료 상태를 다시 요청하면 현재 상태를 그대로 반환한다")
	void keepsCompletedProductStatusWhenRequestingCompletedAgain() throws Exception {
		Member member = memberRepository.save(Member.createUser("same-completed-status@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리15"));
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", BigDecimal.valueOf(800000), regionA());
		product.complete();
		Product savedProduct = productRepository.saveAndFlush(product);
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "tradeStatus": "COMPLETED"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", savedProduct.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tradeStatus").value("COMPLETED"));
	}

	@Test
	@DisplayName("인증 없이 상품 거래 상태 변경 요청 시 401을 반환한다")
	void returnsUnauthorizedWhenUpdatingStatusWithoutAuthentication() throws Exception {
		String body = """
				{
				  "tradeStatus": "RESERVED"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("작성자가 아니면 상품 거래 상태 변경 시 PRODUCT_OWNER_ONLY를 반환한다")
	void returnsProductOwnerOnlyWhenUpdatingStatusByNonOwner() throws Exception {
		Member owner = memberRepository.save(Member.createUser("owner-status@example.com", "encodedPassword", "작성자"));
		Member other = memberRepository.save(Member.createUser("other-status@example.com", "encodedPassword", "다른사용자"));
		Category category = categoryRepository.save(new Category("테스트카테고리16"));
		Product product = productRepository.saveAndFlush(Product.create(
				owner,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				BigDecimal.valueOf(800000),
				regionA()
		));
		String token = jwtTokenProvider.createAccessToken(other.getId(), other.getRole().name());
		String body = """
				{
				  "tradeStatus": "RESERVED"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", product.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("PRODUCT_OWNER_ONLY"));
	}

	@Test
	@DisplayName("거래완료 상품을 다른 거래 상태로 변경하면 CANNOT_CHANGE_COMPLETED_PRODUCT를 반환한다")
	void returnsCannotChangeCompletedProductWhenUpdatingCompletedStatus() throws Exception {
		Member member = memberRepository.save(Member.createUser("completed-status@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리17"));
		Product product = Product.create(member, category, "아이폰 15", "상태 좋은 아이폰입니다.", BigDecimal.valueOf(800000), regionA());
		product.complete();
		Product savedProduct = productRepository.saveAndFlush(product);
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "tradeStatus": "ON_SALE"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", savedProduct.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CANNOT_CHANGE_COMPLETED_PRODUCT"));
	}

	@Test
	@DisplayName("삭제된 상품 거래 상태 변경 시 DELETED_PRODUCT를 반환한다")
	void returnsDeletedProductWhenUpdatingStatusOfDeletedProduct() throws Exception {
		Member member = memberRepository.save(Member.createUser("deleted-status@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리18"));
		Product product = Product.create(member, category, "삭제 상품", "삭제 상품 설명", BigDecimal.valueOf(40000), regionA());
		product.softDelete();
		Product savedProduct = productRepository.saveAndFlush(product);
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "tradeStatus": "RESERVED"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", savedProduct.getId())
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("DELETED_PRODUCT"));
	}

	@Test
	@DisplayName("유효하지 않은 거래 상태로 변경하면 INVALID_TRADE_STATUS를 반환한다")
	void returnsInvalidTradeStatusWhenUpdatingWithInvalidStatus() throws Exception {
		Member member = memberRepository.save(Member.createUser("invalid-status@example.com", "encodedPassword", "판매자"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "tradeStatus": "INVALID"
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", 1L)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_TRADE_STATUS"));
	}

	@Test
	@DisplayName("비어 있는 거래 상태로 변경하면 INVALID_TRADE_STATUS를 반환한다")
	void returnsInvalidTradeStatusWhenUpdatingWithBlankStatus() throws Exception {
		Member member = memberRepository.save(Member.createUser("blank-status@example.com", "encodedPassword", "판매자"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "tradeStatus": " "
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", 1L)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_TRADE_STATUS"));
	}

	@Test
	@DisplayName("null 거래 상태로 변경하면 INVALID_TRADE_STATUS를 반환한다")
	void returnsInvalidTradeStatusWhenUpdatingWithNullStatus() throws Exception {
		Member member = memberRepository.save(Member.createUser("null-status@example.com", "encodedPassword", "판매자"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				  "tradeStatus": null
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", 1L)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_TRADE_STATUS"));
	}

	@Test
	@DisplayName("거래 상태 필드가 없으면 INVALID_TRADE_STATUS를 반환한다")
	void returnsInvalidTradeStatusWhenUpdatingWithoutStatusField() throws Exception {
		Member member = memberRepository.save(Member.createUser("missing-status@example.com", "encodedPassword", "판매자"));
		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		String body = """
				{
				}
				""";

		mockMvc.perform(patch("/api/products/{productId}/status", 1L)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_TRADE_STATUS"));
	}

	@Test
	@DisplayName("작성자는 상품을 삭제할 수 있다")
	void deletesProductByOwner() throws Exception {
		Member member = memberRepository.save(Member.createUser("seller-delete@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("테스트카테고리19"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				BigDecimal.valueOf(800000),
				regionA()
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
		Category category = categoryRepository.save(new Category("테스트카테고리20"));
		Product product = productRepository.saveAndFlush(Product.create(
				owner,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				BigDecimal.valueOf(800000),
				regionA()
		));
		String token = jwtTokenProvider.createAccessToken(other.getId(), other.getRole().name());

		mockMvc.perform(delete("/api/products/{productId}", product.getId())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("PRODUCT_OWNER_ONLY"));
	}
}
