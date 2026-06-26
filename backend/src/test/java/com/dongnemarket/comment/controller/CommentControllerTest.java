package com.dongnemarket.comment.controller;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.comment.repository.CommentRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 HTTP 요청으로 댓글 작성 성공/실패를 검증하는 통합 테스트 (H2, MySQL/Docker 불필요).
 * 실제 JWT로 @AuthenticationPrincipal(memberId) 바인딩까지 검증한다.
 * Postman 시나리오(docs/postman/comment-create.md)의 응답 예시는 이 테스트로 직접 확인한 값이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

    private Long productId;
    private Long categoryId;
    private Long memberId;
    private Long otherMemberId;
    private String token;

    @BeforeEach
    void setUp() {
        Member writer = memberRepository.save(Member.createUser("writer@example.com", "encoded-pw", "writer"));
        Member seller = memberRepository.save(Member.createUser("seller@example.com", "encoded-pw", "seller"));
        // 시드된 기본 카테고리(CategoryInitializer)와 이름이 겹치지 않도록 테스트 전용 카테고리를 만든다.
        Category category = categoryRepository.save(new Category("댓글테스트전용카테고리"));
        Product product = productRepository.save(
                Product.create(seller, category, "맥북 프로", "상태 좋음", 1_500_000, "서울 강남구"));

        categoryId = category.getId();
        productId = product.getId();
        memberId = writer.getId();
        otherMemberId = seller.getId();
        token = "Bearer " + jwtTokenProvider.createAccessToken(writer.getId(), "ROLE_USER");
    }

    @AfterEach
    void cleanUp() {
        commentRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
        // 시드 카테고리는 보존하고 테스트가 만든 카테고리만 제거한다.
        categoryRepository.deleteById(categoryId);
    }

    @Test
    @DisplayName("로그인 사용자가 존재하는 상품에 댓글을 작성하면 201과 댓글 정보를 반환한다")
    void createComment_success() throws Exception {
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
    @DisplayName("토큰 없이 댓글 작성 요청하면 401과 UNAUTHORIZED를 반환한다")
    void createComment_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/comments", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"좋은 상품이네요\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("존재하지 않는 상품에 댓글을 작성하면 404와 PRODUCT_NOT_FOUND를 반환한다")
    void createComment_productNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/comments", 999_999L)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"좋은 상품이네요\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("댓글 내용이 공백이면 400과 INVALID_INPUT_VALUE를 반환한다")
    void createComment_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/comments", productId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \" \" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("작성자 본인이 자신의 댓글을 수정하면 200과 수정된 내용을 반환한다")
    void updateComment_success() throws Exception {
        Long commentId = commentRepository.save(Comment.of(memberId, productId, "원본 내용")).getId();

        mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"수정된 내용\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").value("수정된 내용"));
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 수정하면 403과 COMMENT_OWNER_ONLY를 반환한다")
    void updateComment_notOwner_returns403() throws Exception {
        Long commentId = commentRepository.save(Comment.of(otherMemberId, productId, "남의 댓글")).getId();

        mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"수정 시도\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("COMMENT_OWNER_ONLY"));
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 수정하면 404와 COMMENT_NOT_FOUND를 반환한다")
    void updateComment_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/comments/{commentId}", 999_999L)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"수정\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이 댓글 수정 요청하면 401과 UNAUTHORIZED를 반환한다")
    void updateComment_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/comments/{commentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"수정\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("작성자 본인이 자신의 댓글을 삭제하면 200을 반환한다")
    void deleteComment_success() throws Exception {
        Long commentId = commentRepository.save(Comment.of(memberId, productId, "삭제될 댓글")).getId();

        mockMvc.perform(delete("/api/comments/{commentId}", commentId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 삭제하면 403과 COMMENT_OWNER_ONLY를 반환한다")
    void deleteComment_notOwner_returns403() throws Exception {
        Long commentId = commentRepository.save(Comment.of(otherMemberId, productId, "남의 댓글")).getId();

        mockMvc.perform(delete("/api/comments/{commentId}", commentId)
                        .header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("COMMENT_OWNER_ONLY"));
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 삭제하면 404와 COMMENT_NOT_FOUND를 반환한다")
    void deleteComment_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/comments/{commentId}", 999_999L)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이 댓글 삭제 요청하면 401과 UNAUTHORIZED를 반환한다")
    void deleteComment_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/comments/{commentId}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
