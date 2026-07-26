package com.dongnemarket.region.entity;

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

/**
 * 법정동코드 기반 지역 마스터. 시(1)–구(2)–동(3) 계층을 parent 셀프참조로 표현한다.
 *
 * <p>PK는 의미 없는 auto id다. 법정동코드는 행정구역 개편 시 폐지·재발급되는 가변값이라
 * PK에 싣지 않는다. 식별은 항상 id 또는 code로 하며, display_name은 중복(전국 590종)이라
 * 절대 조회 키로 쓰지 않는다.
 */
@Entity
@Table(name = "regions", indexes = {
		@Index(name = "idx_regions_parent", columnList = "parent_id")
})
public class Region {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 법정동 10자리 코드. 앞 2자리=시도, 앞 5자리=시군구. prefix 조작용이라 문자열.
	@Column(nullable = false, unique = true, length = 10)
	private String code;

	@Column(nullable = false)
	private int level;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Region parent;

	// "서울특별시 종로구 청운동" 표시용 비정규화.
	@Column(nullable = false, length = 100)
	private String fullName;

	// "청운동" 단일 표시용. unique 아님.
	@Column(nullable = false, length = 50)
	private String displayName;

	protected Region() {
	}

	public Region(String code, int level, Region parent, String fullName, String displayName) {
		this.code = code;
		this.level = level;
		this.parent = parent;
		this.fullName = fullName;
		this.displayName = displayName;
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
}
