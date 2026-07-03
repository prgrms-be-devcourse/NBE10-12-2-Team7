package com.dongnemarket.member.dto;

import com.dongnemarket.member.entity.MemberLocation;

public class MemberLocationResponse {

	private final String region;
	private final int sortOrder;
	private final boolean active;

	private MemberLocationResponse(String region, int sortOrder, boolean active) {
		this.region = region;
		this.sortOrder = sortOrder;
		this.active = active;
	}

	public static MemberLocationResponse from(MemberLocation memberLocation) {
		return new MemberLocationResponse(
				memberLocation.getRegion(),
				memberLocation.getSortOrder(),
				memberLocation.isActive()
		);
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
