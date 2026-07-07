package com.dongnemarket.product.controller;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.response.ApiResponse;
import com.dongnemarket.product.dto.ProductImageUploadResponse;
import com.dongnemarket.product.service.ProductImageStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.util.List;

@RestController
public class ProductImageController {

	private final ProductImageStorageService productImageStorageService;

	public ProductImageController(ProductImageStorageService productImageStorageService) {
		this.productImageStorageService = productImageStorageService;
	}

	@PostMapping(value = "/api/products/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ProductImageUploadResponse>> uploadProductImages(
			@AuthenticationPrincipal Long memberId,
			@RequestPart("files") List<MultipartFile> files) {
		if (memberId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		ProductImageUploadResponse response = ProductImageUploadResponse.of(productImageStorageService.store(files));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(HttpStatus.CREATED.value(), "상품 이미지가 업로드되었습니다.", response));
	}

	@GetMapping("/api/products/images/{filename}")
	public ResponseEntity<Resource> getProductImage(@PathVariable String filename) {
		Resource resource = productImageStorageService.load(filename);
		String contentType = URLConnection.guessContentTypeFromName(filename);
		MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
		return ResponseEntity.ok()
				.contentType(mediaType)
				.body(resource);
	}
}
