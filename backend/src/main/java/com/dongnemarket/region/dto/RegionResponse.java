package com.dongnemarket.region.dto;

import com.dongnemarket.region.entity.Region;

public class RegionResponse {

	private final Long regionId;
	private final String code;
	private final int level;
	private final String displayName;

	private RegionResponse(Long regionId, String code, int level, String displayName) {
		this.regionId = regionId;
		this.code = code;
		this.level = level;
		this.displayName = displayName;
	}

	public static RegionResponse from(Region region) {
		return new RegionResponse(region.getId(), region.getCode(), region.getLevel(), region.getDisplayName());
	}

	public Long getRegionId() {
		return regionId;
	}

	public String getCode() {
		return code;
	}

	public int getLevel() {
		return level;
	}

	public String getDisplayName() {
		return displayName;
	}
}
