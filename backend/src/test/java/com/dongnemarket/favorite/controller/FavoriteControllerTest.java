package com.dongnemarket.favorite.controller;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.favorite.repository.FavoriteRepository;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 HTTP 요청으로 관심 등록 성공/실패를 검증하는 통합 테스트 (H2, MySQL/Docker 불필요).
 * 실제 JWT로 @AuthenticationPrincipal(memberId) 바인딩까지 검증한다.
 * Postman 시나리오(docs/postman/favorite-add.md)의 응답 예시는 이 테스트로 직접 확인한 값이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteControllerTest {

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
    FavoriteRepository favoriteRepository;

    private Long productId;
    private Long categoryId;
    private String token;

    @BeforeEach
    void setUp() {
        Member buyer = memberRepository.save(Member.createUser("buyer@example.com", "encoded-pw", "buyer"));
        Member seller = memberRepository.save(Member.createUser("seller@example.com", "encoded-pw", "seller"));
        // 시드된 기본 카테고리(CategoryInitializer)와 이름이 겹치지 않도록 테스트 전용 카테고리를 만든다.
        Category category = categoryRepository.save(new Category("관심테스트전용카테고리"));
        Product product = productRepository.save(
                Product.create(seller, category, "맥북 프로", "상태 좋음", 1_500_000, "서울 강남구"));

        categoryId = category.getId();
        productId = product.getId();
        token = "Bearer " + jwtTokenProvider.createAccessToken(buyer.getId(), "ROLE_USER");
    }

    @AfterEach
    void cleanUp() {
        favoriteRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
        // 시드 카테고리는 보존하고 테스트가 만든 카테고리만 제거한다.
        categoryRepository.deleteById(categoryId);
    }

    @Test
    @DisplayName("로그인 사용자가 존재하는 상품을 관심 등록하면 201과 관심 정보를 반환한다")
    void addFavorite_success() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/favorites", productId)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.productId").value(productId.intValue()))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("토큰 없이 관심 등록 요청하면 401과 UNAUTHORIZED를 반환한다")
    void addFavorite_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/favorites", productId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 관심 등록하면 404와 PRODUCT_NOT_FOUND를 반환한다")
    void addFavorite_productNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/favorites", 999_999L)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 관심 등록한 상품을 다시 등록하면 409와 FAVORITE_ALREADY_EXISTS를 반환한다")
    void addFavorite_duplicate_returns409() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/favorites", productId)
                .header("Authorization", token));

        mockMvc.perform(post("/api/products/{productId}/favorites", productId)
                        .header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("FAVORITE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("등록한 관심 상품을 취소하면 200을 반환한다")
    void removeFavorite_success() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/favorites", productId)
                        .header("Authorization", token))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/products/{productId}/favorites", productId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("등록하지 않은 상품을 취소하면 404와 FAVORITE_NOT_FOUND를 반환한다")
    void removeFavorite_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/products/{productId}/favorites", productId)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FAVORITE_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이 관심 취소 요청하면 401과 UNAUTHORIZED를 반환한다")
    void removeFavorite_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/products/{productId}/favorites", productId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
