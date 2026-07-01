package com.dongnemarket.report;

import java.math.BigDecimal;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.report.dto.MemberReportCreateRequest;
import com.dongnemarket.report.dto.ProductReportCreateRequest;
import com.dongnemarket.report.dto.ReportResponse;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import com.dongnemarket.report.repository.ReportRepository;
import com.dongnemarket.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    ReportRepository reportRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ReportService reportService;

    @Test
    @DisplayName("로그인 사용자가 상품을 신고하면 RECEIVED 상태로 신고가 저장된다")
    void reportProduct_success() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        Long ownerId = 2L;
        ProductReportCreateRequest request = createProductReportRequest();
        Product product = createProduct(ownerId);

        given(memberRepository.existsById(reporterId)).willReturn(true);
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));
        given(reportRepository.existsByReporterIdAndTargetProductId(reporterId, targetProductId)).willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.reportProduct(reporterId, targetProductId, request);

        assertThat(response.getReportType().name()).isEqualTo("PRODUCT");
        assertThat(response.getReason()).isEqualTo(ReportReason.FAKE_ITEM);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 신고하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void reportProduct_productNotFound_throwsException() {
        Long reporterId = 1L;
        Long targetProductId = 999L;
        ProductReportCreateRequest request = createProductReportRequest();

        given(memberRepository.existsById(reporterId)).willReturn(true);
        given(productRepository.findById(targetProductId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.reportProduct(reporterId, targetProductId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인이 등록한 상품을 신고하면 CANNOT_REPORT_OWN_PRODUCT 예외가 발생한다")
    void reportProduct_ownProduct_throwsException() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        ProductReportCreateRequest request = createProductReportRequest();
        Product product = createProduct(reporterId);

        given(memberRepository.existsById(reporterId)).willReturn(true);
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> reportService.reportProduct(reporterId, targetProductId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REPORT_OWN_PRODUCT);

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 상품을 중복 신고하면 DUPLICATE_REPORT 예외가 발생한다")
    void reportProduct_duplicate_throwsException() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        Long ownerId = 2L;
        ProductReportCreateRequest request = createProductReportRequest();
        Product product = createProduct(ownerId);

        given(memberRepository.existsById(reporterId)).willReturn(true);
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));
        given(reportRepository.existsByReporterIdAndTargetProductId(reporterId, targetProductId)).willReturn(true);

        assertThatThrownBy(() -> reportService.reportProduct(reporterId, targetProductId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REPORT);

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 회원이 상품을 신고하면 MEMBER_NOT_FOUND 예외가 발생한다")
    void reportProduct_reporterNotFound_throwsException() {
        Long reporterId = 999L;
        Long targetProductId = 10L;
        ProductReportCreateRequest request = createProductReportRequest();

        given(memberRepository.existsById(reporterId)).willReturn(false);

        assertThatThrownBy(() -> reportService.reportProduct(reporterId, targetProductId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 사용자가 다른 회원을 신고하면 RECEIVED 상태로 신고가 저장된다")
    void reportMember_success() {
        Long reporterId = 1L;
        Long targetMemberId = 2L;
        MemberReportCreateRequest request = createMemberReportRequest();
        Member targetMember = Member.createUser("target@example.com", "pw", "targetNick");

        given(memberRepository.existsById(reporterId)).willReturn(true);
        given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));
        given(reportRepository.existsByReporterIdAndTargetMemberId(reporterId, targetMemberId)).willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.reportMember(reporterId, targetMemberId, request);

        assertThat(response.getReportType().name()).isEqualTo("MEMBER");
        assertThat(response.getReason()).isEqualTo(ReportReason.FRAUD_SUSPECTED);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("본인 계정을 신고하면 CANNOT_REPORT_SELF 예외가 발생한다")
    void reportMember_self_throwsException() {
        Long reporterId = 1L;
        MemberReportCreateRequest request = createMemberReportRequest();

        given(memberRepository.existsById(reporterId)).willReturn(true);

        assertThatThrownBy(() -> reportService.reportMember(reporterId, reporterId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REPORT_SELF);

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 회원을 신고하면 MEMBER_NOT_FOUND 예외가 발생한다")
    void reportMember_targetNotFound_throwsException() {
        Long reporterId = 1L;
        Long targetMemberId = 999L;
        MemberReportCreateRequest request = createMemberReportRequest();

        given(memberRepository.existsById(reporterId)).willReturn(true);
        given(memberRepository.findById(targetMemberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.reportMember(reporterId, targetMemberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        verify(reportRepository, never()).save(any());
    }

    private Product createProduct(Long ownerId) {
        try {
            Member owner = Member.createUser("owner@example.com", "pw", "owner");
            var idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(owner, ownerId);

            Product product = Product.create(owner, null, "테스트 상품", "설명", BigDecimal.valueOf(10000), "서울");
            return product;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProductReportCreateRequest createProductReportRequest() {
        try {
            var constructor = ProductReportCreateRequest.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ProductReportCreateRequest request = constructor.newInstance();
            var reasonField = ProductReportCreateRequest.class.getDeclaredField("reason");
            reasonField.setAccessible(true);
            reasonField.set(request, ReportReason.FAKE_ITEM);
            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MemberReportCreateRequest createMemberReportRequest() {
        try {
            var constructor = MemberReportCreateRequest.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MemberReportCreateRequest request = constructor.newInstance();
            var reasonField = MemberReportCreateRequest.class.getDeclaredField("reason");
            reasonField.setAccessible(true);
            reasonField.set(request, ReportReason.FRAUD_SUSPECTED);
            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
