package com.dongnemarket.favorite.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
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

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    protected Favorite() {}

    public static Favorite of(Long memberId, Long productId) {
        Favorite favorite = new Favorite();
        favorite.memberId = memberId;
        favorite.productId = productId;
        return favorite;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getProductId() { return productId; }
}
