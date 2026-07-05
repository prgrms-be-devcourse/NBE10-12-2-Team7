package com.dongnemarket.report.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 신고 증빙 이미지를 로컬 디스크에 저장/조회한다.
 * 저장 경로는 {@code report.evidence-image.storage-path}로 설정하며,
 * local(도커) 프로파일에서는 컨테이너 재시작에도 유지되도록 이 경로에 볼륨을 마운트한다(docker-compose.yml 참고).
 */
@Service
public class EvidenceImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final Path storageDir;

    public EvidenceImageStorageService(@Value("${report.evidence-image.storage-path}") String storagePath) {
        this.storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("증빙 이미지 저장 디렉터리를 생성할 수 없습니다: " + this.storageDir, e);
        }
    }

    public String store(MultipartFile file) {
        validate(file);
        String filename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        Path target = storageDir.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.EVIDENCE_IMAGE_UPLOAD_FAILED);
        }
        return filename;
    }

    public Resource load(String filename) {
        Path target = storageDir.resolve(filename).normalize();
        // 경로 조작(path traversal) 방지: 정규화한 경로가 반드시 저장 디렉터리 내부여야 한다.
        if (!target.startsWith(storageDir)) {
            throw new BusinessException(ErrorCode.EVIDENCE_IMAGE_NOT_FOUND);
        }

        Resource resource = new FileSystemResource(target);
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(ErrorCode.EVIDENCE_IMAGE_NOT_FOUND);
        }
        return resource;
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

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int idx = originalFilename.lastIndexOf('.');
        return idx >= 0 ? originalFilename.substring(idx) : "";
    }
}
