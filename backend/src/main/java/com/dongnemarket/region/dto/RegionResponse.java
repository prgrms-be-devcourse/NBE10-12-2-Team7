package com.dongnemarket.region.dto;

import com.dongnemarket.region.entity.Region;

public class RegionResponse {

	private final Long regionId;
	private final String name;

	private RegionResponse(Long regionId, String name) {
		this.regionId = regionId;
		this.name = name;
	}

	public static RegionResponse from(Region region) {
		return new RegionResponse(region.getId(), region.getName());
	}

	public Long getRegionId() {
		return regionId;
	}

	public String getName() {
		return name;
	}
}
