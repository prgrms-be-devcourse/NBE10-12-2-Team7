package com.dongnemarket.member.entity;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "member_locations",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_member_locations_member_region",
				columnNames = {"member_id", "region"}
		)
)
public class MemberLocation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, length = 50)
	private String region;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active;

	protected MemberLocation() {
	}

	private MemberLocation(Member member, String region, int sortOrder, boolean active) {
		this.member = member;
		this.region = region;
		this.sortOrder = sortOrder;
		this.active = active;
	}

	public static MemberLocation create(Member member, String region, int sortOrder, boolean active) {
		return new MemberLocation(member, region, sortOrder, active);
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getRegion() {
		return region;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}
}
