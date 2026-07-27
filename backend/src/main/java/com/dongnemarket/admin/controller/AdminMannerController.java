package com.dongnemarket.admin.controller;

import com.dongnemarket.admin.dto.AdminMannerResponse;
import com.dongnemarket.admin.service.AdminMannerService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Admin - Manner", description = "관리자 매너온도(저신뢰 회원) 모니터링 API")
@RestController
@RequestMapping("/api/admin/manner-scores")
public class AdminMannerController {

    private final AdminMannerService adminMannerService;

    public AdminMannerController(AdminMannerService adminMannerService) {
        this.adminMannerService = adminMannerService;
    }

    @Operation(summary = "저신뢰 회원 모니터링", description = "매너온도가 threshold 이하인 회원을 낮은 순으로 조회한다(기본 20.0).")
    @GetMapping
    public ApiResponse<List<AdminMannerResponse>> getLowTrustMembers(
            @RequestParam(required = false) BigDecimal threshold) {
        return ApiResponse.success(adminMannerService.getLowTrustMembers(threshold));
    }
}
