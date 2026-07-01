package com.dongnemarket.favorite.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import jakarta.persistence.*;

@Entity
@Table(name = "favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorites_member_product",
                columnNames = {"member_id", "product_id"}
        ))
public class Favorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    protected Favorite() {}

    public static Favorite of(Member member, Product product) {
        Favorite favorite = new Favorite();
        favorite.member = member;
        favorite.product = product;
        return favorite;
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Product getProduct() { return product; }

    /** 연관 프록시의 식별자만 반환한다(식별자 접근은 프록시 초기화를 유발하지 않음). */
    public Long getMemberId() { return member.getId(); }
    public Long getProductId() { return product.getId(); }
}
