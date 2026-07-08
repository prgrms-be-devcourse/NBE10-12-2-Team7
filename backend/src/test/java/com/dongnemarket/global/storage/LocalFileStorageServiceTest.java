package com.dongnemarket.global.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

	private static final String DIRECTORY = "report-evidence";

	@TempDir
	Path tempDir;

	LocalFileStorageService storageService;

	@BeforeEach
	void setUp() {
		storageService = new LocalFileStorageService(tempDir.toString());
	}

	@Test
	@DisplayName("store()는 directory 하위에 UUID 파일명으로 실제 파일을 만들고, 저장한 파일명을 반환한다")
	void store_createsFileUnderDirectory() throws IOException {
		MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "content".getBytes());

		String filename = storageService.store(file, DIRECTORY);

		assertThat(filename).endsWith(".png");
		assertThat(Files.exists(tempDir.resolve(DIRECTORY).resolve(filename))).isTrue();
	}

	@Test
	@DisplayName("store() 후 load()로 같은 디렉터리·파일명으로 다시 읽을 수 있다")
	void store_thenLoad_returnsSameContent() throws IOException {
		MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "content bytes".getBytes());
		String filename = storageService.store(file, DIRECTORY);

		Resource loaded = storageService.load(filename, DIRECTORY);

		assertThat(loaded.exists()).isTrue();
		assertThat(loaded.getInputStream().readAllBytes()).isEqualTo("content bytes".getBytes());
	}

	@Test
	@DisplayName("서로 다른 directory는 서로 다른 하위 폴더에 저장되어 섞이지 않는다")
	void store_differentDirectories_areIsolated() throws IOException {
		MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "a".getBytes());
		String filename = storageService.store(file, "product-images");

		assertThatThrownBy(() -> storageService.load(filename, "report-evidence"))
				.isInstanceOf(StorageFileNotFoundException.class);
		assertThat(storageService.load(filename, "product-images")).isNotNull();
	}

	@Test
	@DisplayName("존재하지 않는 파일명을 조회하면 StorageFileNotFoundException이 발생한다")
	void load_notFound_throwsException() {
		assertThatThrownBy(() -> storageService.load("does-not-exist.png", DIRECTORY))
				.isInstanceOf(StorageFileNotFoundException.class);
	}

	@Test
	@DisplayName("경로 조작 문자열(../ 등)로 조회하면 저장 디렉터리를 벗어나지 못하고 StorageFileNotFoundException이 발생한다")
	void load_pathTraversalAttempt_throwsException() throws IOException {
		// 저장 디렉터리(tempDir/report-evidence) 바깥에 실제로 파일이 있어도 접근할 수 없어야 한다.
		Path outsideFile = tempDir.resolve("secret.txt");
		Files.writeString(outsideFile, "should not be readable");

		assertThatThrownBy(() -> storageService.load("../secret.txt", DIRECTORY))
				.isInstanceOf(StorageFileNotFoundException.class);
	}
}
