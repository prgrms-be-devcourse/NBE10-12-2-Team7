package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.OAuthProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code test} 프로파일 전용 구현체. 외부 Redis 없이 {@code ./gradlew test}가 통과하도록
 * 메모리 맵 + 만료시각으로 동작을 흉내 낸다. 단일 JVM 내 테스트만 대상이라 {@code synchronized}로
 * {@link RedisOAuthStateRepository}의 Lua 원자성과 동등한 효과를 낸다.
 */
@Repository
@Profile("test")
public class InMemoryOAuthStateRepository implements OAuthStateRepository {

	private record Entry(OAuthAuthorizationState value, Instant expiresAt) {
		boolean isExpired(Instant now) {
			return now.isAfter(expiresAt);
		}
	}

	private final Map<String, Entry> states = new ConcurrentHashMap<>();

	@Override
	public synchronized boolean issue(String state, OAuthAuthorizationState value, Duration ttl, int maxPendingPerBrowser) {
		Instant now = Instant.now();
		states.entrySet().removeIf(e -> e.getValue().isExpired(now));

		long pendingForBrowser = states.values().stream()
				.filter(e -> e.value().browserCorrelationHash().equals(value.browserCorrelationHash()))
				.count();
		if (pendingForBrowser >= maxPendingPerBrowser) {
			return false;
		}

		states.put(state, new Entry(value, now.plus(ttl)));
		return true;
	}

	@Override
	public synchronized Optional<OAuthAuthorizationState> consume(String state, OAuthProvider provider, String browserCorrelationHash) {
		Entry entry = states.get(state);
		if (entry == null || entry.isExpired(Instant.now())) {
			states.remove(state);
			return Optional.empty();
		}
		if (entry.value().provider() != provider || !entry.value().browserCorrelationHash().equals(browserCorrelationHash)) {
			return Optional.empty();
		}
		states.remove(state);
		return Optional.of(entry.value());
	}
}
