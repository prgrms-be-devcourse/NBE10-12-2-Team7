package com.dongnemarket.global.storage;

/**
 * {@link FileStorageService#load}에서 대상 파일을 찾지 못했을 때 던진다(로컬: 파일 없음/경로 이탈,
 * S3: NoSuchKey). 도메인 어댑터가 이 예외를 잡아 자기 도메인의 BusinessException/ErrorCode로 변환한다.
 */
public class StorageFileNotFoundException extends RuntimeException {

	public StorageFileNotFoundException(String filename) {
		super("파일을 찾을 수 없습니다: " + filename);
	}
}
