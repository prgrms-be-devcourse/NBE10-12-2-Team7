package com.dongnemarket.admin.dto;

public class AdminReportStatusUpdateRequest {

    private String status;

    public AdminReportStatusUpdateRequest() {
    }

    public AdminReportStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
