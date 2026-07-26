package com.dongnemarket.region.controller;

import java.util.List;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.region.dto.RegionResponse;
import com.dongnemarket.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Region", description = "지역 API")
@RestController
@RequestMapping("/api/regions")
public class RegionController {

	private final RegionService regionService;

	public RegionController(RegionService regionService) {
		this.regionService = regionService;
	}

	@Operation(summary = "지역 계단식 조회",
			description = "parentId가 없으면 최상위(시도) 목록, 있으면 해당 지역의 자식 목록을 조회합니다. "
					+ "응답의 level이 3이면 말단(동)입니다.")
	@GetMapping
	public ApiResponse<List<RegionResponse>> getRegions(
			@RequestParam(required = false) Long parentId) {
		return ApiResponse.success(regionService.getRegions(parentId));
	}
}
