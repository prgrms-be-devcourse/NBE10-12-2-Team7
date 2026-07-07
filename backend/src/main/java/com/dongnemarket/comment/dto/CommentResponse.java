package com.dongnemarket.comment.dto;

import com.dongnemarket.comment.entity.Comment;

import java.time.LocalDateTime;

public class CommentResponse {

    private final Long id;
    private final Long memberId;
    private final String authorNickname;
    private final Long productId;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CommentResponse(Long id, Long memberId, String authorNickname, Long productId, String content,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.authorNickname = authorNickname;
        this.productId = productId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getMemberId(),
                // 탈퇴한 작성자는 "탈퇴한 사용자"로 마스킹된다(Member.getDisplayNickname, 내용은 그대로 보존).
                comment.getMember().getDisplayNickname(),
                comment.getProductId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getAuthorNickname() { return authorNickname; }
    public Long getProductId() { return productId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
