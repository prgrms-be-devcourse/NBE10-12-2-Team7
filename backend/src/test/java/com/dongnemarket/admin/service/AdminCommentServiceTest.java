package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminCommentResponse;
import com.dongnemarket.admin.repository.AdminCommentRepository;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminCommentServiceTest {

    @Mock
    AdminCommentRepository adminCommentRepository;

    @InjectMocks
    AdminCommentService adminCommentService;

    @Test
    @DisplayName("댓글 목록을 조회하면 삭제 포함 전체 댓글을 반환한다")
    void getComments_success() {
        Comment c1 = Comment.of(1L, 1L, "댓글1");
        Comment c2 = Comment.of(2L, 1L, "댓글2");
        given(adminCommentRepository.findAll()).willReturn(List.of(c1, c2));

        List<AdminCommentResponse> responses = adminCommentService.getComments();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(AdminCommentResponse::getContent)
                .containsExactly("댓글1", "댓글2");
    }

    @Test
    @DisplayName("댓글이 없으면 빈 목록을 반환한다")
    void getComments_empty_returnsEmptyList() {
        given(adminCommentRepository.findAll()).willReturn(List.of());

        assertThat(adminCommentService.getComments()).isEmpty();
    }

    @Test
    @DisplayName("관리자가 댓글을 삭제하면 소프트 삭제된다")
    void deleteComment_success() {
        Comment comment = Comment.of(1L, 1L, "부적절한 댓글");
        given(adminCommentRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(comment));

        adminCommentService.deleteComment(1L);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("없는(또는 이미 삭제된) 댓글을 삭제하면 COMMENT_NOT_FOUND 예외가 발생한다")
    void deleteComment_notFound_throwsException() {
        given(adminCommentRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommentService.deleteComment(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }
}
