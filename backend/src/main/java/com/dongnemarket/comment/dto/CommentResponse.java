package com.dongnemarket.comment.dto;

import com.dongnemarket.comment.entity.Comment;

import java.time.LocalDateTime;

public class CommentResponse {

    private final Long id;
    private final Long memberId;
    private final Long productId;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CommentResponse(Long id, Long memberId, Long productId, String content,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.productId = productId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getMemberId(),
                comment.getProductId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getProductId() { return productId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
