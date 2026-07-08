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
public class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {

	private record Entry(String value, Instant expiresAt) {
	}

	private final ConcurrentHashMap<Long, Entry> byMemberId = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Entry> byTokenHash = new ConcurrentHashMap<>();

	@Override
	public synchronized void save(Long memberId, String tokenHash, Duration ttl) {
		Instant expiresAt = Instant.now().plus(ttl);
		byMemberId.put(memberId, new Entry(tokenHash, expiresAt));
		byTokenHash.put(tokenHash, new Entry(String.valueOf(memberId), expiresAt));
	}

	@Override
	public synchronized Optional<String> findTokenHashByMemberId(Long memberId) {
		return getIfNotExpired(byMemberId, memberId);
	}

	@Override
	public synchronized Optional<Duration> getRemainingTtlByMemberId(Long memberId) {
		Entry entry = byMemberId.get(memberId);
		if (entry == null) {
			return Optional.empty();
		}
		Duration remaining = Duration.between(Instant.now(), entry.expiresAt());
		if (remaining.isNegative()) {
			byMemberId.remove(memberId);
			return Optional.empty();
		}
		return Optional.of(remaining);
	}

	@Override
	public synchronized Optional<Long> findMemberIdByTokenHash(String tokenHash) {
		return getIfNotExpired(byTokenHash, tokenHash).map(Long::valueOf);
	}

	@Override
	public synchronized void deleteByMemberId(Long memberId) {
		byMemberId.remove(memberId);
	}

	@Override
	public synchronized void deleteByTokenHash(String tokenHash) {
		byTokenHash.remove(tokenHash);
	}

	/** 테스트 간 상태 격리용(스프링 컨텍스트가 캐싱되어 빈이 테스트 클래스 간에도 공유되기 때문). */
	public synchronized void clear() {
		byMemberId.clear();
		byTokenHash.clear();
	}

	private <K> Optional<String> getIfNotExpired(ConcurrentHashMap<K, Entry> map, K key) {
		Entry entry = map.get(key);
		if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
			map.remove(key);
			return Optional.empty();
		}
		return Optional.of(entry.value());
	}
}
