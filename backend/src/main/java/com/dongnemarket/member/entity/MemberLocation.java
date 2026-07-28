package com.dongnemarket.member.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.region.entity.Region;
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

@Entity
@Table(
			name = "member_locations",
			uniqueConstraints = @UniqueConstraint(
					name = "uk_member_locations_member_region_id",
					columnNames = {"member_id", "region_id"}
			)
	)
public class MemberLocation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region_id", nullable = false)
	private Region regionRef;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active;

	protected MemberLocation() {
	}

	private MemberLocation(Member member, Region regionRef, int sortOrder, boolean active) {
		this.member = member;
		this.regionRef = regionRef;
		this.sortOrder = sortOrder;
		this.active = active;
	}

	public static MemberLocation create(Member member, Region regionRef, int sortOrder, boolean active) {
		return new MemberLocation(member, regionRef, sortOrder, active);
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Region getRegionRef() {
		return regionRef;
	}

	public String getRegionCode() {
		return regionRef == null ? null : regionRef.getCode();
	}

	public String getRegionName() {
		return regionRef == null ? null : regionRef.getDisplayName();
	}

	public String getRegionFullName() {
		return regionRef == null ? null : regionRef.getFullName();
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}
}
