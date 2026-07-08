package com.dongnemarket.auth.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code test} 프로파일 전용 구현체. 외부 Redis 없이 {@code ./gradlew test}가 통과하도록
 * 메모리 맵 + 만료시각으로 TTL 동작을 흉내 낸다(윈도우가 지나면 조회 시점에 lazy하게 초기화).
 */
@Repository
@Profile("test")
public class InMemoryLoginAttemptRepository implements LoginAttemptRepository {

	private record Entry(long count, Instant expiresAt) {
		boolean isExpired(Instant now) {
			return now.isAfter(expiresAt);
		}
	}

	private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

	@Override
	public synchronized long getFailureCount(String email) {
		Entry entry = store.get(email);
		if (entry == null || entry.isExpired(Instant.now())) {
			return 0L;
		}
		return entry.count();
	}

	@Override
	public synchronized void incrementFailure(String email, Duration lockWindow) {
		Instant now = Instant.now();
		Entry existing = store.get(email);
		if (existing == null || existing.isExpired(now)) {
			store.put(email, new Entry(1L, now.plus(lockWindow)));
			return;
		}
		store.put(email, new Entry(existing.count() + 1, existing.expiresAt()));
	}

	@Override
	public synchronized void resetFailure(String email) {
		store.remove(email);
	}
}
