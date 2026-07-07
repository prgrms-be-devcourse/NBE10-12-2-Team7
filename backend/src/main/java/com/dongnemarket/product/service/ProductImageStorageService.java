package com.dongnemarket.product.service;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageStorageService {

	private static final String PRODUCT_IMAGE_URL_PREFIX = "/api/products/images/";
	private static final int MAX_IMAGE_COUNT = 5;
	private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"image/jpeg", "image/png", "image/gif", "image/webp"
	);

	private final Path storageDir;

	public ProductImageStorageService(@Value("${product.image.storage-path:./uploads/product-images}") String storagePath) {
		this.storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.storageDir);
		} catch (IOException e) {
			throw new IllegalStateException("상품 이미지 저장 디렉터리를 생성할 수 없습니다: " + this.storageDir, e);
		}
	}

	public List<String> store(List<MultipartFile> files) {
		validateFiles(files);
		return files.stream()
				.map(this::storeOne)
				.map(filename -> PRODUCT_IMAGE_URL_PREFIX + filename)
				.toList();
	}

	public Resource load(String filename) {
		Path target = storageDir.resolve(filename).normalize();
		if (!target.startsWith(storageDir)) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}

		Resource resource = new FileSystemResource(target);
		if (!resource.exists() || !resource.isReadable()) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		return resource;
	}

	private String storeOne(MultipartFile file) {
		String filename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
		Path target = storageDir.resolve(filename);
		try {
			file.transferTo(target);
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		return filename;
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

	private String extractExtension(String originalFilename) {
		if (originalFilename == null) {
			return "";
		}
		int index = originalFilename.lastIndexOf('.');
		if (index < 0) {
			return "";
		}
		return originalFilename.substring(index);
	}
}
