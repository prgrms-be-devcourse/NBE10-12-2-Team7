package com.dongnemarket.report.service;

import java.util.Set;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.storage.FileStorageService;
import com.dongnemarket.global.storage.StorageException;
import com.dongnemarket.global.storage.StorageFileNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 신고 증빙 이미지 검증 + 저장을 담당한다. 실제 저장/조회는 {@link FileStorageService}(global/storage)에 위임하는
 * 얇은 어댑터다 — 저장 백엔드(로컬 디스크/S3)는 {@code file.storage.type} 설정으로 갈리며 이 클래스는 그 차이를 모른다.
 * 검증 로직·에러코드 등 report 도메인 정책은 이 클래스가 그대로 소유한다.
 */
@Service
public class EvidenceImageStorageService {

    private static final String DIRECTORY = "report-evidence";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final FileStorageService fileStorageService;

    public EvidenceImageStorageService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public String store(MultipartFile file) {
        validate(file);
        try {
            return fileStorageService.store(file, DIRECTORY);
        } catch (StorageException e) {
            throw new BusinessException(ErrorCode.EVIDENCE_IMAGE_UPLOAD_FAILED);
        }
    }

    public Resource load(String filename) {
        try {
            return fileStorageService.load(filename, DIRECTORY);
        } catch (StorageFileNotFoundException e) {
            throw new BusinessException(ErrorCode.EVIDENCE_IMAGE_NOT_FOUND);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_EVIDENCE_IMAGE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_EVIDENCE_IMAGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_EVIDENCE_IMAGE);
        }
    }
}
