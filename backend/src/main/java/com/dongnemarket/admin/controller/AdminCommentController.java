package com.dongnemarket.admin.controller;

import com.dongnemarket.admin.dto.AdminCommentResponse;
import com.dongnemarket.admin.service.AdminCommentService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin - Comment", description = "관리자 댓글 관리 API")
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    @Operation(summary = "댓글 목록 조회", description = "관리자가 삭제 여부와 무관하게 전체 댓글을 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminCommentResponse>>> getComments() {
        List<AdminCommentResponse> responses = adminCommentService.getComments();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "댓글 삭제", description = "관리자가 부적절한 댓글을 소프트 삭제한다(작성자가 아니어도 가능).")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        adminCommentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
