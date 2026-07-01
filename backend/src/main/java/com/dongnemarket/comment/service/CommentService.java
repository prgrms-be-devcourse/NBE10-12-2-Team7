package com.dongnemarket.comment.service;

import com.dongnemarket.comment.dto.CommentCreateRequest;
import com.dongnemarket.comment.dto.CommentResponse;
import com.dongnemarket.comment.dto.CommentUpdateRequest;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.comment.repository.CommentRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.service.ProductService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProductService productService;
    private final EntityManager entityManager;

    public CommentService(CommentRepository commentRepository,
                          ProductService productService,
                          EntityManager entityManager) {
        this.commentRepository = commentRepository;
        this.productService = productService;
        this.entityManager = entityManager;
    }

    /** 댓글 작성. 로그인 사용자가 접근 가능한 상품에 댓글을 단다. */
    @Transactional
    public CommentResponse create(Long memberId, Long productId, CommentCreateRequest request) {
        productService.validateAccessibleProduct(productId);

        Member member = entityManager.find(Member.class, memberId);
        Product product = entityManager.find(Product.class, productId);
        Comment saved = commentRepository.save(Comment.of(member, product, request.getContent()));
        return CommentResponse.from(saved);
    }

    /** 댓글 목록 조회. 접근 가능한 상품의 삭제되지 않은 댓글을 조회한다. 비로그인도 가능하다. */
    public List<CommentResponse> getComments(Long productId) {
        productService.validateAccessibleProduct(productId);
        return commentRepository.findAllByProduct_IdAndDeletedAtIsNullOrderByCreatedAtAsc(productId).stream()
                .map(CommentResponse::from)
                .toList();
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
