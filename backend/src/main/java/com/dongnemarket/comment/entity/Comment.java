package com.dongnemarket.comment.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Comment() {}

    public static Comment of(Long memberId, Long productId, String content) {
        Comment comment = new Comment();
        comment.memberId = memberId;
        comment.productId = productId;
        comment.content = content;
        return comment;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getProductId() { return productId; }
    public String getContent() { return content; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
