package com.dongnemarket.auth.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code test} 프로파일 전용 구현체. 외부 Redis 없이 {@code ./gradlew test}가 통과하도록
 * 메모리 맵 + 만료시각으로 TTL 동작을 흉내 낸다(조회 시점에 lazy하게 만료 처리).
 */
@Repository
@Profile("test")
public class InMemoryEmailVerificationCodeRepository implements EmailVerificationCodeRepository {

	private record Entry(String code, Instant expiresAt) {
	}

	private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

	@Override
	public synchronized void save(String email, String code, Duration ttl) {
		store.put(email, new Entry(code, Instant.now().plus(ttl)));
	}

	@Override
	public synchronized Optional<String> findCode(String email) {
		Entry entry = store.get(email);
		if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
			store.remove(email);
			return Optional.empty();
		}
		return Optional.of(entry.code());
	}

	@Override
	public synchronized Optional<Duration> getRemainingTtl(String email) {
		Entry entry = store.get(email);
		if (entry == null) {
			return Optional.empty();
		}
		Duration remaining = Duration.between(Instant.now(), entry.expiresAt());
		if (remaining.isNegative()) {
			store.remove(email);
			return Optional.empty();
		}
		return Optional.of(remaining);
	}

	@Override
	public synchronized void delete(String email) {
		store.remove(email);
	}
}
