package com.dongnemarket.product.service;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.storage.FileStorageService;
import com.dongnemarket.global.storage.StorageException;
import com.dongnemarket.global.storage.StorageFileNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * 상품 이미지 검증 + 저장을 담당한다. 실제 저장/조회는 {@link FileStorageService}(global/storage)에 위임하는
 * 얇은 어댑터다 — test 프로파일은 로컬 디스크, 그 외(dev/local/prod)는 S3를 쓰지만 이 클래스는 그 차이를 모른다.
 * 검증 로직·에러코드 등 product 도메인 정책은 이 클래스가 그대로 소유한다.
 */
@Service
public class ProductImageStorageService {

	private static final String DIRECTORY = "product-images";
	private static final String PRODUCT_IMAGE_URL_PREFIX = "/api/products/images/";
	private static final int MAX_IMAGE_COUNT = 5;
	private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"image/jpeg", "image/png", "image/gif", "image/webp"
	);

	private final FileStorageService fileStorageService;

	public ProductImageStorageService(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	public List<String> store(List<MultipartFile> files) {
		validateFiles(files);
		return files.stream()
				.map(this::storeOne)
				.map(filename -> PRODUCT_IMAGE_URL_PREFIX + filename)
				.toList();
	}

	public Resource load(String filename) {
		try {
			return fileStorageService.load(filename, DIRECTORY);
		} catch (StorageFileNotFoundException e) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
	}

	private String storeOne(MultipartFile file) {
		try {
			return fileStorageService.store(file, DIRECTORY);
		} catch (StorageException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}

	private void validateFiles(List<MultipartFile> files) {
		if (files == null || files.isEmpty() || files.size() > MAX_IMAGE_COUNT) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		files.forEach(this::validateFile);
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}
}
