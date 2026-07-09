package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.RefreshToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Redis 기반 구현체. 회원당 키 1개({@code auth:refresh:{memberId}})에 토큰 문자열을 저장하고,
 * TTL을 Refresh Token 만료시간과 동일하게 둬서 만료 판정 자체를 Redis에 위임한다.
 * <p>{@code SET}은 있으면 덮어쓰고 없으면 새로 만드는 연산이라 JPA 구현과 달리 insert/update
 * 분기가 필요 없다.
 */
@Repository
@Profile("!test")
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

	private static final String KEY_PREFIX = "auth:refresh:";

	private final StringRedisTemplate redisTemplate;
	private final long refreshTokenValiditySeconds;

	public RedisRefreshTokenRepository(
			StringRedisTemplate redisTemplate,
			@Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds) {
		this.redisTemplate = redisTemplate;
		this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
	}

	@Override
	public Optional<RefreshToken> findByMemberId(Long memberId) {
		String key = key(memberId);
		String token = redisTemplate.opsForValue().get(key);
		if (token == null) {
			return Optional.empty();
		}
		return Optional.of(RefreshToken.issue(memberId, token, expiresAtFromTtl(key)));
	}

	@Override
	public RefreshToken save(RefreshToken refreshToken) {
		redisTemplate.opsForValue().set(
				key(refreshToken.getMemberId()),
				refreshToken.getToken(),
				Duration.ofSeconds(refreshTokenValiditySeconds));
		return refreshToken;
	}

	@Override
	public void deleteByMemberId(Long memberId) {
		redisTemplate.delete(key(memberId));
	}

	private String key(Long memberId) {
		return KEY_PREFIX + memberId;
	}

	/** 남은 TTL로부터 만료 시각을 역산한다. RefreshToken.expiresAt은 검증 로직에서 실제로 읽히지 않아(JWT의 exp가 권위) 근사치로 충분하다. */
	private LocalDateTime expiresAtFromTtl(String key) {
		Long remainingSeconds = redisTemplate.getExpire(key);
		if (remainingSeconds == null || remainingSeconds < 0) {
			return LocalDateTime.now();
		}
		return LocalDateTime.now().plusSeconds(remainingSeconds);
	}
}
