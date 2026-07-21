package com.dongnemarket.manner.controller;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.manner.dto.MannerScoreHistoryResponse;
import com.dongnemarket.manner.dto.MannerScoreResponse;
import com.dongnemarket.manner.service.MannerScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "MannerScore", description = "매너온도 조회 API")
@RestController
public class MannerScoreController {

    private final MannerScoreService mannerScoreService;

    public MannerScoreController(MannerScoreService mannerScoreService) {
        this.mannerScoreService = mannerScoreService;
    }

    @Operation(summary = "회원 매너온도 조회", description = "상품 상세·채팅방 등에서 노출할 공개 매너온도를 조회한다.")
    @GetMapping("/api/members/{memberId}/manner-score")
    public ApiResponse<MannerScoreResponse> getScore(@PathVariable Long memberId) {
        return ApiResponse.success(MannerScoreResponse.from(mannerScoreService.getOrCreate(memberId)));
    }

    @Operation(summary = "내 매너온도 변화 이력 조회", description = "내정보 페이지의 매너온도 변화 이력 타임라인용 목록을 조회한다.")
    @GetMapping("/api/members/me/manner-score/history")
    public ApiResponse<List<MannerScoreHistoryResponse>> getMyHistory(@AuthenticationPrincipal Long memberId) {
        List<MannerScoreHistoryResponse> response = mannerScoreService.getHistory(memberId).stream()
                .map(MannerScoreHistoryResponse::from)
                .toList();
        return ApiResponse.success(response);
    }
}
