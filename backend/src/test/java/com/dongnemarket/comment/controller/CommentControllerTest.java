package com.dongnemarket.comment.controller;

import java.math.BigDecimal;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.comment.repository.CommentRepository;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.notification.repository.NotificationRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * 댓글 API 통합 테스트.
 * <p>실제 HTTP 요청으로 사용자 유스케이스(성공·실패·엣지)를 검증한다.
 * 통신 계층만 MockMvc로 대체하고 Controller·Service·Repository는 실제로 동작한다(H2, MySQL/Docker 불필요).
 * 실제 JWT로 @AuthenticationPrincipal(memberId) 바인딩까지 검증한다.
 * Postman 시나리오(docs/postman/comment-create.md)의 응답 예시는 이 테스트로 직접 확인한 값이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("댓글 API 통합 테스트")
class CommentControllerTest {

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
    CommentRepository commentRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    RegionRepository regionRepository;

    private Long productId;
    private Long categoryId;
    private String token;
    private Member writer;
    private Member seller;
    private Product product;

    @BeforeEach
    void setUp() {
        writer = memberRepository.save(Member.createUser("writer@example.com", "encoded-pw", "writer"));
        seller = memberRepository.save(Member.createUser("seller@example.com", "encoded-pw", "seller"));
        // 시드된 기본 카테고리(CategorySeeder)와 이름이 겹치지 않도록 테스트 전용 카테고리를 만든다.
        Category category = categoryRepository.save(new Category("댓글테스트전용카테고리"));
        Region region = regionRepository.findFirstByLevelOrderByCodeAsc(3).orElseThrow();
        product = productRepository.save(
                Product.create(seller, category, "맥북 프로", "상태 좋음", BigDecimal.valueOf(1_500_000), region));

        categoryId = category.getId();
        productId = product.getId();
        token = "Bearer " + jwtTokenProvider.createAccessToken(writer.getId(), "ROLE_USER");
    }

    @AfterEach
    void cleanUp() {
        // 댓글 작성은 상품 소유자에게 알림을 남기므로(AFTER_COMMIT), member 삭제 전에 알림부터 정리한다(FK).
        notificationRepository.deleteAll();
        commentRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
        // 시드 카테고리는 보존하고 테스트가 만든 카테고리만 제거한다.
        categoryRepository.deleteById(categoryId);
    }

    @Nested
    @DisplayName("댓글 작성 (POST /api/products/{productId}/comments)")
    class CreateComment {

