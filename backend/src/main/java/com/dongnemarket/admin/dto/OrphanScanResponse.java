package com.dongnemarket.admin.dto;

import java.util.List;

public record OrphanScanResponse(
        List<OrphanFileResponse> orphans,
        int totalCount,
        long totalBytes,
        long graceHours) {

    public static OrphanScanResponse of(List<OrphanFileResponse> orphans, long graceHours) {
        long totalBytes = orphans.stream().mapToLong(OrphanFileResponse::sizeBytes).sum();
        return new OrphanScanResponse(orphans, orphans.size(), totalBytes, graceHours);
    }
}
