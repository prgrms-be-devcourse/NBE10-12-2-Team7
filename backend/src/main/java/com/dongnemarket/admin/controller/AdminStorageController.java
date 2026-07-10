package com.dongnemarket.admin.controller;

import com.dongnemarket.admin.dto.OrphanDeleteRequest;
import com.dongnemarket.admin.dto.OrphanDeleteResponse;
import com.dongnemarket.admin.dto.OrphanScanResponse;
import com.dongnemarket.admin.service.AdminStorageService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Storage", description = "관리자 저장소 고아파일 관리 API")
@RestController
@RequestMapping("/api/admin/storage")
public class AdminStorageController {

    private final AdminStorageService adminStorageService;

    public AdminStorageController(AdminStorageService adminStorageService) {
        this.adminStorageService = adminStorageService;
    }

    @Operation(summary = "고아파일 목록 조회", description = "DB가 참조하지 않는 저장소 파일을 조회한다. graceHours 이내 파일은 제외.")
    @GetMapping("/orphans")
    public ResponseEntity<ApiResponse<OrphanScanResponse>> getOrphans(
            @RequestParam(defaultValue = "24") long graceHours) {
        OrphanScanResponse response = OrphanScanResponse.of(adminStorageService.scanOrphans(graceHours), graceHours);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "고아파일 삭제", description = "선택한 고아파일을 삭제한다. 삭제 직전 실제 고아·grace 통과 여부를 재확인한다.")
    @DeleteMapping("/orphans")
    public ResponseEntity<ApiResponse<OrphanDeleteResponse>> deleteOrphans(
            @RequestBody OrphanDeleteRequest request,
            @RequestParam(defaultValue = "24") long graceHours) {
        OrphanDeleteResponse response = adminStorageService.deleteOrphans(request, graceHours);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
