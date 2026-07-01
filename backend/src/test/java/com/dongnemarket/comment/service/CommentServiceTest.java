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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    CommentRepository commentRepository;

    @Mock
    ProductService productService;

    @Mock
    EntityManager entityManager;

    @InjectMocks
    CommentService commentService;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long COMMENT_ID = 10L;

    /** 작성자 식별자만 스텁한 댓글 엔티티(소유자 검증·응답 변환용). */
    private Comment commentByMember(Long memberId, String content) {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(memberId);
        return Comment.of(member, mock(Product.class), content);
    }

    @Test
    @DisplayName("접근 가능한 상품이면 댓글 작성에 성공한다")
    void create_success() {
        CommentCreateRequest request = new CommentCreateRequest("좋은 상품이네요");
        Member member = mock(Member.class);
        Product product = mock(Product.class);
        given(member.getId()).willReturn(MEMBER_ID);
        given(product.getId()).willReturn(PRODUCT_ID);
        given(entityManager.find(Member.class, MEMBER_ID)).willReturn(member);
        given(entityManager.find(Product.class, PRODUCT_ID)).willReturn(product);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.create(MEMBER_ID, PRODUCT_ID, request);

        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(response.getContent()).isEqualTo("좋은 상품이네요");
    }

    @Test
    @DisplayName("접근 불가(존재하지 않거나 삭제·숨김) 상품에 댓글을 작성하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void create_productNotAccessible_throwsException() {
        CommentCreateRequest request = new CommentCreateRequest("좋은 상품이네요");
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(productService).validateAccessibleProduct(PRODUCT_ID);

        assertThatThrownBy(() -> commentService.create(MEMBER_ID, PRODUCT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("접근 가능한 상품이면 삭제되지 않은 댓글 목록을 반환한다")
    void getComments_success() {
        // 스텁 진행 중 중첩 스텁을 피하기 위해 목록을 먼저 구성한다.
        List<Comment> comments = List.of(
                commentByMember(MEMBER_ID, "첫 번째 댓글"),
                commentByMember(2L, "두 번째 댓글"));
        given(commentRepository.findAllByProduct_IdAndDeletedAtIsNullOrderByCreatedAtAsc(PRODUCT_ID))
                .willReturn(comments);

        List<CommentResponse> responses = commentService.getComments(PRODUCT_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CommentResponse::getContent)
                .containsExactly("첫 번째 댓글", "두 번째 댓글");
    }

    @Test
    @DisplayName("댓글이 없는 상품을 조회하면 빈 목록을 반환한다")
    void getComments_empty() {
        given(commentRepository.findAllByProduct_IdAndDeletedAtIsNullOrderByCreatedAtAsc(PRODUCT_ID))
                .willReturn(List.of());

        List<CommentResponse> responses = commentService.getComments(PRODUCT_ID);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("접근 불가(존재하지 않거나 삭제·숨김) 상품의 댓글을 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void getComments_productNotAccessible_throwsException() {
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(productService).validateAccessibleProduct(PRODUCT_ID);

        assertThatThrownBy(() -> commentService.getComments(PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(commentRepository, never()).findAllByProduct_IdAndDeletedAtIsNullOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("작성자 본인이면 댓글 수정에 성공한다")
    void update_success() {
        Comment comment = commentByMember(MEMBER_ID, "원본 내용");
        given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.of(comment));

        CommentResponse response = commentService.update(MEMBER_ID, COMMENT_ID, new CommentUpdateRequest("수정된 내용"));

        assertThat(response.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 댓글을 수정하면 COMMENT_NOT_FOUND 예외가 발생한다")
    void update_notFound_throwsException() {
        given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(MEMBER_ID, COMMENT_ID, new CommentUpdateRequest("수정")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 아니면 댓글 수정 시 COMMENT_OWNER_ONLY 예외가 발생한다")
    void update_notOwner_throwsException() {
        Comment othersComment = commentByMember(999L, "남의 댓글");
        given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.of(othersComment));

        assertThatThrownBy(() -> commentService.update(MEMBER_ID, COMMENT_ID, new CommentUpdateRequest("수정")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_OWNER_ONLY);
    }

    @Test
    @DisplayName("작성자 본인이면 댓글 삭제(소프트)에 성공한다")
    void delete_success() {
        Comment comment = commentByMember(MEMBER_ID, "삭제될 댓글");
        given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.of(comment));

        commentService.delete(MEMBER_ID, COMMENT_ID);

        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않거나 이미 삭제된 댓글을 삭제하면 COMMENT_NOT_FOUND 예외가 발생한다")
    void delete_notFound_throwsException() {
        given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(MEMBER_ID, COMMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 아니면 댓글 삭제 시 COMMENT_OWNER_ONLY 예외가 발생한다")
    void delete_notOwner_throwsException() {
        Comment othersComment = commentByMember(999L, "남의 댓글");
        given(commentRepository.findByIdAndDeletedAtIsNull(COMMENT_ID)).willReturn(Optional.of(othersComment));

        assertThatThrownBy(() -> commentService.delete(MEMBER_ID, COMMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_OWNER_ONLY);
    }
}
