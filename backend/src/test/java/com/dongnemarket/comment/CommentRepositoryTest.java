package com.dongnemarket.comment;

import com.dongnemarket.comment.dto.CommentResponse;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.comment.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    // 성공: 저장 후 조회 및 DTO 변환, 소프트 삭제 동작 확인
    @Test
    void 댓글_저장_조회_및_소프트삭제_성공() {
        Comment saved = commentRepository.saveAndFlush(Comment.of(1L, 100L, "좋은 상품이네요!"));

        // 저장 후 삭제되지 않은 댓글 목록 조회
        List<Comment> comments = commentRepository.findAllByProductIdAndDeletedAtIsNullOrderByCreatedAtAsc(100L);
        assertThat(comments).hasSize(1);

        // DTO 변환
        CommentResponse response = CommentResponse.from(saved);
        assertThat(response.getContent()).isEqualTo("좋은 상품이네요!");
        assertThat(response.getMemberId()).isEqualTo(1L);

        // 소프트 삭제 후 목록에서 제외
        saved.softDelete();
        commentRepository.saveAndFlush(saved);
        List<Comment> afterDelete = commentRepository.findAllByProductIdAndDeletedAtIsNullOrderByCreatedAtAsc(100L);
        assertThat(afterDelete).isEmpty();
    }

    // 실패 1: 삭제된 댓글은 findByIdAndDeletedAtIsNull 으로 조회되지 않음
    @Test
    void 삭제된_댓글_단건_조회시_빈값_반환() {
        Comment comment = commentRepository.saveAndFlush(Comment.of(1L, 100L, "삭제될 댓글"));
        comment.softDelete();
        commentRepository.saveAndFlush(comment);

        Optional<Comment> result = commentRepository.findByIdAndDeletedAtIsNull(comment.getId());
        assertThat(result).isEmpty();
    }

    // 실패 2: 존재하지 않는 댓글 단건 조회 시 빈값 반환
    @Test
    void 존재하지_않는_댓글_조회시_빈값_반환() {
        Optional<Comment> result = commentRepository.findByIdAndDeletedAtIsNull(999L);
        assertThat(result).isEmpty();
    }
}
