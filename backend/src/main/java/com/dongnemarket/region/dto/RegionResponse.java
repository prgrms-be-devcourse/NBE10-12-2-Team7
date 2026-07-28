package com.dongnemarket.region.dto;

import com.dongnemarket.region.entity.Region;

public class RegionResponse {

	private final Long regionId;
	private final String code;
	private final int level;
	private final String parentCode;
	private final String fullName;
	private final String displayName;

	private RegionResponse(Long regionId, String code, int level,
						   String parentCode, String fullName, String displayName) {
		this.regionId = regionId;
		this.code = code;
		this.level = level;
		this.parentCode = parentCode;
		this.fullName = fullName;
		this.displayName = displayName;
	}

	public static RegionResponse from(Region region) {
		String parentCode = region.getParent() == null ? null : region.getParent().getCode();
		return new RegionResponse(
				region.getId(),
				region.getCode(),
				region.getLevel(),
				parentCode,
				region.getFullName(),
				region.getDisplayName()
		);
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

	public String getParentCode() {
		return parentCode;
	}

	public String getFullName() {
		return fullName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
