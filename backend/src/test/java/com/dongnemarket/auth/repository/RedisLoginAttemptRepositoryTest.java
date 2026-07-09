package com.dongnemarket.auth.repository;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisLoginAttemptRepository}를 실제 Redis(Testcontainers) 위에서 검증한다.
 * Docker가 필요해 기본 {@code test}에서 제외하고 {@code ./gradlew integrationTest}로만 실행한다.
 */
@DataRedisTest
@Tag("integration")
@Testcontainers
class RedisLoginAttemptRepositoryTest {

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	private static final Duration LOCK_WINDOW = Duration.ofSeconds(2);

	@Autowired
	StringRedisTemplate redisTemplate;

	RedisLoginAttemptRepository repository;

	@BeforeEach
	void setUp() {
		repository = new RedisLoginAttemptRepository(redisTemplate);
	}

	@AfterEach
	void cleanUp() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@Test
	@DisplayName("실패를 누적할 때마다 getFailureCount()가 1씩 증가하고, key는 auth:login:fail:{email} 형태다")
	void incrementFailure_accumulatesCount() {
		String email = "user-a@example.com";

		repository.incrementFailure(email, LOCK_WINDOW);
		repository.incrementFailure(email, LOCK_WINDOW);
		repository.incrementFailure(email, LOCK_WINDOW);

		assertThat(repository.getFailureCount(email)).isEqualTo(3L);
		assertThat(redisTemplate.hasKey("auth:login:fail:" + email)).isTrue();
	}

	@Test
	@DisplayName("resetFailure() 호출 후에는 getFailureCount()가 0을 반환한다")
	void resetFailure_clearsCount() {
		String email = "user-b@example.com";
		repository.incrementFailure(email, LOCK_WINDOW);
		repository.incrementFailure(email, LOCK_WINDOW);

		repository.resetFailure(email);

		assertThat(repository.getFailureCount(email)).isZero();
	}

	@Test
	@DisplayName("한 번도 실패하지 않은 이메일의 getFailureCount()는 0이다")
	void getFailureCount_neverFailed_returnsZero() {
		assertThat(repository.getFailureCount("never-failed@example.com")).isZero();
	}

	@Test
	@DisplayName("최초 실패 시 설정한 윈도우(TTL)가 지나면 실패 횟수가 자연히 0으로 초기화된다")
	void incrementFailure_afterLockWindowExpires_countResetsToZero() throws InterruptedException {
		String email = "user-c@example.com";
		repository.incrementFailure(email, LOCK_WINDOW);
		assertThat(repository.getFailureCount(email)).isEqualTo(1L);

		Thread.sleep(LOCK_WINDOW.plusSeconds(1).toMillis());

		assertThat(repository.getFailureCount(email)).isZero();
	}

	@Test
	@DisplayName("최초 실패 이후 윈도우 내 반복 실패는 TTL을 다시 연장하지 않는다(고정 윈도우)")
	void incrementFailure_withinWindow_doesNotExtendTtl() {
		String email = "user-d@example.com";
		repository.incrementFailure(email, LOCK_WINDOW);
		Long firstTtl = redisTemplate.getExpire("auth:login:fail:" + email);

		repository.incrementFailure(email, LOCK_WINDOW);
		Long secondTtl = redisTemplate.getExpire("auth:login:fail:" + email);

		assertThat(firstTtl).isNotNull();
		assertThat(secondTtl).isNotNull();
		assertThat(secondTtl).isLessThanOrEqualTo(firstTtl);
	}
}