        @Test
        @DisplayName("로그인 사용자가 존재하는 상품에 댓글을 작성하면 201과 댓글 정보를 반환한다")
        void success() throws Exception {
            mockMvc.perform(post("/api/products/{productId}/comments", productId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"좋은 상품이네요\" }"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.productId").value(productId.intValue()))
                    .andExpect(jsonPath("$.data.content").value("좋은 상품이네요"))
                    .andExpect(jsonPath("$.data.id").exists());
        }

        @Test
        @DisplayName("토큰 없이 요청하면 401과 UNAUTHORIZED를 반환한다")
        void withoutToken_returns401() throws Exception {
            mockMvc.perform(post("/api/products/{productId}/comments", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"좋은 상품이네요\" }"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("변조된 토큰으로 요청하면 401과 INVALID_TOKEN을 반환한다")
        void tamperedToken_returns401() throws Exception {
            mockMvc.perform(post("/api/products/{productId}/comments", productId)
                            .header("Authorization", token + "tampered")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"좋은 상품이네요\" }"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
        }

        @Test
        @DisplayName("존재하지 않는 상품에 작성하면 404와 PRODUCT_NOT_FOUND를 반환한다")
        void productNotFound_returns404() throws Exception {
            mockMvc.perform(post("/api/products/{productId}/comments", 999_999L)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"좋은 상품이네요\" }"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
        }

        @Test
        @DisplayName("숨김 처리된 상품에 작성하면 404와 PRODUCT_NOT_FOUND를 반환한다")
        void hiddenProduct_returns404() throws Exception {
            product.hide();
            productRepository.saveAndFlush(product);

            mockMvc.perform(post("/api/products/{productId}/comments", productId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"좋은 상품이네요\" }"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
        }

        @Test
        @DisplayName("댓글 내용이 공백이면 400과 INVALID_INPUT_VALUE를 반환한다")
        void blankContent_returns400() throws Exception {
            mockMvc.perform(post("/api/products/{productId}/comments", productId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \" \" }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
        }

        @Test
        @DisplayName("댓글 내용이 500자를 초과하면 400과 INVALID_INPUT_VALUE를 반환한다")
        void tooLongContent_returns400() throws Exception {
            String tooLong = "a".repeat(501);
            mockMvc.perform(post("/api/products/{productId}/comments", productId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"" + tooLong + "\" }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회 (GET /api/products/{productId}/comments)")
    class GetComments {

        @Test
        @DisplayName("비로그인 사용자도 삭제되지 않은 댓글을 작성순으로 200과 함께 반환한다")
        void withoutToken_success() throws Exception {
            commentRepository.save(Comment.of(writer, product, "첫 번째 댓글"));
            commentRepository.save(Comment.of(seller, product, "두 번째 댓글"));
            Comment deleted = commentRepository.save(Comment.of(writer, product, "삭제된 댓글"));
            deleted.softDelete();
            commentRepository.save(deleted);

            mockMvc.perform(get("/api/products/{productId}/comments", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].content").value("첫 번째 댓글"))
                    .andExpect(jsonPath("$.data[0].authorNickname").value("writer"))
                    .andExpect(jsonPath("$.data[1].content").value("두 번째 댓글"))
                    .andExpect(jsonPath("$.data[1].authorNickname").value("seller"));
        }

        @Test
        @DisplayName("탈퇴한 작성자의 댓글은 닉네임이 '탈퇴한 사용자'로 마스킹되고 내용은 그대로 유지된다")
        void withdrawnAuthor_maskedNicknameButContentKept() throws Exception {
            commentRepository.save(Comment.of(writer, product, "탈퇴 전에 남긴 댓글"));
            writer.softDelete();            // 작성자 탈퇴(회원 소프트 삭제는 댓글에 cascade 하지 않음)
            memberRepository.save(writer);

            mockMvc.perform(get("/api/products/{productId}/comments", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].authorNickname").value("탈퇴한 사용자"))
                    .andExpect(jsonPath("$.data[0].content").value("탈퇴 전에 남긴 댓글"));
        }

        @Test
        @DisplayName("댓글이 없는 상품을 조회하면 200과 빈 배열을 반환한다")
        void empty_returns200() throws Exception {
            mockMvc.perform(get("/api/products/{productId}/comments", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 404와 PRODUCT_NOT_FOUND를 반환한다")
        void productNotFound_returns404() throws Exception {
            mockMvc.perform(get("/api/products/{productId}/comments", 999_999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
        }

        @Test
        @DisplayName("숨김 처리된 상품을 조회하면 404와 PRODUCT_NOT_FOUND를 반환한다")
        void hiddenProduct_returns404() throws Exception {
            product.hide();
            productRepository.saveAndFlush(product);

            mockMvc.perform(get("/api/products/{productId}/comments", productId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
        }

        @Test
        @DisplayName("삭제된 상품을 조회하면 404와 PRODUCT_NOT_FOUND를 반환한다")
        void deletedProduct_returns404() throws Exception {
            product.softDelete();
            productRepository.saveAndFlush(product);

            mockMvc.perform(get("/api/products/{productId}/comments", productId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("댓글 수정 (PATCH /api/comments/{commentId})")
    class UpdateComment {

        @Test
        @DisplayName("작성자 본인이 자신의 댓글을 수정하면 200과 수정된 내용을 반환한다")
        void success() throws Exception {
            Long commentId = commentRepository.save(Comment.of(writer, product, "원본 내용")).getId();

            mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"수정된 내용\" }"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.content").value("수정된 내용"));
        }

        @Test
        @DisplayName("수정 내용이 공백이면 400과 INVALID_INPUT_VALUE를 반환한다")
        void blankContent_returns400() throws Exception {
            Long commentId = commentRepository.save(Comment.of(writer, product, "원본 내용")).getId();

            mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \" \" }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 수정하면 403과 COMMENT_OWNER_ONLY를 반환한다")
        void notOwner_returns403() throws Exception {
            Long commentId = commentRepository.save(Comment.of(seller, product, "남의 댓글")).getId();

            mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"수정 시도\" }"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("COMMENT_OWNER_ONLY"));
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 수정하면 404와 COMMENT_NOT_FOUND를 반환한다")
        void notFound_returns404() throws Exception {
            mockMvc.perform(patch("/api/comments/{commentId}", 999_999L)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"수정\" }"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("COMMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("토큰 없이 요청하면 401과 UNAUTHORIZED를 반환한다")
        void withoutToken_returns401() throws Exception {
            mockMvc.perform(patch("/api/comments/{commentId}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"content\": \"수정\" }"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("댓글 삭제 (DELETE /api/comments/{commentId})")
    class DeleteComment {

        @Test
        @DisplayName("작성자 본인이 자신의 댓글을 삭제하면 200을 반환한다")
        void success() throws Exception {
            Long commentId = commentRepository.save(Comment.of(writer, product, "삭제될 댓글")).getId();

            mockMvc.perform(delete("/api/comments/{commentId}", commentId)
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 삭제하면 403과 COMMENT_OWNER_ONLY를 반환한다")
        void notOwner_returns403() throws Exception {
            Long commentId = commentRepository.save(Comment.of(seller, product, "남의 댓글")).getId();

            mockMvc.perform(delete("/api/comments/{commentId}", commentId)
                            .header("Authorization", token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("COMMENT_OWNER_ONLY"));
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 삭제하면 404와 COMMENT_NOT_FOUND를 반환한다")
        void notFound_returns404() throws Exception {
            mockMvc.perform(delete("/api/comments/{commentId}", 999_999L)
                            .header("Authorization", token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("COMMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("토큰 없이 요청하면 401과 UNAUTHORIZED를 반환한다")
        void withoutToken_returns401() throws Exception {
            mockMvc.perform(delete("/api/comments/{commentId}", 1L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }
    }
}
