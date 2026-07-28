package com.dongnemarket.escrow.controller;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.escrow.entity.Escrow;
import com.dongnemarket.escrow.repository.EscrowRepository;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EscrowControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired MemberRepository memberRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired EscrowRepository escrowRepository;

    private static final BigDecimal PRICE = BigDecimal.valueOf(15_000);

    @AfterEach
    void cleanUp() {
        escrowRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    // ===== 픽스처 =====

    private Member saveMember(String email, String nickname) {
        return memberRepository.save(Member.createUser(email, "encodedPassword", nickname));
    }

    private Product saveOnSaleProduct(Member seller) {
        Category category = categoryRepository.save(new Category("생활/가전"));
        return productRepository.save(
                Product.create(seller, category, "남은 고기", "같이 먹다 남은 고기", PRICE, "강원 강릉시"));
    }

    /** 예치중(IN_ESCROW) 거래 + 상품 RESERVED 상태를 준비한다(실제 create가 만드는 상태와 동일). */
    private Escrow saveEscrow(Product product, Member buyer, Member seller) {
        product.changeTradeStatus(TradeStatus.RESERVED);
        productRepository.save(product);
        return escrowRepository.save(Escrow.create(product, buyer, seller, product.getPrice()));
    }

    private String token(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
    }

    // ===== E1: 거래 시작 =====

    @Test
    @DisplayName("구매자가 판매중 상품에 안심결제를 시작하면 201·IN_ESCROW로 예치되고 상품이 RESERVED가 된다")
    void startEscrow_success() throws Exception {
        // given
        Member seller = saveMember("seller@example.com", "판매자");
        Member buyer = saveMember("buyer@example.com", "구매자");
        Product product = saveOnSaleProduct(seller);

        // when
        mockMvc.perform(post("/api/escrows")
                        .header("Authorization", token(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d}".formatted(product.getId())))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.escrowId").exists())
                .andExpect(jsonPath("$.data.status").value("IN_ESCROW"))
                .andExpect(jsonPath("$.data.amount").value(15000))
                .andExpect(jsonPath("$.data.buyerId").value(buyer.getId()))
                .andExpect(jsonPath("$.data.sellerId").value(seller.getId()));

        Product reserved = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reserved.getTradeStatus()).isEqualTo(TradeStatus.RESERVED);
    }

    // ===== E2: 구매확정 =====

    @Test
    @DisplayName("구매자가 구매확정하면 200·DONE으로 정산되고 상품이 COMPLETED가 된다")
    void confirmEscrow_success() throws Exception {
        // given
        Member seller = saveMember("seller@example.com", "판매자");
        Member buyer = saveMember("buyer@example.com", "구매자");
        Escrow escrow = saveEscrow(saveOnSaleProduct(seller), buyer, seller);

        // when
        mockMvc.perform(post("/api/escrows/{id}/confirm", escrow.getId())
                        .header("Authorization", token(buyer)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));

        Product completed = productRepository.findById(escrow.getProduct().getId()).orElseThrow();
        assertThat(completed.getTradeStatus()).isEqualTo(TradeStatus.COMPLETED);
    }

    // ===== E3: 취소 =====

    @Test
    @DisplayName("구매자가 취소하면 200·CANCELED로 환불되고 상품이 다시 ON_SALE로 돌아온다")
    void cancelEscrow_success() throws Exception {
        // given
        Member seller = saveMember("seller@example.com", "판매자");
        Member buyer = saveMember("buyer@example.com", "구매자");
        Escrow escrow = saveEscrow(saveOnSaleProduct(seller), buyer, seller);

        // when
        mockMvc.perform(post("/api/escrows/{id}/cancel", escrow.getId())
                        .header("Authorization", token(buyer)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        Product onSale = productRepository.findById(escrow.getProduct().getId()).orElseThrow();
        assertThat(onSale.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
    }

    // ===== E4: 본인 상품 =====

    @Test
    @DisplayName("판매자가 본인 상품에 거래를 시작하면 400 CANNOT_ESCROW_OWN_PRODUCT")
    void startEscrow_ownProduct_400() throws Exception {
        Member seller = saveMember("seller@example.com", "판매자");
        Product product = saveOnSaleProduct(seller);

        mockMvc.perform(post("/api/escrows")
                        .header("Authorization", token(seller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d}".formatted(product.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CANNOT_ESCROW_OWN_PRODUCT"));
    }

    // ===== E5: 거래중(RESERVED) 상품 재시작 =====

    @Test
    @DisplayName("이미 예치중이라 RESERVED가 된 상품에 다른 구매자가 거래를 시작하면 400 PRODUCT_NOT_ON_SALE")
    void startEscrow_reservedProduct_400() throws Exception {
        Member seller = saveMember("seller@example.com", "판매자");
        Member buyerA = saveMember("buyerA@example.com", "구매자A");
        Member buyerB = saveMember("buyerB@example.com", "구매자B");
        Escrow escrow = saveEscrow(saveOnSaleProduct(seller), buyerA, seller); // 이미 진행 중 → 상품 RESERVED

        mockMvc.perform(post("/api/escrows")
                        .header("Authorization", token(buyerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d}".formatted(escrow.getProduct().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_ON_SALE"));
    }

    // ===== E6: 남의 거래 =====

    @Test
    @DisplayName("자신의 거래가 아닌 거래를 확정하려 하면 403 ESCROW_ACCESS_DENIED")
    void confirmEscrow_notOwner_403() throws Exception {
        Member seller = saveMember("seller@example.com", "판매자");
        Member buyer = saveMember("buyer@example.com", "구매자");
        Member stranger = saveMember("stranger@example.com", "제3자");
        Escrow escrow = saveEscrow(saveOnSaleProduct(seller), buyer, seller);

        mockMvc.perform(post("/api/escrows/{id}/confirm", escrow.getId())
                        .header("Authorization", token(stranger)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ESCROW_ACCESS_DENIED"));
    }

    // ===== E7: 인증 없음 =====

    @Test
    @DisplayName("인증 없이 거래를 시작하면 401을 반환한다")
    void startEscrow_noToken_401() throws Exception {
        mockMvc.perform(post("/api/escrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ===== E8: 종료된 거래 재확정 =====

    @Test
    @DisplayName("이미 종료(DONE)된 거래를 다시 확정하면 409 ESCROW_NOT_IN_ESCROW")
    void confirmEscrow_alreadyDone_409() throws Exception {
        Member seller = saveMember("seller@example.com", "판매자");
        Member buyer = saveMember("buyer@example.com", "구매자");
        Escrow escrow = saveEscrow(saveOnSaleProduct(seller), buyer, seller);
        escrow.confirm();                 // DONE으로 만든 뒤 저장
        escrowRepository.save(escrow);

        mockMvc.perform(post("/api/escrows/{id}/confirm", escrow.getId())
                        .header("Authorization", token(buyer)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ESCROW_NOT_IN_ESCROW"));
    }
}
