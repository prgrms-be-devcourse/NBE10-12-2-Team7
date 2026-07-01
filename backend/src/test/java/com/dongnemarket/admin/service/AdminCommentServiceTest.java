package com.dongnemarket.admin.service;

import com.dongnemarket.admin.repository.AdminCommentRepository;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * [단위] AdminCommentService.deleteComment — 서비스 고유 로직만 검증.
 *  - 검증 대상: 성공(찾아서 softDelete) + NOT_FOUND 예외.
 *  - 제외: getComments 위임(통합), "이미 삭제=404" 판별은 findByIdAndDeletedAtIsNull 쿼리(리포지토리/통합).
 */
@ExtendWith(MockitoExtension.class)
class AdminCommentServiceTest {

    @Mock
    AdminCommentRepository adminCommentRepository;

    @InjectMocks
    AdminCommentService adminCommentService;

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("미삭제 댓글을 삭제하면 softDelete 되어 deletedAt이 기록된다(작성자 불문)")
        void deleteComment_success() {
            Comment comment = Comment.of(null, null, "부적절한 댓글");
            given(adminCommentRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(comment));

            adminCommentService.deleteComment(1L);

            assertThat(comment.isDeleted()).isTrue();
            assertThat(comment.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("없는(또는 이미 삭제된) 댓글을 삭제하면 COMMENT_NOT_FOUND 예외가 발생한다")
        void notFound_throwsException() {
            given(adminCommentRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminCommentService.deleteComment(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
        }
    }
}