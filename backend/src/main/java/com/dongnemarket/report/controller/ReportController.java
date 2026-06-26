package com.dongnemarket.report.controller;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.report.dto.MemberReportCreateRequest;
import com.dongnemarket.report.dto.MyReportResponse;
import com.dongnemarket.report.dto.ProductReportCreateRequest;
import com.dongnemarket.report.dto.ReportResponse;
import com.dongnemarket.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Report", description = "신고 API")
@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "상품 신고", description = "부적절한 상품을 신고한다.")
    @PostMapping("/api/products/{productId}/reports")
    public ResponseEntity<ApiResponse<ReportResponse>> reportProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductReportCreateRequest request,
            @AuthenticationPrincipal Long reporterId) {
        ReportResponse response = reportService.reportProduct(reporterId, productId, request);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(201, "신고가 접수되었습니다.", response));
    }

    @Operation(summary = "사용자 신고", description = "부적절한 사용자를 신고한다.")
    @PostMapping("/api/members/{memberId}/reports")
    public ResponseEntity<ApiResponse<ReportResponse>> reportMember(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberReportCreateRequest request,
            @AuthenticationPrincipal Long reporterId) {
        ReportResponse response = reportService.reportMember(reporterId, memberId, request);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(201, "신고가 접수되었습니다.", response));
    }

    @Operation(summary = "내 신고 내역 조회", description = "내가 작성한 신고 목록을 조회한다.")
    @GetMapping("/api/members/me/reports")
    public ResponseEntity<ApiResponse<List<MyReportResponse>>> getMyReports(
            @AuthenticationPrincipal Long reporterId) {
        List<MyReportResponse> response = reportService.getMyReports(reporterId);
        return ResponseEntity.ok(ApiResponse.success("목록 조회에 성공했습니다.", response));
    }
}
