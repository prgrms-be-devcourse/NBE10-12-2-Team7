package com.dongnemarket.auth.repository;

import java.time.Duration;

/**
 * 로그인 실패 횟수 저장소 추상화. 구현체는 프로파일에 따라 갈린다.
 * <p>{@code test} → {@link InMemoryLoginAttemptRepository}(H2/외부 인프라 불필요)
 * <p>그 외(dev/prod) → {@link RedisLoginAttemptRepository}(TTL 기반, {@code auth:login:fail:{email}})
 */
public interface LoginAttemptRepository {

	/** 이메일의 현재 실패 횟수를 조회한다(윈도우가 지났다면 0). */
	long getFailureCount(String email);

	/**
	 * 실패 횟수를 1 증가시킨다. 최초 실패(1로 증가) 시에만 {@code lockWindow}를 TTL로 설정해
	 * 그 시점부터 고정된 윈도우가 시작되게 한다(윈도우 내 반복 실패는 TTL을 연장하지 않는다).
	 */
	void incrementFailure(String email, Duration lockWindow);

	/** 로그인 성공 시 호출: 실패 횟수를 초기화한다. */
	void resetFailure(String email);
}
