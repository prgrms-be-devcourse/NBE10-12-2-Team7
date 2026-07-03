package com.dongnemarket.region.controller;

import java.util.List;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.region.dto.RegionResponse;
import com.dongnemarket.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Region", description = "지역 API")
@RestController
@RequestMapping("/api/regions")
public class RegionController {

	private final RegionService regionService;

	public RegionController(RegionService regionService) {
		this.regionService = regionService;
	}

	@Operation(summary = "지역 목록 조회", description = "상품 등록과 지역 필터에 사용할 지역 목록을 조회합니다.")
	@GetMapping
	public ApiResponse<List<RegionResponse>> getRegions() {
		return ApiResponse.success(regionService.getRegions());
	}
}
