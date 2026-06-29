package com.dongnemarket.admin.controller;

import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.comment.repository.CommentRepository;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 댓글 관리 API(/api/admin/comments) 통합 테스트 (H2, Docker 불필요).
 * 토큰은 JwtTokenProvider 로 직접 발급한다(role 문자열로 권한 부여).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CommentRepository commentRepository;

    @AfterEach
    void cleanUp() {
        commentRepository.deleteAll();
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

    @Test
    @DisplayName("관리자가 댓글 목록을 조회하면 200과 전체 댓글을 반환한다")
    void getComments_asAdmin_success() throws Exception {
        commentRepository.save(Comment.of(1L, 1L, "댓글1"));
        commentRepository.save(Comment.of(1L, 1L, "댓글2"));

        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("관리자가 댓글을 삭제하면 200을 반환하고 소프트 삭제된다")
    void deleteComment_asAdmin_success() throws Exception {
        Comment comment = commentRepository.save(Comment.of(1L, 1L, "부적절한 댓글"));

        mockMvc.perform(delete("/api/admin/comments/{commentId}", comment.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("관리자가 없는 댓글을 삭제하면 404와 COMMENT_NOT_FOUND를 반환한다")
    void deleteComment_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/admin/comments/{commentId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("일반 사용자가 댓글 목록을 조회하면 403과 FORBIDDEN을 반환한다")
    void getComments_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("토큰 없이 댓글 목록을 조회하면 401과 UNAUTHORIZED를 반환한다")
    void getComments_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
