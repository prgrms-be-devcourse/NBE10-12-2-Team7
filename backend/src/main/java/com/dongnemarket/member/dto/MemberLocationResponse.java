package com.dongnemarket.member.dto;

import com.dongnemarket.member.entity.MemberLocation;

public class MemberLocationResponse {

	private final String region;
	private final String regionCode;
	private final String regionName;
	private final String regionFullName;
	private final int sortOrder;
	private final boolean active;

	private MemberLocationResponse(String region, String regionCode, String regionName, String regionFullName,
								   int sortOrder, boolean active) {
		this.region = region;
		this.regionCode = regionCode;
		this.regionName = regionName;
		this.regionFullName = regionFullName;
		this.sortOrder = sortOrder;
		this.active = active;
	}

	public static MemberLocationResponse from(MemberLocation memberLocation) {
			return new MemberLocationResponse(
					memberLocation.getRegion(),
					memberLocation.getRegionCode(),
					memberLocation.getRegionName(),
					memberLocation.getRegionFullName(),
					memberLocation.getSortOrder(),
					memberLocation.isActive()
			);
	}

	public String getRegion() {
		return region;
	}

	public String getRegionCode() {
		return regionCode;
	}

	public String getRegionName() {
		return regionName;
	}

	public String getRegionFullName() {
		return regionFullName;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}
}
