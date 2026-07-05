package com.dongnemarket.report.dto;

public class EvidenceImageUploadResponse {

    private final String evidenceImageUrl;

    private EvidenceImageUploadResponse(String evidenceImageUrl) {
        this.evidenceImageUrl = evidenceImageUrl;
    }

    public static EvidenceImageUploadResponse of(String evidenceImageUrl) {
        return new EvidenceImageUploadResponse(evidenceImageUrl);
    }

    public String getEvidenceImageUrl() {
        return evidenceImageUrl;
    }
}
