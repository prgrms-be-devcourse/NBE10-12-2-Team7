package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.OAuthProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisOAuthStateRepository}를 실제 Redis(Testcontainers) 위에서 검증한다.
 * InMemory 구현({@link InMemoryOAuthStateRepository})은 {@code synchronized}로 원자성을 흉내낼 뿐이라,
 * 실제 Lua 스크립트의 원자성(동시 요청 하에서도 발급 한도를 넘지 않는지, 같은 state를 두 번 소비할 수
 * 없는지)은 진짜 Redis로만 확인할 수 있다.
 */
@DataRedisTest
@Tag("integration")
@Testcontainers
class RedisOAuthStateRepositoryTest {

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	private static final Duration TTL = Duration.ofSeconds(5);

	@Autowired
	StringRedisTemplate redisTemplate;

	RedisOAuthStateRepository repository;

	@BeforeEach
	void setUp() {
		repository = new RedisOAuthStateRepository(redisTemplate);
	}

	@AfterEach
	void cleanUp() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	private OAuthAuthorizationState sampleState(String browserCorrelationHash) {
		return new OAuthAuthorizationState(
				OAuthProvider.GOOGLE, browserCorrelationHash, "https://app.example.com/oauth/google/callback",
				"code-verifier-value", "oidc-nonce-value", Instant.now());
	}

	private String newState() {
		return UUID.randomUUID().toString();
	}

	@Test
	@DisplayName("issue() 후 consume()으로 같은 값을 정확히 한 번 돌려받는다")
	void issue_thenConsume_returnsSameValue() {
		String state = newState();
		OAuthAuthorizationState value = sampleState("bcid-1");

		boolean issued = repository.issue(state, value, TTL, 5);
		Optional<OAuthAuthorizationState> consumed = repository.consume(state, OAuthProvider.GOOGLE, "bcid-1");

		assertThat(issued).isTrue();
		assertThat(consumed).isPresent();
		assertThat(consumed.get().provider()).isEqualTo(OAuthProvider.GOOGLE);
		assertThat(consumed.get().browserCorrelationHash()).isEqualTo("bcid-1");
		assertThat(consumed.get().redirectUri()).isEqualTo(value.redirectUri());
		assertThat(consumed.get().codeVerifier()).isEqualTo(value.codeVerifier());
		assertThat(consumed.get().oidcNonce()).isEqualTo(value.oidcNonce());
	}

	@Test
	@DisplayName("브라우저당 미완료 state가 한도(maxPendingPerBrowser)에 도달하면 다음 issue()는 거부된다")
	void issue_upToMaxPending_rejectsNext() {
		String bcid = "bcid-cap";
		for (int i = 0; i < 3; i++) {
			assertThat(repository.issue(newState(), sampleState(bcid), TTL, 3)).isTrue();
		}

		boolean fourth = repository.issue(newState(), sampleState(bcid), TTL, 3);

		assertThat(fourth).isFalse();
	}

	@Test
	@DisplayName("만료된 state는 브라우저별 한도 계산에서 제외된다(발급 시 자동 정리)")
	void issue_expiredEntriesAreExcludedFromPendingCount() throws InterruptedException {
		String bcid = "bcid-expiry";
		Duration shortTtl = Duration.ofSeconds(1);
		repository.issue(newState(), sampleState(bcid), shortTtl, 1);

		Thread.sleep(1500);

		boolean afterExpiry = repository.issue(newState(), sampleState(bcid), TTL, 1);

		assertThat(afterExpiry).isTrue();
	}

	@Test
	@DisplayName("provider가 불일치하면 consume()은 빈 값을 반환하고 state를 소비하지 않는다 — 이후 올바른 provider로 다시 소비할 수 있다")
	void consume_wrongProvider_doesNotConsumeState() {
		String state = newState();
		repository.issue(state, sampleState("bcid-2"), TTL, 5);

		Optional<OAuthAuthorizationState> wrongAttempt = repository.consume(state, OAuthProvider.KAKAO, "bcid-2");
		assertThat(wrongAttempt).isEmpty();

		Optional<OAuthAuthorizationState> correctAttempt = repository.consume(state, OAuthProvider.GOOGLE, "bcid-2");
		assertThat(correctAttempt).isPresent();
	}

