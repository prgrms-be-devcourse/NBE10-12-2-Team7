package com.dongnemarket.comment.controller;

import com.dongnemarket.comment.dto.CommentCreateRequest;
import com.dongnemarket.comment.dto.CommentResponse;
import com.dongnemarket.comment.dto.CommentUpdateRequest;
import com.dongnemarket.comment.service.CommentService;
import com.dongnemarket.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comment", description = "댓글 API")
@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "댓글 작성", description = "로그인 사용자가 특정 상품에 댓글을 작성한다.")
    @PostMapping("/api/products/{productId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long productId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.create(memberId, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "댓글이 작성되었습니다.", response));
    }

    @Operation(summary = "댓글 수정", description = "작성자 본인이 자신의 댓글 내용을 수정한다.")
    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        CommentResponse response = commentService.update(memberId, commentId, request);
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", response));
    }
}
