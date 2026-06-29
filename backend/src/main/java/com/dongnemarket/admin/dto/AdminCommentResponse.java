package com.dongnemarket.admin.dto;

import com.dongnemarket.comment.entity.Comment;

import java.time.LocalDateTime;

/**
 * 관리자용 댓글 응답. 관리 목적상 deletedAt 까지 포함한다.
 */
public class AdminCommentResponse {

    private final Long commentId;
    private final Long memberId;
    private final Long productId;
    private final String content;
    private final LocalDateTime deletedAt;
    private final LocalDateTime createdAt;

    private AdminCommentResponse(Long commentId, Long memberId, Long productId, String content,
                                LocalDateTime deletedAt, LocalDateTime createdAt) {
        this.commentId = commentId;
        this.memberId = memberId;
        this.productId = productId;
        this.content = content;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
    }

    public static AdminCommentResponse from(Comment comment) {
        return new AdminCommentResponse(
                comment.getId(),
                comment.getMemberId(),
                comment.getProductId(),
                comment.getContent(),
                comment.getDeletedAt(),
                comment.getCreatedAt()
        );
    }

    public Long getCommentId() { return commentId; }
    public Long getMemberId() { return memberId; }
    public Long getProductId() { return productId; }
    public String getContent() { return content; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
