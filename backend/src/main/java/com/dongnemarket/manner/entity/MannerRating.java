package com.dongnemarket.manner.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 거래 완료 후 구매자가 판매자에게 남기는 별점(1~5). 매너온도의 입력값 중 하나다.
 * <p>한 거래(상품+구매자)당 별점은 한 번만 남길 수 있다 — {@code UNIQUE(product_id, rater_id)}로 보장한다.
 * 등록 가능 여부(그 상품 채팅방 참여자인지, 거래가 실제로 완료됐는지)는 서비스 레이어에서 검증하고,
 * 이 엔티티는 "이미 검증된 별점 하나"만 표현한다.
 */
@Entity
@Table(name = "manner_ratings", uniqueConstraints = @UniqueConstraint(
        name = "uk_manner_ratings_product_rater", columnNames = {"product_id", "rater_id"}
))
public class MannerRating extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 별점을 남긴 구매자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id", nullable = false)
    private Member rater;

    /** 별점을 받는 판매자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ratee_id", nullable = false)
    private Member ratee;

    @Column(nullable = false)
    private int score;

    protected MannerRating() {}

    private MannerRating(Product product, Member rater, Member ratee, int score) {
        this.product = product;
        this.rater = rater;
        this.ratee = ratee;
        this.score = score;
    }

    public static MannerRating of(Product product, Member rater, Member ratee, int score) {
        return new MannerRating(product, rater, ratee, score);
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Member getRater() { return rater; }
    public Member getRatee() { return ratee; }
    public int getScore() { return score; }
}
