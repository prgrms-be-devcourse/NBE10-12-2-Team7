package com.dongnemarket.trade.controller;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.trade.dto.MonthlyTradeStatsResponse;
import com.dongnemarket.trade.dto.TradePurchaseResponse;
import com.dongnemarket.trade.dto.TradeSaleResponse;
import com.dongnemarket.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Trade", description = "거래내역 API")
@RestController
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @Operation(summary = "판매내역 조회", description = "내가 판매자로서 거래완료한 상품 목록을 조회한다.")
    @GetMapping("/api/members/me/trades/sales")
    public ResponseEntity<ApiResponse<List<TradeSaleResponse>>> getSales(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.getSales(memberId)));
    }

    @Operation(summary = "구매내역 조회", description = "내가 구매자로서 거래완료한 상품 목록을 조회한다.")
    @GetMapping("/api/members/me/trades/purchases")
    public ResponseEntity<ApiResponse<List<TradePurchaseResponse>>> getPurchases(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.getPurchases(memberId)));
    }

    @Operation(summary = "월별 거래 통계 조회", description = "월별(yyyy-MM) 판매/구매 건수·금액 통계를 조회한다.")
    @GetMapping("/api/members/me/trades/monthly-stats")
    public ResponseEntity<ApiResponse<List<MonthlyTradeStatsResponse>>> getMonthlyStats(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(tradeService.getMonthlyStats(memberId)));
    }
}
