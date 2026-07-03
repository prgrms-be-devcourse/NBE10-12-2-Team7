package com.dongnemarket.product.entity;

import com.dongnemarket.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, length = 1000)
	private String imageUrl;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean representative;

	protected ProductImage() {
	}

	private ProductImage(Product product, String imageUrl, int sortOrder, boolean representative) {
		this.product = product;
		this.imageUrl = imageUrl;
		this.sortOrder = sortOrder;
		this.representative = representative;
	}

	public static ProductImage create(Product product, String imageUrl, int sortOrder, boolean representative) {
		return new ProductImage(product, imageUrl, sortOrder, representative);
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isRepresentative() {
		return representative;
	}
}
