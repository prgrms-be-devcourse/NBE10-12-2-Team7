package com.dongnemarket.report;

import java.math.BigDecimal;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.report.dto.MemberReportCreateRequest;
import com.dongnemarket.report.dto.ProductReportCreateRequest;
import com.dongnemarket.report.dto.ReportResponse;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import com.dongnemarket.report.entity.ReportStatus;
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

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    ReportRepository reportRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    com.dongnemarket.report.service.EvidenceImageStorageService evidenceImageStorageService;

    @InjectMocks
    ReportService reportService;

    @Test
    @DisplayName("로그인 사용자가 상품을 신고하면 RECEIVED 상태로 신고가 저장된다")
    void reportProduct_success() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Product product = createProduct(2L);
        ProductReportCreateRequest request = createProductReportRequest();

        given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));
        given(reportRepository.existsByReporterAndTargetProduct(reporter, product)).willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.reportProduct(reporterId, targetProductId, request);

        assertThat(response.getReportType().name()).isEqualTo("PRODUCT");
        assertThat(response.getReason()).isEqualTo(ReportReason.FAKE_ITEM);
    }

    @Test
    @DisplayName("본인이 등록한 상품을 신고하면 CANNOT_REPORT_OWN_PRODUCT 예외가 발생한다")
    void reportProduct_ownProduct_throwsException() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Product product = createProduct(reporterId);
        ProductReportCreateRequest request = createProductReportRequest();

        given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> reportService.reportProduct(reporterId, targetProductId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REPORT_OWN_PRODUCT);

    }

    @Test
    @DisplayName("existsBy... 통과 후 저장 시점에 DB 유니크 제약을 위반해도 DUPLICATE_REPORT로 변환된다 (동시 요청 대비)")
    void reportProduct_dbUniqueConstraintViolation_convertsToDuplicateReportException() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Product product = createProduct(2L);
        ProductReportCreateRequest request = createProductReportRequest();

        given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));
        given(reportRepository.existsByReporterAndTargetProduct(reporter, product)).willReturn(false);
        given(reportRepository.save(any(Report.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> reportService.reportProduct(reporterId, targetProductId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REPORT);
    }

    @Test
    @DisplayName("같은 상품을 중복 신고하면 DUPLICATE_REPORT 예외가 발생한다")
    void reportProduct_duplicate_throwsException() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Product product = createProduct(2L);
        ProductReportCreateRequest request = createProductReportRequest();

        given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));
        given(reportRepository.existsByReporterAndTargetProduct(reporter, product)).willReturn(true);

        assertThatThrownBy(() -> reportService.reportProduct(reporterId, targetProductId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REPORT);

    }

    @Test
    @DisplayName("증빙 이미지를 첨부해 상품을 신고하면 응답에 이미지 URL이 포함된다")
    void reportProduct_withEvidenceImage_includesUrlInResponse() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Product product = createProduct(2L);
        ProductReportCreateRequest request = createProductReportRequest("https://example.com/evidence.jpg");

        given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));
        given(reportRepository.existsByReporterAndTargetProduct(reporter, product)).willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.reportProduct(reporterId, targetProductId, request);

        assertThat(response.getEvidenceImageUrl()).isEqualTo("https://example.com/evidence.jpg");
    }

    @Test
    @DisplayName("증빙 이미지 없이 신고하면 응답의 이미지 URL은 null이다")
    void reportProduct_withoutEvidenceImage_urlIsNull() {
        Long reporterId = 1L;
        Long targetProductId = 10L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Product product = createProduct(2L);
        ProductReportCreateRequest request = createProductReportRequest();

        given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(productRepository.findById(targetProductId)).willReturn(Optional.of(product));
        given(reportRepository.existsByReporterAndTargetProduct(reporter, product)).willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.reportProduct(reporterId, targetProductId, request);

        assertThat(response.getEvidenceImageUrl()).isNull();
    }

    @Test
    @DisplayName("로그인 사용자가 다른 회원을 신고하면 RECEIVED 상태로 신고가 저장된다")
    void reportMember_success() {
        Long reporterId = 1L;
        Long targetMemberId = 2L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Member targetMember = createMember(targetMemberId, "target@example.com", "신고대상");
        MemberReportCreateRequest request = createMemberReportRequest();

        given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));
        given(reportRepository.existsByReporterAndTargetMember(reporter, targetMember)).willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.reportMember(reporterId, targetMemberId, request);

        assertThat(response.getReportType().name()).isEqualTo("MEMBER");
        assertThat(response.getReason()).isEqualTo(ReportReason.FRAUD_SUSPECTED);
    }

    @Test
    @DisplayName("본인 계정을 신고하면 CANNOT_REPORT_SELF 예외가 발생한다")
    void reportMember_self_throwsException() {
        Long reporterId = 1L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        MemberReportCreateRequest request = createMemberReportRequest();

        given(memberRepository.findById(reporterId)).willReturn(Optional.of(reporter));

        assertThatThrownBy(() -> reportService.reportMember(reporterId, reporterId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REPORT_SELF);

    }

    @Test
    @DisplayName("RECEIVED 상태인 본인 신고를 취소하면 삭제된다")
    void cancelReport_success() {
        Long reporterId = 1L;
        Long reportId = 100L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Report report = Report.ofProduct(reporter, createProduct(2L), ReportReason.FAKE_ITEM, "신고합니다");

        given(reportRepository.findById(reportId)).willReturn(Optional.of(report));

        reportService.cancelReport(reporterId, reportId);

        org.mockito.Mockito.verify(reportRepository).delete(report);
    }

    @Test
    @DisplayName("존재하지 않는 신고를 취소하면 REPORT_NOT_FOUND 예외가 발생한다")
    void cancelReport_notFound_throwsException() {
        Long reporterId = 1L;
        Long reportId = 999L;

        given(reportRepository.findById(reportId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.cancelReport(reporterId, reportId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    @DisplayName("타인의 신고를 취소하려 하면 REPORT_OWNER_ONLY 예외가 발생한다")
    void cancelReport_notOwner_throwsException() {
        Long reporterId = 1L;
        Long otherReporterId = 2L;
        Long reportId = 100L;
        Member otherReporter = createMember(otherReporterId, "other@example.com", "다른신고자");
        Report report = Report.ofProduct(otherReporter, createProduct(3L), ReportReason.FAKE_ITEM, "신고합니다");

        given(reportRepository.findById(reportId)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.cancelReport(reporterId, reportId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_OWNER_ONLY);
    }

    @Test
    @DisplayName("이미 처리 중인 신고를 취소하려 하면 CANNOT_CANCEL_REPORT 예외가 발생한다")
    void cancelReport_notReceivedStatus_throwsException() {
        Long reporterId = 1L;
        Long reportId = 100L;
        Member reporter = createMember(reporterId, "reporter@example.com", "신고자");
        Report report = Report.ofProduct(reporter, createProduct(2L), ReportReason.FAKE_ITEM, "신고합니다");
        report.changeStatus(ReportStatus.REVIEWING);

        given(reportRepository.findById(reportId)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.cancelReport(reporterId, reportId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_CANCEL_REPORT);
    }

    private Member createMember(Long id, String email, String nickname) {
        try {
            Member member = Member.createUser(email, "pw", nickname);
            var idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
            return member;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Product createProduct(Long ownerId) {
        try {
            Member owner = Member.createUser("owner@example.com", "pw", "owner");
            var idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(owner, ownerId);

            Region region = new Region("1168010100", 3, null, "서울특별시 강남구 역삼동", "역삼동");
            return Product.create(owner, null, "테스트 상품", "설명", BigDecimal.valueOf(10000), region);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProductReportCreateRequest createProductReportRequest() {
        return createProductReportRequest(null);
    }

    private ProductReportCreateRequest createProductReportRequest(String evidenceImageUrl) {
        try {
            var constructor = ProductReportCreateRequest.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ProductReportCreateRequest request = constructor.newInstance();
            var reasonField = ProductReportCreateRequest.class.getDeclaredField("reason");
            reasonField.setAccessible(true);
            reasonField.set(request, ReportReason.FAKE_ITEM);
            var evidenceField = ProductReportCreateRequest.class.getDeclaredField("evidenceImageUrl");
            evidenceField.setAccessible(true);
            evidenceField.set(request, evidenceImageUrl);
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
