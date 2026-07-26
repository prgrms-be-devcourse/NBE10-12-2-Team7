package com.dongnemarket.product.entity;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.region.entity.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = @Index(name = "idx_products_region", columnList = "region_id"))
public class Product extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 1000)
	private String description;

	@Column(nullable = false)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TradeStatus tradeStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region_id", nullable = false)
	private Region region;

	@Column(nullable = false)
	private long viewCount;

	@Column(nullable = false)
	private int favoriteCount;

	@Column(length = 1000)
	private String thumbnailUrl;

	@Column(nullable = false)
	private boolean hidden;

	@Column
	private LocalDateTime deletedAt;

	@Column
	private LocalDateTime completedAt;

	protected Product() {
	}

	private Product(Member member, Category category, String title, String description,
					BigDecimal price, Region region) {
		this.member = member;
		this.category = category;
		this.title = title;
		this.description = description;
		this.price = price;
		this.tradeStatus = TradeStatus.ON_SALE;
		this.region = region;
		this.viewCount = 0L;
		this.favoriteCount = 0;
		this.hidden = false;
	}

	public static Product create(Member member, Category category, String title, String description,
								 BigDecimal price, Region region) {
		return new Product(member, category, title, description, price, region);
	}

	public void hide() {
		this.hidden = true;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	public void increaseViewCount() {
		this.viewCount++;
	}

	public void update(Category category, String title, String description, BigDecimal price, Region region) {
		this.category = category;
		this.title = title;
		this.description = description;
		this.price = price;
		this.region = region;
	}

	public void changeTradeStatus(TradeStatus tradeStatus) {
		this.tradeStatus = tradeStatus;
		if (tradeStatus == TradeStatus.COMPLETED && this.completedAt == null) {
			this.completedAt = LocalDateTime.now();
		}
	}

	public void changeThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public void complete() {
		changeTradeStatus(TradeStatus.COMPLETED);
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public boolean isCompleted() {
		return tradeStatus == TradeStatus.COMPLETED;
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Category getCategory() {
		return category;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public TradeStatus getTradeStatus() {
		return tradeStatus;
	}

	public Region getRegion() {
		return region;
	}

	public long getViewCount() {
		return viewCount;
	}

	public int getFavoriteCount() {
		return favoriteCount;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public boolean isHidden() {
		return hidden;
	}

	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

}
