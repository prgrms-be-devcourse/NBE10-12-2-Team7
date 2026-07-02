package com.dongnemarket.admin.ai.tool;

import com.dongnemarket.admin.dto.AdminReportResponse;
import com.dongnemarket.admin.service.AdminReportService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 신고 조회 Tool (읽기 전용). 기존 AdminReportService에 위임만 한다.
 */
@Component
public class AdminReportTools {

    private final AdminReportService adminReportService;

    public AdminReportTools(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @Tool(description = "전체 신고 목록을 조회한다. 상태(RECEIVED/REVIEWING/COMPLETED/REJECTED)와 무관하게 모든 신고가 반환된다. "
            + "접수 대기(RECEIVED), 검토 중(REVIEWING) 등 특정 상태만 필요하면 이 목록에서 status 값으로 걸러서 답하라.")
    public List<AdminReportResponse> listReports() {
        return adminReportService.getReports();
    }

    @Tool(description = "신고 ID(숫자)로 신고 한 건의 상세 정보를 조회한다.")
    public AdminReportResponse getReport(
            @ToolParam(description = "조회할 신고의 ID (양의 정수)") Long reportId) {
        return adminReportService.getReport(reportId);
    }
}
