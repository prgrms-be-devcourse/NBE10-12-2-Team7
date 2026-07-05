package com.dongnemarket.report.controller;

import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.global.response.ErrorResponse;
import com.dongnemarket.report.dto.EvidenceImageUploadResponse;
import com.dongnemarket.report.dto.MemberReportCreateRequest;
import com.dongnemarket.report.dto.MyReportResponse;
import com.dongnemarket.report.dto.ProductReportCreateRequest;
import com.dongnemarket.report.dto.ReportResponse;
import com.dongnemarket.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
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

    @Operation(summary = "신고 취소", description = "아직 처리되지 않은(RECEIVED) 내 신고를 취소한다.")
    @DeleteMapping("/api/members/me/reports/{reportId}")
    public ResponseEntity<ApiResponse<Void>> cancelReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal Long reporterId) {
        reportService.cancelReport(reporterId, reportId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "신고 증빙 이미지 업로드", description = "신고 작성 시 첨부할 증빙 이미지를 업로드하고 접근 URL을 반환한다.")
    @PostMapping(value = "/api/reports/evidence-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EvidenceImageUploadResponse>> uploadEvidenceImage(
            @RequestPart("file") MultipartFile file) {
        EvidenceImageUploadResponse response = reportService.uploadEvidenceImage(file);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(201, "이미지를 업로드했습니다.", response));
    }

    @Operation(summary = "신고 증빙 이미지 조회", description = "업로드된 신고 증빙 이미지 파일을 반환한다.")
    @GetMapping("/api/reports/evidence-image/{filename}")
    public ResponseEntity<Resource> getEvidenceImage(@PathVariable String filename) {
        Resource resource = reportService.loadEvidenceImage(filename);
        String contentType = URLConnection.guessContentTypeFromName(filename);
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    /**
     * spring.servlet.multipart.max-file-size 초과 시 서블릿 계층에서 컨트롤러 메서드 진입 전에 던져지는 예외.
     * 전역 예외 처리기(GlobalExceptionHandler, 공통 영역)를 건드리지 않기 위해 이 컨트롤러에 국한해 처리한다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.INVALID_EVIDENCE_IMAGE.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_EVIDENCE_IMAGE));
    }
}
