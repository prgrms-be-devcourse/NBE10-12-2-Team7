package com.dongnemarket.escrow.controller;

import com.dongnemarket.escrow.dto.EscrowCreateRequest;
import com.dongnemarket.escrow.dto.EscrowResponse;
import com.dongnemarket.escrow.service.EscrowService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Escrow", description = "안심결제(에스크로) 거래 API")
@RestController
@RequestMapping("/api/escrows")
public class EscrowController {

    private final EscrowService escrowService;

    public EscrowController(EscrowService escrowService) {
        this.escrowService = escrowService;
    }

    @Operation(summary = "거래 시작", description = "구매자가 안심결제로 거래를 시작하고 대금을 예치합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<EscrowResponse>> createEscrow(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody EscrowCreateRequest request) {
        EscrowResponse response = escrowService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "거래가 시작되었습니다.", response));
    }

    @Operation(summary = "거래 조회", description = "거래 상세를 조회합니다.")
    @GetMapping("/{escrowId}")
    public ApiResponse<EscrowResponse> getEscrow(@PathVariable Long escrowId) {
        return ApiResponse.success(escrowService.get(escrowId));
    }

    @Operation(summary = "구매확정", description = "구매자가 물건을 확인하고 거래를 확정합니다. 대금이 판매자에게 정산(가정)됩니다.")
    @PostMapping("/{escrowId}/confirm")
    public ApiResponse<EscrowResponse> confirmEscrow(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long escrowId) {
        return ApiResponse.success("구매가 확정되었습니다.", escrowService.confirm(memberId, escrowId));
    }

    @Operation(summary = "거래 취소", description = "구매확정 전 거래를 취소하고 환불(가정)합니다.")
    @PostMapping("/{escrowId}/cancel")
    public ApiResponse<EscrowResponse> cancelEscrow(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long escrowId) {
        return ApiResponse.success("거래가 취소되었습니다.", escrowService.cancel(memberId, escrowId));
    }
}
