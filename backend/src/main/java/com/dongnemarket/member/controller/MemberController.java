package com.dongnemarket.member.controller;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.member.dto.MemberResponse;
import com.dongnemarket.member.dto.MemberUpdateRequest;
import com.dongnemarket.member.dto.PasswordChangeRequest;
import com.dongnemarket.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 API")
@RestController
@RequestMapping("/api/members")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회한다.")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(@AuthenticationPrincipal Long memberId) {
		MemberResponse response = memberService.getMyInfo(memberId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 닉네임을 수정한다.")
	@PatchMapping("/me")
	public ResponseEntity<ApiResponse<MemberResponse>> updateMyInfo(
			@AuthenticationPrincipal Long memberId,
			@Valid @RequestBody MemberUpdateRequest request) {
		MemberResponse response = memberService.updateMyInfo(memberId, request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "비밀번호 변경", description = "현재 로그인한 사용자의 비밀번호를 변경한다. 변경 성공 시 저장된 Refresh Token이 삭제되어 재로그인이 필요하다.")
	@PatchMapping("/me/password")
	public ResponseEntity<ApiResponse<Void>> changePassword(
			@AuthenticationPrincipal Long memberId,
			@Valid @RequestBody PasswordChangeRequest request) {
		memberService.changePassword(memberId, request);
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자의 계정을 탈퇴 처리한다.")
	@DeleteMapping("/me")
	public ResponseEntity<ApiResponse<Void>> deleteMyInfo(@AuthenticationPrincipal Long memberId) {
		memberService.deleteMyInfo(memberId);
		return ResponseEntity.ok(ApiResponse.success());
	}
}
