package com.dongnemarket.auth.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 기반 구현체. member→tokenHash({@code auth:password:reset:member:{memberId}})와
 * tokenHash→memberId({@code auth:password:reset:token:{tokenHash}}) 두 키를 항상 같은 TTL로 함께 관리한다.
 */
@Repository
@Profile("!test")
public class RedisPasswordResetTokenRepository implements PasswordResetTokenRepository {

	private static final String MEMBER_KEY_PREFIX = "auth:password:reset:member:";
	private static final String TOKEN_KEY_PREFIX = "auth:password:reset:token:";

	private final StringRedisTemplate redisTemplate;

	public RedisPasswordResetTokenRepository(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(Long memberId, String tokenHash, Duration ttl) {
		redisTemplate.opsForValue().set(memberKey(memberId), tokenHash, ttl);
		redisTemplate.opsForValue().set(tokenKey(tokenHash), String.valueOf(memberId), ttl);
	}

	@Override
	public Optional<String> findTokenHashByMemberId(Long memberId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(memberKey(memberId)));
	}

	@Override
	public Optional<Duration> getRemainingTtlByMemberId(Long memberId) {
		Long remainingSeconds = redisTemplate.getExpire(memberKey(memberId));
		if (remainingSeconds == null || remainingSeconds < 0) {
			return Optional.empty();
		}
		return Optional.of(Duration.ofSeconds(remainingSeconds));
	}

	@Override
	public Optional<Long> findMemberIdByTokenHash(String tokenHash) {
		String value = redisTemplate.opsForValue().get(tokenKey(tokenHash));
		return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
	}

	@Override
	public void deleteByMemberId(Long memberId) {
		redisTemplate.delete(memberKey(memberId));
	}

	@Override
	public void deleteByTokenHash(String tokenHash) {
		redisTemplate.delete(tokenKey(tokenHash));
	}

	private String memberKey(Long memberId) {
		return MEMBER_KEY_PREFIX + memberId;
	}

	private String tokenKey(String tokenHash) {
		return TOKEN_KEY_PREFIX + tokenHash;
	}
}
