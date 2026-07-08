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
 * {@link RedisPasswordResetTokenRepository}를 실제 Redis(Testcontainers) 위에서 검증한다 — 특히
 * member→tokenHash / tokenHash→memberId 2-key가 항상 같이 움직이는지에 집중한다.
 * Docker가 필요해 기본 {@code test}에서 제외하고 {@code ./gradlew integrationTest}로만 실행한다.
 */
@DataRedisTest
@Tag("integration")
@Testcontainers
class RedisPasswordResetTokenRepositoryTest {

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	private static final Duration TTL = Duration.ofSeconds(2);

	@Autowired
	StringRedisTemplate redisTemplate;

	RedisPasswordResetTokenRepository repository;

	@BeforeEach
	void setUp() {
		repository = new RedisPasswordResetTokenRepository(redisTemplate);
	}

	@AfterEach
	void cleanUp() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@Test
	@DisplayName("save() 하면 member→tokenHash, tokenHash→memberId 양방향으로 모두 조회된다")
	void save_thenBothDirectionsAreQueryable() {
		repository.save(1L, "hash-a", TTL);

		assertThat(repository.findTokenHashByMemberId(1L)).contains("hash-a");
		assertThat(repository.findMemberIdByTokenHash("hash-a")).contains(1L);
		assertThat(redisTemplate.hasKey("auth:password:reset:member:1")).isTrue();
		assertThat(redisTemplate.hasKey("auth:password:reset:token:hash-a")).isTrue();
	}

	@Test
	@DisplayName("deleteByTokenHash()는 tokenHash→memberId 키만 지우고 member→tokenHash 키는 남긴다")
	void deleteByTokenHash_onlyRemovesTokenKey() {
		repository.save(2L, "hash-b", TTL);

		repository.deleteByTokenHash("hash-b");

		assertThat(repository.findMemberIdByTokenHash("hash-b")).isEmpty();
		assertThat(repository.findTokenHashByMemberId(2L)).contains("hash-b");
	}

	@Test
	@DisplayName("deleteByMemberId()는 member→tokenHash 키만 지우고 tokenHash→memberId 키는 남긴다")
	void deleteByMemberId_onlyRemovesMemberKey() {
		repository.save(3L, "hash-c", TTL);

		repository.deleteByMemberId(3L);

		assertThat(repository.findTokenHashByMemberId(3L)).isEmpty();
		assertThat(repository.findMemberIdByTokenHash("hash-c")).contains(3L);
	}

	@Test
	@DisplayName("confirmReset 성공 시나리오처럼 두 키를 모두 지우면 양방향 모두 조회되지 않는다")
	void deleteBothKeys_removesBothDirections() {
		repository.save(4L, "hash-d", TTL);

		repository.deleteByTokenHash("hash-d");
		repository.deleteByMemberId(4L);

		assertThat(repository.findTokenHashByMemberId(4L)).isEmpty();
		assertThat(repository.findMemberIdByTokenHash("hash-d")).isEmpty();
	}

	@Test
	@DisplayName("save() 직후 getRemainingTtlByMemberId()는 설정한 TTL 이하의 값을 반환한다")
	void getRemainingTtlByMemberId_afterSave_returnsWithinConfiguredTtl() {
		repository.save(5L, "hash-e", TTL);

		assertThat(repository.getRemainingTtlByMemberId(5L))
				.isPresent()
				.get()
				.satisfies(remaining -> assertThat(remaining).isLessThanOrEqualTo(TTL).isPositive());
	}

	@Test
	@DisplayName("TTL이 지나면 두 키 모두 자연 만료되어 양방향 조회 모두 빈 값을 반환한다")
	void save_afterTtlExpires_bothDirectionsReturnEmpty() throws InterruptedException {
		repository.save(6L, "hash-f", TTL);
		assertThat(repository.findTokenHashByMemberId(6L)).isPresent();

		Thread.sleep(TTL.plusSeconds(1).toMillis());

		assertThat(repository.findTokenHashByMemberId(6L)).isEmpty();
		assertThat(repository.findMemberIdByTokenHash("hash-f")).isEmpty();
	}
}
