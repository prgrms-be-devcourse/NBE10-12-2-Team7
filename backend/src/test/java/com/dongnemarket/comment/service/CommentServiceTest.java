package com.dongnemarket.comment.service;

import com.dongnemarket.comment.dto.CommentCreateRequest;
import com.dongnemarket.comment.dto.CommentResponse;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.comment.repository.CommentRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    CommentRepository commentRepository;

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    CommentService commentService;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    @Test
    @DisplayName("상품이 존재하면 댓글 작성에 성공한다")
    void create_success() {
        CommentCreateRequest request = new CommentCreateRequest("좋은 상품이네요");
        given(productRepository.existsById(PRODUCT_ID)).willReturn(true);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.create(MEMBER_ID, PRODUCT_ID, request);

        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(response.getContent()).isEqualTo("좋은 상품이네요");
    }

    @Test
    @DisplayName("존재하지 않는 상품에 댓글을 작성하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void create_productNotFound_throwsException() {
        CommentCreateRequest request = new CommentCreateRequest("좋은 상품이네요");
        given(productRepository.existsById(PRODUCT_ID)).willReturn(false);

        assertThatThrownBy(() -> commentService.create(MEMBER_ID, PRODUCT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(commentRepository, never()).save(any());
    }
}
