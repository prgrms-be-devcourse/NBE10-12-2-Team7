package com.dongnemarket.report.dto;

import com.dongnemarket.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MemberReportCreateRequest {

    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReason reason;

    @Size(max = 500, message = "신고 내용은 500자 이하여야 합니다.")
    private String content;

    @Size(max = 500, message = "증빙 이미지 URL은 500자 이하여야 합니다.")
    private String evidenceImageUrl;

    protected MemberReportCreateRequest() {}

    public ReportReason getReason() { return reason; }
    public String getContent() { return content; }
    public String getEvidenceImageUrl() { return evidenceImageUrl; }
}