	@Test
	@DisplayName("browserCorrelationHash가 불일치하면 consume()은 빈 값을 반환하고 state를 소비하지 않는다")
	void consume_wrongBrowserCorrelationHash_doesNotConsumeState() {
		String state = newState();
		repository.issue(state, sampleState("bcid-3"), TTL, 5);

		Optional<OAuthAuthorizationState> wrongAttempt = repository.consume(state, OAuthProvider.GOOGLE, "other-browser");
		assertThat(wrongAttempt).isEmpty();

		Optional<OAuthAuthorizationState> correctAttempt = repository.consume(state, OAuthProvider.GOOGLE, "bcid-3");
		assertThat(correctAttempt).isPresent();
	}

	@Test
	@DisplayName("정상 소비 시 state Hash와 브라우저별 ZSET 인덱스가 함께 제거된다")
	void consume_success_removesHashAndZsetEntry() {
		String state = newState();
		String bcid = "bcid-4";
		repository.issue(state, sampleState(bcid), TTL, 5);

		repository.consume(state, OAuthProvider.GOOGLE, bcid);

		assertThat(redisTemplate.hasKey("auth:oauth:state:" + state)).isFalse();
		assertThat(redisTemplate.opsForZSet().zCard("auth:oauth:bcid:" + bcid + ":states")).isEqualTo(0L);
	}

	@Test
	@DisplayName("issue() 직후 state Hash와 ZSET 모두 TTL이 설정되어 있다")
	void issue_setsTtlOnBothKeys() {
		String state = newState();
		String bcid = "bcid-5";

		repository.issue(state, sampleState(bcid), TTL, 5);

		Long stateTtl = redisTemplate.getExpire("auth:oauth:state:" + state);
		Long bcidTtl = redisTemplate.getExpire("auth:oauth:bcid:" + bcid + ":states");

		assertThat(stateTtl).isNotNull().isGreaterThan(0L);
		assertThat(bcidTtl).isNotNull().isGreaterThan(0L);
	}

	@Test
	@DisplayName("동일 state를 동시에 소비하려는 여러 요청 중 정확히 하나만 성공한다(원자적 검증+삭제)")
	void concurrentConsume_sameState_onlyOneSucceeds() throws InterruptedException {
		String state = newState();
		String bcid = "bcid-race";
		repository.issue(state, sampleState(bcid), TTL, 10);

		int attempts = 20;
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);

		List<Callable<Boolean>> tasks = IntStream.range(0, attempts)
				.<Callable<Boolean>>mapToObj(i -> () -> {
					ready.countDown();
					start.await();
					return repository.consume(state, OAuthProvider.GOOGLE, bcid).isPresent();
				})
				.collect(Collectors.toList());

		List<Future<Boolean>> futures = tasks.stream().map(executor::submit).collect(Collectors.toList());
		ready.await(5, TimeUnit.SECONDS);
		start.countDown();

		long successCount = futures.stream().mapToLong(f -> {
			try {
				return f.get() ? 1 : 0;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).sum();
		executor.shutdown();

		assertThat(successCount).isEqualTo(1L);
	}

	@Test
	@DisplayName("동시 issue() 요청이 한도를 넘어도 발급 성공 개수는 정확히 maxPendingPerBrowser를 넘지 않는다")
	void concurrentIssue_neverExceedsMaxPending() throws Exception {
		String bcid = "bcid-concurrent-issue";
		int maxPending = 5;
		int attempts = 30;
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();

		List<Callable<Void>> tasks = IntStream.range(0, attempts)
				.<Callable<Void>>mapToObj(i -> () -> {
					ready.countDown();
					start.await();
					if (repository.issue(newState(), sampleState(bcid), TTL, maxPending)) {
						successCount.incrementAndGet();
					}
					return null;
				})
				.collect(Collectors.toList());

		List<Future<Void>> futures = tasks.stream().map(executor::submit).collect(Collectors.toList());
		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		for (Future<Void> f : futures) {
			f.get(5, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertThat(successCount.get()).isEqualTo(maxPending);
		Long finalPending = redisTemplate.opsForZSet().zCard("auth:oauth:bcid:" + bcid + ":states");
		assertThat(finalPending).isEqualTo((long) maxPending);
	}
}
