package com.dongnemarket.admin.controller;

import com.dongnemarket.admin.dto.AdminReportResponse;
import com.dongnemarket.admin.dto.AdminReportStatusUpdateRequest;
import com.dongnemarket.admin.service.AdminReportService;
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

@Tag(name = "Admin - Report", description = "관리자 신고 관리 API")
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @Operation(summary = "신고 목록 조회", description = "관리자가 전체 신고 내역을 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminReportResponse>>> getReports() {
        List<AdminReportResponse> responses = adminReportService.getReports();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "신고 상세 조회", description = "관리자가 신고 단건 정보를 조회한다.")
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<AdminReportResponse>> getReport(@PathVariable Long reportId) {
        AdminReportResponse response = adminReportService.getReport(reportId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "신고 상태 변경", description = "관리자가 신고 상태를 RECEIVED/REVIEWING/COMPLETED/REJECTED 로 변경한다.")
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<ApiResponse<AdminReportResponse>> changeReportStatus(
            @PathVariable Long reportId,
            @RequestBody AdminReportStatusUpdateRequest request) {
        AdminReportResponse response = adminReportService.changeReportStatus(reportId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
