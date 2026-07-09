package com.dongnemarket.auth.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 기반 구현체. 이메일당 키 1개({@code auth:email:verify:{email}})에 코드를 저장하고,
 * TTL을 인증 코드 유효기간과 동일하게 둬서 만료 판정 자체를 Redis에 위임한다.
 */
@Repository
@Profile("!test")
public class RedisEmailVerificationCodeRepository implements EmailVerificationCodeRepository {

	private static final String KEY_PREFIX = "auth:email:verify:";

	private final StringRedisTemplate redisTemplate;

	public RedisEmailVerificationCodeRepository(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(String email, String code, Duration ttl) {
		redisTemplate.opsForValue().set(key(email), code, ttl);
	}

	@Override
	public Optional<String> findCode(String email) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(email)));
	}

	@Override
	public Optional<Duration> getRemainingTtl(String email) {
		Long remainingSeconds = redisTemplate.getExpire(key(email));
		if (remainingSeconds == null || remainingSeconds < 0) {
			return Optional.empty();
		}
		return Optional.of(Duration.ofSeconds(remainingSeconds));
	}

	@Override
	public void delete(String email) {
		redisTemplate.delete(key(email));
	}

	private String key(String email) {
		return KEY_PREFIX + email;
	}
}
