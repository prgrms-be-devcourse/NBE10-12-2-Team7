package com.dongnemarket.product.service;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageStorageServiceTest {

	@TempDir
	Path tempDir;

	@Test
	@DisplayName("이미지 파일을 저장하면 상품 이미지 URL 목록을 반환한다")
	void storesProductImagesAndReturnsUrls() throws Exception {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());
		MockMultipartFile firstFile = imageFile("first.jpg", "image/jpeg", "first image");
		MockMultipartFile secondFile = imageFile("second.png", "image/png", "second image");

		List<String> imageUrls = storageService.store(List.of(firstFile, secondFile));

		assertThat(imageUrls).hasSize(2);
		assertThat(imageUrls).allMatch(url -> url.startsWith("/api/products/images/"));
		for (String imageUrl : imageUrls) {
			String filename = imageUrl.substring("/api/products/images/".length());
			assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
		}
	}

	@Test
	@DisplayName("저장된 상품 이미지를 Resource로 조회할 수 있다")
	void loadsStoredProductImage() throws Exception {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());
		List<String> imageUrls = storageService.store(List.of(imageFile("product.webp", "image/webp", "image content")));
		String filename = imageUrls.get(0).substring("/api/products/images/".length());

		Resource resource = storageService.load(filename);

		assertThat(resource.exists()).isTrue();
		assertThat(resource.getContentAsByteArray()).isEqualTo("image content".getBytes());
	}

	@Test
	@DisplayName("이미지 파일이 없으면 저장할 수 없다")
	void throwsInvalidInputWhenFilesAreEmpty() {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());

		assertInvalidInput(() -> storageService.store(List.of()));
	}

	@Test
	@DisplayName("상품 이미지는 최대 5장까지만 저장할 수 있다")
	void throwsInvalidInputWhenFilesAreMoreThanFive() {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());
		List<MultipartFile> files = List.of(
				imageFile("1.jpg", "image/jpeg", "1"),
				imageFile("2.jpg", "image/jpeg", "2"),
				imageFile("3.jpg", "image/jpeg", "3"),
				imageFile("4.jpg", "image/jpeg", "4"),
				imageFile("5.jpg", "image/jpeg", "5"),
				imageFile("6.jpg", "image/jpeg", "6")
		);

		assertInvalidInput(() -> storageService.store(files));
	}

	@Test
	@DisplayName("빈 파일은 저장할 수 없다")
	void throwsInvalidInputWhenFileIsEmpty() {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());
		MockMultipartFile emptyFile = new MockMultipartFile("files", "empty.png", "image/png", new byte[0]);

		assertInvalidInput(() -> storageService.store(List.of(emptyFile)));
	}

	@Test
	@DisplayName("이미지가 아닌 파일은 저장할 수 없다")
	void throwsInvalidInputWhenFileIsNotImage() {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());
		MockMultipartFile textFile = new MockMultipartFile("files", "memo.txt", "text/plain", "memo".getBytes());

		assertInvalidInput(() -> storageService.store(List.of(textFile)));
	}

	@Test
	@DisplayName("5MB를 초과하는 파일은 저장할 수 없다")
	void throwsInvalidInputWhenFileIsTooLarge() {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());
		byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
		MockMultipartFile largeFile = new MockMultipartFile("files", "large.png", "image/png", tooLarge);

		assertInvalidInput(() -> storageService.store(List.of(largeFile)));
	}

	@Test
	@DisplayName("존재하지 않는 상품 이미지는 조회할 수 없다")
	void throwsProductNotFoundWhenProductImageDoesNotExist() {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());

		assertProductNotFound(() -> storageService.load("missing.png"));
	}

	@Test
	@DisplayName("저장 경로 밖 파일은 조회할 수 없다")
	void throwsProductNotFoundWhenPathTraversalIsRequested() {
		ProductImageStorageService storageService = new ProductImageStorageService(tempDir.toString());

		assertProductNotFound(() -> storageService.load("../secret.png"));
	}

	private MockMultipartFile imageFile(String filename, String contentType, String content) {
		return new MockMultipartFile("files", filename, contentType, content.getBytes());
	}

	private void assertInvalidInput(ThrowingCallable callable) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
	}

	private void assertProductNotFound(ThrowingCallable callable) {
		assertThatThrownBy(callable)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
	}
}
