package com.dongnemarket.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

class EvidenceImageStorageServiceTest {

    @TempDir
    Path tempDir;

    EvidenceImageStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new EvidenceImageStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("이미지 파일을 저장하면 저장 디렉터리에 실제 파일이 생기고, 저장된 이름으로 다시 읽을 수 있다")
    void store_and_load_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evidence.png", "image/png", "fake-image-bytes".getBytes());

        String filename = storageService.store(file);

        assertThat(filename).endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();

        Resource loaded = storageService.load(filename);
        assertThat(loaded.exists()).isTrue();
        assertThat(loaded.getInputStream().readAllBytes()).isEqualTo("fake-image-bytes".getBytes());
    }

    @Test
    @DisplayName("빈 파일을 업로드하면 INVALID_EVIDENCE_IMAGE 예외가 발생한다")
    void store_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EVIDENCE_IMAGE);
    }

    @Test
    @DisplayName("이미지가 아닌 파일(content-type)을 업로드하면 INVALID_EVIDENCE_IMAGE 예외가 발생한다")
    void store_nonImageContentType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "not an image".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EVIDENCE_IMAGE);
    }

    @Test
    @DisplayName("5MB를 초과하는 파일을 업로드하면 INVALID_EVIDENCE_IMAGE 예외가 발생한다")
    void store_tooLarge_throwsException() {
        byte[] tooLarge = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", tooLarge);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EVIDENCE_IMAGE);
    }

    @Test
    @DisplayName("존재하지 않는 파일명을 조회하면 EVIDENCE_IMAGE_NOT_FOUND 예외가 발생한다")
    void load_notFound_throwsException() {
        assertThatThrownBy(() -> storageService.load("does-not-exist.png"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EVIDENCE_IMAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("경로 조작 문자열(../ 등)로 조회하면 저장 디렉터리를 벗어나지 못하고 EVIDENCE_IMAGE_NOT_FOUND 예외가 발생한다")
    void load_pathTraversalAttempt_throwsException() throws IOException {
        // 저장 디렉터리 바깥(부모 디렉터리)에 실제로 파일이 있어도 접근할 수 없어야 한다.
        Path outsideFile = tempDir.getParent().resolve("secret.txt");
        Files.writeString(outsideFile, "should not be readable");

        assertThatThrownBy(() -> storageService.load("../secret.txt"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EVIDENCE_IMAGE_NOT_FOUND);
    }
}
