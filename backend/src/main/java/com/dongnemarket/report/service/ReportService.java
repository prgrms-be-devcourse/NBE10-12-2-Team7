package com.dongnemarket.report.service;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.report.dto.EvidenceImageUploadResponse;
import com.dongnemarket.report.dto.MemberReportCreateRequest;
import com.dongnemarket.report.dto.MyReportResponse;
import com.dongnemarket.report.dto.ProductReportCreateRequest;
import com.dongnemarket.report.dto.ReportResponse;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportStatus;
import com.dongnemarket.report.repository.ReportRepository;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final EvidenceImageStorageService evidenceImageStorageService;

    public ReportService(ReportRepository reportRepository, MemberRepository memberRepository,
                         ProductRepository productRepository,
                         EvidenceImageStorageService evidenceImageStorageService) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.evidenceImageStorageService = evidenceImageStorageService;
    }

    /** 신고 증빙 이미지를 저장하고, 신고 생성 요청에 그대로 넣을 수 있는 접근 URL을 반환한다. */
    public EvidenceImageUploadResponse uploadEvidenceImage(MultipartFile file) {
        String filename = evidenceImageStorageService.store(file);
        return EvidenceImageUploadResponse.of("/api/reports/evidence-image/" + filename);
    }

    public Resource loadEvidenceImage(String filename) {
        return evidenceImageStorageService.load(filename);
    }

    @Transactional
    public ReportResponse reportProduct(Long reporterId, Long targetProductId,
                                        ProductReportCreateRequest request) {
        Member reporter = memberRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Product product = productRepository.findById(targetProductId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getMember().getId().equals(reporterId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_OWN_PRODUCT);
        }

        if (reportRepository.existsByReporterAndTargetProduct(reporter, product)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        Report report = Report.ofProduct(reporter, product, request.getReason(), request.getContent(),
                request.getEvidenceImageUrl());
        return ReportResponse.from(saveReport(report));
    }

    @Transactional
    public ReportResponse reportMember(Long reporterId, Long targetMemberId,
                                       MemberReportCreateRequest request) {
        Member reporter = memberRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (reporterId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
        }

        Member targetMember = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (reportRepository.existsByReporterAndTargetMember(reporter, targetMember)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        Report report = Report.ofMember(reporter, targetMember, request.getReason(), request.getContent(),
                request.getEvidenceImageUrl());
        return ReportResponse.from(saveReport(report));
    }

    /**
     * 애플리케이션 레벨의 existsBy... 중복 검증은 동시 요청(레이스 컨디션)에서는 뚫릴 수 있다.
     * 이 경우 DB 유니크 제약(uk_reports_reporter_target_product / uk_reports_reporter_target_member)이
     * 최종 방어선 역할을 하며, 그 위반을 동일한 DUPLICATE_REPORT 비즈니스 예외로 변환해 API 응답을 일관되게 만든다.
     */
    private Report saveReport(Report report) {
        try {
            return reportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }
    }

    @Transactional(readOnly = true)
    public List<MyReportResponse> getMyReports(Long reporterId) {
        Member reporter = memberRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return reportRepository.findAllByReporter(reporter).stream()
                .map(MyReportResponse::from)
                .collect(Collectors.toList());
    }

    /** 아직 처리되지 않은(RECEIVED) 본인 신고만 취소(삭제)할 수 있다. */
    @Transactional
    public void cancelReport(Long reporterId, Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        if (!report.getReporter().getId().equals(reporterId)) {
            throw new BusinessException(ErrorCode.REPORT_OWNER_ONLY);
        }
        if (report.getStatus() != ReportStatus.RECEIVED) {
            throw new BusinessException(ErrorCode.CANNOT_CANCEL_REPORT);
        }

        reportRepository.delete(report);
    }
}
