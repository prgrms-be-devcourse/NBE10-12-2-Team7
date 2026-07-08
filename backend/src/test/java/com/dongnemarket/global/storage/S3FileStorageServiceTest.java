package com.dongnemarket.global.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

	private static final String BUCKET = "test-bucket";
	private static final String DIRECTORY = "product-images";

	@Mock
	S3Client s3Client;

	S3FileStorageService storageService;

	@BeforeEach
	void setUp() {
		storageService = new S3FileStorageService(s3Client, BUCKET);
	}

	@Test
	@DisplayName("store()는 UUID 기반 파일명으로 directory/파일명 키에 putObject를 호출하고, 파일명(디렉터리 제외)을 반환한다")
	void store_callsPutObjectWithCorrectKey() {
		MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "content".getBytes());

		String filename = storageService.store(file, DIRECTORY);

		assertThat(filename).endsWith(".png");

		ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
		PutObjectRequest request = captor.getValue();
		assertThat(request.bucket()).isEqualTo(BUCKET);
		assertThat(request.key()).isEqualTo(DIRECTORY + "/" + filename);
		assertThat(request.contentType()).isEqualTo("image/png");
	}

	@Test
	@DisplayName("putObject 중 SdkException이 발생하면 StorageException으로 변환한다")
	void store_sdkExceptionDuringPut_throwsStorageException() {
		MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "content".getBytes());
		given(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
				.willThrow(SdkException.builder().message("network error").build());

		assertThatThrownBy(() -> storageService.store(file, DIRECTORY))
				.isInstanceOf(StorageException.class);
	}

	@Test
	@DisplayName("load()는 directory/filename 키로 조회해 Resource로 반환한다")
	void load_returnsResourceWithCorrectContent() throws java.io.IOException {
		byte[] content = "image bytes".getBytes();
		ResponseBytes<GetObjectResponse> responseBytes =
				ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content);
		given(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).willReturn(responseBytes);

		Resource resource = storageService.load("existing.png", DIRECTORY);

		assertThat(resource.getContentAsByteArray()).isEqualTo(content);
		ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
		verify(s3Client).getObjectAsBytes(captor.capture());
		assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(captor.getValue().key()).isEqualTo(DIRECTORY + "/existing.png");
	}

	@Test
	@DisplayName("존재하지 않는 키를 조회하면 NoSuchKeyException을 StorageFileNotFoundException으로 변환한다")
	void load_noSuchKey_throwsStorageFileNotFoundException() {
		given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
				.willThrow(NoSuchKeyException.builder().message("not found").build());

		assertThatThrownBy(() -> storageService.load("missing.png", DIRECTORY))
				.isInstanceOf(StorageFileNotFoundException.class);
	}

	@Test
	@DisplayName("S3 조회 중 그 외 SdkException이 발생하면 StorageException으로 변환한다")
	void load_otherSdkException_throwsStorageException() {
		given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
				.willThrow(SdkException.builder().message("throttled").build());

		assertThatThrownBy(() -> storageService.load("existing.png", DIRECTORY))
				.isInstanceOf(StorageException.class);
	}

	@Test
	@DisplayName("파일명에 경로 이탈 문자(.. 또는 /)가 있으면 S3를 호출하지 않고 바로 StorageFileNotFoundException을 던진다")
	void load_pathTraversalFilename_throwsWithoutCallingS3() {
		assertThatThrownBy(() -> storageService.load("../secret.png", DIRECTORY))
				.isInstanceOf(StorageFileNotFoundException.class);
		assertThatThrownBy(() -> storageService.load("sub/dir.png", DIRECTORY))
				.isInstanceOf(StorageFileNotFoundException.class);

		verify(s3Client, never()).getObjectAsBytes(any(GetObjectRequest.class));
	}
}
