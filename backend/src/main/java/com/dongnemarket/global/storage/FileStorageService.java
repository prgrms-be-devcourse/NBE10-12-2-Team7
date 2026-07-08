package com.dongnemarket.global.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화. 구현체는 프로파일에 따라 갈린다.
 * <p>{@code test} → {@link LocalFileStorageService}(로컬 디스크, 외부 인프라 불필요)
 * <p>그 외(dev/local/prod) → {@link S3FileStorageService}(AWS S3)
 * <p>{@code directory}는 report-evidence/product-images처럼 도메인별 저장 위치를 구분하는 논리적 네임스페이스로,
 * 로컬 구현체에서는 하위 폴더, S3 구현체에서는 키 prefix로 쓰인다.
 * <p>검증(허용 확장자, 용량 제한 등)은 이 계층의 책임이 아니다 — 호출하는 도메인 어댑터가 담당한다.
 */
public interface FileStorageService {

	/** 파일을 저장하고, 저장에 쓰인 파일명(디렉터리 제외, UUID 기반)을 반환한다. */
	String store(MultipartFile file, String directory);

	/** 저장된 파일을 조회한다. 없으면 {@link StorageFileNotFoundException}. */
	Resource load(String filename, String directory);
}
