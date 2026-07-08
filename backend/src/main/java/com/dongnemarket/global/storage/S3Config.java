package com.dongnemarket.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3 연결 설정. {@code file.storage.type=s3}일 때만 빈을 만든다 — 그 외(local, 기본값)에서는
 * {@code file.storage.s3.*} 프로퍼티가 없어도 컨텍스트가 뜬다(test 프로파일도 이 경로로 자연스럽게 커버된다).
 * <p>자격증명은 {@link DefaultCredentialsProvider}를 사용한다 — 로컬/dev는 개발자의 {@code ~/.aws/credentials}
 * 또는 실제 OS 환경변수(AWS_ACCESS_KEY_ID/SECRET), 배포 환경(EC2)은 IAM 인스턴스 역할을 자동으로 사용한다.
 * 자격증명을 코드나 .env에 직접 넣지 않는다.
 */
@Configuration
@ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
public class S3Config {

	@Bean
	public S3Client s3Client(@Value("${file.storage.s3.region}") String region) {
		return S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(DefaultCredentialsProvider.builder().build())
				.build();
	}
}
