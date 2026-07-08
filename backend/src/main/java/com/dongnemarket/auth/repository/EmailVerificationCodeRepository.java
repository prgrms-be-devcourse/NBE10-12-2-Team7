package com.dongnemarket.auth.repository;

import java.time.Duration;
import java.util.Optional;

/**
 * 이메일 인증 코드(TTL 데이터) 저장소 추상화. 구현체는 프로파일에 따라 갈린다.
 * <p>{@code test} → {@link InMemoryEmailVerificationCodeRepository}(외부 인프라 불필요)
 * <p>그 외(dev/prod) → {@link RedisEmailVerificationCodeRepository}(TTL 기반, {@code auth:email:verify:{email}})
 */
public interface EmailVerificationCodeRepository {

	/** 코드를 저장한다(이미 있으면 덮어쓰기 — 재요청 시 새 코드로 교체). */
	void save(String email, String code, Duration ttl);

	Optional<String> findCode(String email);

	/** 남은 TTL. 코드가 없으면 empty. 쿨다운(재요청 제한) 판단에 쓰인다. */
	Optional<Duration> getRemainingTtl(String email);

	/** 인증 성공 시 호출: 코드를 삭제한다. */
	void delete(String email);
}
