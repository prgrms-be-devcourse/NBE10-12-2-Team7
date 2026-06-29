package com.dongnemarket.admin.dto;

public class AdminMemberStatusUpdateRequest {

    private String status;

    public AdminMemberStatusUpdateRequest() {
    }

    public AdminMemberStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
