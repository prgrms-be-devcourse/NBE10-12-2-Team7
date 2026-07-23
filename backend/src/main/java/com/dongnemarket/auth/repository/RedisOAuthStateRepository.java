package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.OAuthProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Redis 기반 구현체. state는 Hash({@code auth:oauth:state:{state}})로, 브라우저별 미완료 state
 * 인덱스는 ZSET({@code auth:oauth:bcid:{browserCorrelationHash}:states}, score=만료시각 epoch millis)으로 관리한다.
 * <p>발급(issue)과 소비(consume) 모두 Lua 스크립트로 원자 처리한다 — Redis는 스크립트 실행 중 다른 명령을
 * 끼워 넣지 않으므로, "조회 후 판단 후 쓰기" 사이에 동시 요청이 끼어들 여지가 없다.
 */
@Repository
@Profile("!test")
public class RedisOAuthStateRepository implements OAuthStateRepository {

	private static final String STATE_KEY_PREFIX = "auth:oauth:state:";
	private static final String BCID_KEY_PREFIX = "auth:oauth:bcid:";
	private static final String BCID_KEY_SUFFIX = ":states";

	/** KEYS=[stateKey, bcidZsetKey], ARGV=[ttlMillis, nowMillis, maxPending, state, provider, browserCorrelationHash, redirectUri, codeVerifier, oidcNonce, issuedAtEpochMilli] */
	private static final RedisScript<Long> ISSUE_SCRIPT = new DefaultRedisScript<>("""
			local ttlMillis = tonumber(ARGV[1])
			local nowMillis = tonumber(ARGV[2])
			local maxPending = tonumber(ARGV[3])

			redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', nowMillis)
			local count = redis.call('ZCARD', KEYS[2])
			if count >= maxPending then
			    return 0
			end

			redis.call('HSET', KEYS[1],
			    'provider', ARGV[5],
			    'browserCorrelationHash', ARGV[6],
			    'redirectUri', ARGV[7],
			    'codeVerifier', ARGV[8],
			    'oidcNonce', ARGV[9],
			    'issuedAt', ARGV[10])
			redis.call('PEXPIRE', KEYS[1], ttlMillis)

			redis.call('ZADD', KEYS[2], nowMillis + ttlMillis, ARGV[4])
			redis.call('PEXPIRE', KEYS[2], ttlMillis + 1000)

			return 1
			""", Long.class);

	/**
	 * KEYS=[stateKey, bcidZsetKey], ARGV=[expectedProvider, expectedBrowserCorrelationHash, state]
	 * <p>{@code HGETALL}은 순서를 보장하지 않으므로(구현/버전에 따라 달라질 수 있음), 필요한 필드를
	 * 고정된 순서로 {@code HGET}해 배열로 반환한다 — 반환값 인덱스가 항상 [provider, browserCorrelationHash,
	 * redirectUri, codeVerifier, oidcNonce, issuedAt]로 고정된다.
	 */
	@SuppressWarnings("rawtypes")
	private static final RedisScript<List> CONSUME_SCRIPT = new DefaultRedisScript<>("""
			local provider = redis.call('HGET', KEYS[1], 'provider')
			if not provider then
			    return {}
			end
			local bch = redis.call('HGET', KEYS[1], 'browserCorrelationHash')
			if provider ~= ARGV[1] or bch ~= ARGV[2] then
			    return {}
			end

			local redirectUri = redis.call('HGET', KEYS[1], 'redirectUri')
			local codeVerifier = redis.call('HGET', KEYS[1], 'codeVerifier')
			local oidcNonce = redis.call('HGET', KEYS[1], 'oidcNonce')
			local issuedAt = redis.call('HGET', KEYS[1], 'issuedAt')

			redis.call('DEL', KEYS[1])
			redis.call('ZREM', KEYS[2], ARGV[3])

			return {provider, bch, redirectUri, codeVerifier, oidcNonce, issuedAt}
			""", List.class);

	private static final int IDX_PROVIDER = 0;
	private static final int IDX_BROWSER_CORRELATION_HASH = 1;
	private static final int IDX_REDIRECT_URI = 2;
	private static final int IDX_CODE_VERIFIER = 3;
	private static final int IDX_OIDC_NONCE = 4;
	private static final int IDX_ISSUED_AT = 5;

	private final StringRedisTemplate redisTemplate;

	public RedisOAuthStateRepository(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public boolean issue(String state, OAuthAuthorizationState value, Duration ttl, int maxPendingPerBrowser) {
		long nowMillis = System.currentTimeMillis();
		Long result = redisTemplate.execute(ISSUE_SCRIPT,
				List.of(stateKey(state), bcidKey(value.browserCorrelationHash())),
				String.valueOf(ttl.toMillis()),
				String.valueOf(nowMillis),
				String.valueOf(maxPendingPerBrowser),
				state,
				value.provider().name(),
				value.browserCorrelationHash(),
				value.redirectUri(),
				value.codeVerifier(),
				nullToEmpty(value.oidcNonce()),
				String.valueOf(value.issuedAt().toEpochMilli()));
		return result != null && result == 1L;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Optional<OAuthAuthorizationState> consume(String state, OAuthProvider provider, String browserCorrelationHash) {
		List<Object> fields = redisTemplate.execute(CONSUME_SCRIPT,
				List.of(stateKey(state), bcidKey(browserCorrelationHash)),
				provider.name(),
				browserCorrelationHash,
				state);
		if (fields == null || fields.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(toState(fields));
	}

	/** CONSUME_SCRIPT가 고정 순서로 반환한 배열([provider, browserCorrelationHash, redirectUri, codeVerifier, oidcNonce, issuedAt])을 값 객체로 되돌린다. */
	private OAuthAuthorizationState toState(List<Object> fields) {
		String oidcNonce = String.valueOf(fields.get(IDX_OIDC_NONCE));
		return new OAuthAuthorizationState(
				OAuthProvider.valueOf(String.valueOf(fields.get(IDX_PROVIDER))),
				String.valueOf(fields.get(IDX_BROWSER_CORRELATION_HASH)),
				String.valueOf(fields.get(IDX_REDIRECT_URI)),
				String.valueOf(fields.get(IDX_CODE_VERIFIER)),
				oidcNonce.isEmpty() ? null : oidcNonce,
				Instant.ofEpochMilli(Long.parseLong(String.valueOf(fields.get(IDX_ISSUED_AT)))));
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private String stateKey(String state) {
		return STATE_KEY_PREFIX + state;
	}

	private String bcidKey(String browserCorrelationHash) {
		return BCID_KEY_PREFIX + browserCorrelationHash + BCID_KEY_SUFFIX;
	}
}
