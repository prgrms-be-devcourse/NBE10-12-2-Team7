package com.dongnemarket.admin.dto;

import java.time.Instant;

public record OrphanFileResponse(
        String directory,
        String filename,
        long sizeBytes,
        Instant lastModified,
        long ageHours) {
}