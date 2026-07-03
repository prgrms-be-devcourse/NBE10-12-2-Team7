package com.dongnemarket.member.controller;

import java.util.List;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.member.dto.MemberLocationResponse;
import com.dongnemarket.member.dto.MemberLocationUpdateRequest;
import com.dongnemarket.member.service.MemberLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member Location", description = "회원 동네 API")
@RestController
@RequestMapping("/api/members/me/locations")
public class MemberLocationController {

	private final MemberLocationService memberLocationService;

	public MemberLocationController(MemberLocationService memberLocationService) {
		this.memberLocationService = memberLocationService;
	}

	@Operation(summary = "내 동네 설정", description = "현재 로그인한 사용자의 동네 목록을 전체 교체 방식으로 설정한다.")
	@PutMapping
	public ResponseEntity<ApiResponse<List<MemberLocationResponse>>> updateMyLocations(
			@AuthenticationPrincipal Long memberId,
			@Valid @RequestBody MemberLocationUpdateRequest request) {
		List<MemberLocationResponse> response = memberLocationService.updateMyLocations(memberId, request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "내 동네 조회", description = "현재 로그인한 사용자의 동네 목록을 조회한다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<MemberLocationResponse>>> getMyLocations(
			@AuthenticationPrincipal Long memberId) {
		List<MemberLocationResponse> response = memberLocationService.getMyLocations(memberId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
