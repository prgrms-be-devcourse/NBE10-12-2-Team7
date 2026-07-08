package com.dongnemarket.global.storage;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * {@code file.storage.type=s3}일 때 쓰이는 구현체. AWS S3에 저장한다.
 * <p>{@code directory}를 S3 키 prefix로 써서 report-evidence/product-images를 분리한다
 * (예: {@code product-images/3f2c...-a1.png}).
 */
@Component
@ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

	private final S3Client s3Client;
	private final String bucket;

	public S3FileStorageService(S3Client s3Client, @Value("${file.storage.s3.bucket}") String bucket) {
		this.s3Client = s3Client;
		this.bucket = bucket;
	}

	@Override
	public String store(MultipartFile file, String directory) {
		String filename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
		String key = key(directory, filename);
		try {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(file.getContentType())
					.build();
			s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
		} catch (IOException | SdkException e) {
			throw new StorageException("S3 업로드에 실패했습니다: " + key, e);
		}
		return filename;
	}

	@Override
	public Resource load(String filename, String directory) {
		// S3 키에는 파일시스템식 경로 탐색이 없지만, 방어적으로 디렉터리 이탈 문자를 거부한다.
		if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("..")) {
			throw new StorageFileNotFoundException(filename);
		}

		String key = key(directory, filename);
		try {
			byte[] bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.build()).asByteArray();
			return new ByteArrayResource(bytes);
		} catch (NoSuchKeyException e) {
			throw new StorageFileNotFoundException(filename);
		} catch (SdkException e) {
			throw new StorageException("S3 다운로드에 실패했습니다: " + key, e);
		}
	}

	private String key(String directory, String filename) {
		return directory + "/" + filename;
	}

	private String extractExtension(String originalFilename) {
		if (originalFilename == null) {
			return "";
		}
		int idx = originalFilename.lastIndexOf('.');
		return idx >= 0 ? originalFilename.substring(idx) : "";
	}
}
