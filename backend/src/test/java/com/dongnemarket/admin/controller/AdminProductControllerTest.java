package com.dongnemarket.admin.controller;

import java.math.BigDecimal;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 상품 조회 API(/api/admin/products) 통합 테스트 (H2, Docker 불필요).
 * 토큰은 JwtTokenProvider 로 직접 발급한다 — 권한은 토큰의 role 로 결정되므로
 * createAccessToken(id, "ROLE_ADMIN") 한 줄로 관리자 권한을 부여한다(시드 불필요).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductControllerTest {

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
        // categories 는 CategoryInitializer 가 시드한 공유 데이터라 지우지 않는다.
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private String adminToken() {
        Member admin = memberRepository.save(Member.createUser("admin@example.com", "encodedPassword", "관리자"));
        return jwtTokenProvider.createAccessToken(admin.getId(), "ROLE_ADMIN");
    }

    private String userToken() {
        Member user = memberRepository.save(Member.createUser("user@example.com", "encodedPassword", "일반회원"));
        return jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
    }

    private Member savedSeller() {
        return memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
    }

    private Category savedCategory() {
        // CategoryInitializer 가 시드한 카테고리를 재사용한다(유니크 이름 충돌·시드 삭제 방지).
        return categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(new Category("어드민상품테스트카테고리")));
    }

    // ===== GET /api/admin/products =====

    @Test
    @DisplayName("관리자가 상품 목록을 조회하면 200과 숨김 포함 전체 상품을 반환한다")
    void getProducts_asAdmin_success() throws Exception {
        Member seller = savedSeller();
        Category category = savedCategory();
        productRepository.save(Product.create(seller, category, "아이폰 15", "설명1", BigDecimal.valueOf(800000), "서울 강남구"));
        Product hidden = Product.create(seller, category, "숨김 상품", "설명2", BigDecimal.valueOf(5000), "서울 서초구");
        hidden.hide();
        productRepository.save(hidden);

        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("일반 사용자가 상품 목록을 조회하면 403과 FORBIDDEN을 반환한다")
    void getProducts_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("토큰 없이 상품 목록을 조회하면 401과 UNAUTHORIZED를 반환한다")
    void getProducts_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ===== GET /api/admin/products/{productId} =====

    @Test
    @DisplayName("관리자가 특정 상품을 조회하면 200과 해당 상품 정보를 반환한다")
    void getProduct_asAdmin_success() throws Exception {
        Member seller = savedSeller();
        Category category = savedCategory();
        Product product = productRepository.save(
                Product.create(seller, category, "아이폰 15", "상태 좋은 아이폰", BigDecimal.valueOf(800000), "서울 강남구"));

        mockMvc.perform(get("/api/admin/products/{productId}", product.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.title").value("아이폰 15"))
                .andExpect(jsonPath("$.data.price").value(800000))
                .andExpect(jsonPath("$.data.hidden").value(false));
    }

    @Test
    @DisplayName("관리자가 존재하지 않는 상품을 조회하면 404와 PRODUCT_NOT_FOUND를 반환한다")
    void getProduct_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/products/{productId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // ===== PATCH /api/admin/products/{productId}/hidden, DELETE /api/admin/products/{productId} =====

    @Test
    @DisplayName("관리자가 상품을 숨김 처리하면 200을 반환하고 hidden이 true가 된다")
    void hideProduct_asAdmin_success() throws Exception {
        Member seller = savedSeller();
        Category category = savedCategory();
        Product product = productRepository.save(
                Product.create(seller, category, "아이폰 15", "설명", BigDecimal.valueOf(800000), "서울 강남구"));

        mockMvc.perform(patch("/api/admin/products/{productId}/hidden", product.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertThat(productRepository.findById(product.getId()).orElseThrow().isHidden()).isTrue();
    }

    @Test
    @DisplayName("관리자가 상품을 삭제하면 200을 반환하고 소프트 삭제된다")
    void deleteProduct_asAdmin_success() throws Exception {
        Member seller = savedSeller();
        Category category = savedCategory();
        Product product = productRepository.save(
                Product.create(seller, category, "아이폰 15", "설명", BigDecimal.valueOf(800000), "서울 강남구"));

        mockMvc.perform(delete("/api/admin/products/{productId}", product.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertThat(productRepository.findById(product.getId()).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("관리자가 존재하지 않는 상품을 삭제하면 404와 PRODUCT_NOT_FOUND를 반환한다")
    void deleteProduct_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/admin/products/{productId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }
}