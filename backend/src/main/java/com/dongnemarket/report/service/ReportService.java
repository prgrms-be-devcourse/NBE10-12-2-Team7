package com.dongnemarket.report.service;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
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

    public ReportService(ReportRepository reportRepository, MemberRepository memberRepository) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public ReportResponse reportProduct(Long reporterId, Long targetProductId,
                                        ProductReportCreateRequest request) {
        validateMemberExists(reporterId);

        if (reportRepository.existsByReporterIdAndTargetProductId(reporterId, targetProductId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        Report report = Report.ofProduct(reporterId, targetProductId,
                request.getReason(), request.getContent());
        return ReportResponse.from(reportRepository.save(report));
    }

    @Transactional
    public ReportResponse reportMember(Long reporterId, Long targetMemberId,
                                       MemberReportCreateRequest request) {
        validateMemberExists(reporterId);

        if (reporterId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
        }

        memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (reportRepository.existsByReporterIdAndTargetMemberId(reporterId, targetMemberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        Report report = Report.ofMember(reporterId, targetMemberId,
                request.getReason(), request.getContent());
        return ReportResponse.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<MyReportResponse> getMyReports(Long reporterId) {
        validateMemberExists(reporterId);
        return reportRepository.findAllByReporterId(reporterId).stream()
                .map(MyReportResponse::from)
                .collect(Collectors.toList());
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
