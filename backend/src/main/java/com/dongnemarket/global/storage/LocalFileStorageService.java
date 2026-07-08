package com.dongnemarket.global.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code file.storage.type=local}(기본값 — 값이 아예 없어도 선택된다)일 때 쓰이는 구현체.
 * {@code test} 프로파일(외부 인프라 없이 저장/조회 검증)과 온프레미스/로컬 디스크 배포 둘 다 이 구현체를 쓴다.
 * {@code directory}별로 {@code basePath} 하위에 폴더를 만들어 report-evidence/product-images를 분리한다.
 */
@Component
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

	private final Path basePath;

	public LocalFileStorageService(@Value("${file.storage.local.base-path:./uploads}") String basePath) {
		this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
	}

	@Override
	public String store(MultipartFile file, String directory) {
		Path dir = resolveDirectory(directory);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new StorageException("저장 디렉터리를 생성할 수 없습니다: " + dir, e);
		}

		String filename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
		Path target = dir.resolve(filename);
		try {
			file.transferTo(target);
		} catch (IOException e) {
			throw new StorageException("파일 저장에 실패했습니다: " + filename, e);
		}
		return filename;
	}

	@Override
	public Resource load(String filename, String directory) {
		Path dir = resolveDirectory(directory);
		Path target = dir.resolve(filename).normalize();
		// 경로 조작(path traversal) 방지: 정규화한 경로가 반드시 저장 디렉터리 내부여야 한다.
		if (!target.startsWith(dir)) {
			throw new StorageFileNotFoundException(filename);
		}

		Resource resource = new FileSystemResource(target);
		if (!resource.exists() || !resource.isReadable()) {
			throw new StorageFileNotFoundException(filename);
		}
		return resource;
	}

	private Path resolveDirectory(String directory) {
		return basePath.resolve(directory).toAbsolutePath().normalize();
	}

	private String extractExtension(String originalFilename) {
		if (originalFilename == null) {
			return "";
		}
		int idx = originalFilename.lastIndexOf('.');
		return idx >= 0 ? originalFilename.substring(idx) : "";
	}
}
