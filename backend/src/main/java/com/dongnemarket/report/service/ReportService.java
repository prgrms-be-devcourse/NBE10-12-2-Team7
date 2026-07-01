package com.dongnemarket.report.service;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.report.dto.MemberReportCreateRequest;
import com.dongnemarket.report.dto.MyReportResponse;
import com.dongnemarket.report.dto.ProductReportCreateRequest;
import com.dongnemarket.report.dto.ReportResponse;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public ReportService(ReportRepository reportRepository, MemberRepository memberRepository,
                         ProductRepository productRepository) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
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

        Report report = Report.ofProduct(reporter, product, request.getReason(), request.getContent());
        return ReportResponse.from(reportRepository.save(report));
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

        Report report = Report.ofMember(reporter, targetMember, request.getReason(), request.getContent());
        return ReportResponse.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<MyReportResponse> getMyReports(Long reporterId) {
        Member reporter = memberRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return reportRepository.findAllByReporter(reporter).stream()
                .map(MyReportResponse::from)
                .collect(Collectors.toList());
    }
}
