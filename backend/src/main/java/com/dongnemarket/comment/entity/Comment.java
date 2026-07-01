package com.dongnemarket.comment.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Comment() {}

    public static Comment of(Member member, Product product, String content) {
        Comment comment = new Comment();
        comment.member = member;
        comment.product = product;
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
    public Member getMember() { return member; }
    public Product getProduct() { return product; }
    public String getContent() { return content; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    /** 연관 프록시의 식별자만 반환한다(식별자 접근은 프록시 초기화를 유발하지 않음). */
    public Long getMemberId() { return member.getId(); }
    public Long getProductId() { return product.getId(); }
}
