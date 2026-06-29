package com.dongnemarket.admin.controller;

import com.dongnemarket.admin.dto.AdminMemberResponse;
import com.dongnemarket.admin.dto.AdminMemberStatusUpdateRequest;
import com.dongnemarket.admin.service.AdminMemberService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin - Member", description = "관리자 회원 관리 API")
@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    public AdminMemberController(AdminMemberService adminMemberService) {
        this.adminMemberService = adminMemberService;
    }

    @Operation(summary = "회원 목록 조회", description = "관리자가 전체 회원을 상태 무관하게 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminMemberResponse>>> getMembers() {
        List<AdminMemberResponse> responses = adminMemberService.getMembers();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "회원 상세 조회", description = "관리자가 회원 단건 정보를 조회한다.")
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> getMember(@PathVariable Long memberId) {
        AdminMemberResponse response = adminMemberService.getMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "회원 상태 변경", description = "관리자가 회원 상태를 ACTIVE/SUSPENDED/DELETED 로 변경한다.")
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> changeMemberStatus(
            @PathVariable Long memberId,
            @RequestBody AdminMemberStatusUpdateRequest request) {
        AdminMemberResponse response = adminMemberService.changeMemberStatus(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}