package com.dongnemarket.region.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
		name = "regions",
		indexes = {
				@Index(name = "idx_regions_code", columnList = "code"),
				@Index(name = "idx_regions_parent_id", columnList = "parent_id"),
				@Index(name = "idx_regions_level", columnList = "level")
		}
)
public class Region {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 10)
	private String code;

	@Column(nullable = false)
	private int level;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Region parent;

	@Column(nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false, length = 50)
	private String displayName;

	@Column(precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(precision = 10, scale = 7)
	private BigDecimal longitude;

	protected Region() {
	}

	private Region(String code, int level, Region parent, String fullName, String displayName) {
		this.code = code;
		this.level = level;
		this.parent = parent;
		this.fullName = fullName;
		this.displayName = displayName;
	}

	public static Region root(String code, String fullName, String displayName) {
		return new Region(code, 1, null, fullName, displayName);
	}

	public static Region child(String code, int level, Region parent, String fullName, String displayName) {
		return new Region(code, level, parent, fullName, displayName);
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public int getLevel() {
		return level;
	}

	public Region getParent() {
		return parent;
	}

	public String getFullName() {
		return fullName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

}
