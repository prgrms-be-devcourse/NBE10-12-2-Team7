package com.dongnemarket.comment.service;

import com.dongnemarket.comment.dto.CommentCreateRequest;
import com.dongnemarket.comment.dto.CommentResponse;
import com.dongnemarket.comment.dto.CommentUpdateRequest;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.comment.repository.CommentRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;

    public CommentService(CommentRepository commentRepository,
                          ProductRepository productRepository) {
        this.commentRepository = commentRepository;
        this.productRepository = productRepository;
    }

    /** 댓글 작성. 로그인 사용자가 특정 상품에 댓글을 단다. */
    @Transactional
    public CommentResponse create(Long memberId, Long productId, CommentCreateRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        Comment saved = commentRepository.save(Comment.of(memberId, productId, request.getContent()));
        return CommentResponse.from(saved);
    }

    /** 댓글 수정. 작성자 본인만 자신의 댓글 내용을 수정할 수 있다. */
    @Transactional
    public CommentResponse update(Long memberId, Long commentId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.COMMENT_OWNER_ONLY);
        }
        comment.updateContent(request.getContent());
        return CommentResponse.from(comment);
    }

    /** 댓글 삭제. 작성자 본인만 자신의 댓글을 소프트 삭제한다. */
    @Transactional
    public void delete(Long memberId, Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.COMMENT_OWNER_ONLY);
        }
        comment.softDelete();
    }
}
