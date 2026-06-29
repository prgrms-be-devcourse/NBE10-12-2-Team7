package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminCommentResponse;
import com.dongnemarket.admin.repository.AdminCommentRepository;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminCommentService {

    private final AdminCommentRepository adminCommentRepository;

    public AdminCommentService(AdminCommentRepository adminCommentRepository) {
        this.adminCommentRepository = adminCommentRepository;
    }

    /** 전체 댓글 목록 (삭제 포함) */
    public List<AdminCommentResponse> getComments() {
        return adminCommentRepository.findAll().stream()
                .map(AdminCommentResponse::from)
                .toList();
    }

    /** 댓글 소프트 삭제 (관리자는 작성자가 아니어도 삭제 가능) */
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = adminCommentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        comment.softDelete();
    }
}
