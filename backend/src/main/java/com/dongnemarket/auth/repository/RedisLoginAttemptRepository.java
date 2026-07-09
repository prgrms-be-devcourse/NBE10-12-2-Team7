package com.dongnemarket.auth.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * Redis 기반 구현체. 이메일당 키 1개({@code auth:login:fail:{email}})에 실패 횟수를 담고,
 * 최초 실패 시점부터 고정 윈도우(TTL)가 흐르게 해 "N회 실패 시 M분 차단"을 자연스럽게 구현한다.
 */
@Repository
@Profile("!test")
public class RedisLoginAttemptRepository implements LoginAttemptRepository {

	private static final String KEY_PREFIX = "auth:login:fail:";

	private final StringRedisTemplate redisTemplate;

	public RedisLoginAttemptRepository(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public long getFailureCount(String email) {
		String value = redisTemplate.opsForValue().get(key(email));
		return value == null ? 0L : Long.parseLong(value);
	}

	@Override
	public void incrementFailure(String email, Duration lockWindow) {
		String key = key(email);
		Long count = redisTemplate.opsForValue().increment(key);
		if (count != null && count == 1L) {
			redisTemplate.expire(key, lockWindow);
		}
	}

	@Override
	public void resetFailure(String email) {
		redisTemplate.delete(key(email));
	}

	private String key(String email) {
		return KEY_PREFIX + email;
	}
}
