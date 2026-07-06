package com.dongnemarket.notification.controller;

import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.notification.dto.NotificationResponse;
import com.dongnemarket.notification.dto.NotificationUnreadCountResponse;
import com.dongnemarket.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notification", description = "알림 API")
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "내 알림 목록 조회", description = "로그인 사용자가 자신의 알림을 최근 발생순으로 조회한다.")
    @GetMapping("/api/notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal Long memberId) {
        List<NotificationResponse> response = notificationService.getMyNotifications(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "안읽은 알림 개수 조회", description = "로그인 사용자의 안읽은 알림 총 개수(안읽은 댓글 알림 + 안읽은 채팅방 수)를 조회한다. 헤더 배지용.")
    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal Long memberId) {
        long count = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(ApiResponse.success(NotificationUnreadCountResponse.of(count)));
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "로그인 사용자가 자신의 안읽은 알림을 모두 읽음 처리한다(알림 패널 열람 시).")
    @PostMapping("/api/notifications/read")
    public ResponseEntity<ApiResponse<Void>> readAll(
            @AuthenticationPrincipal Long memberId) {
        notificationService.markAllRead(memberId);
        return ResponseEntity.ok(ApiResponse.success("알림을 모두 읽음 처리했습니다.", null));
    }
}
