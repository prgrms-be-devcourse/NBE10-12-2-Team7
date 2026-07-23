package com.dongnemarket.auth.service;

import com.dongnemarket.auth.client.OAuthUserIdentity;
import com.dongnemarket.auth.entity.OAuthProvider;
import com.dongnemarket.auth.repository.MemberSocialAccountRepository;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
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
 * 동시 최초 소셜 로그인 상황을 실제 MySQL(Testcontainers)에서 재현한다. Mock으로는 실제 UNIQUE 제약
 * 위반과 트랜잭션 롤백이 검증되지 않으므로, 여러 스레드가 완전히 같은 {@link OAuthUserIdentity}로
 * 동시에 {@link OAuthSignupTransaction#signUp}을 호출했을 때 정확히 한 요청만 성공하고, 최종적으로
 * Member 1개·MemberSocialAccount 1개만 남는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@Testcontainers
class OAuthSignupTransactionConcurrencyTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("dongne_signup_concurrency")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
		registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
		registry.add("spring.flyway.enabled", () -> false);
		// 동시 스레드 수만큼 커넥션이 필요하다(기본 Hikari 풀 10개로도 충분하지만 명시적으로 넉넉히 잡는다).
		registry.add("spring.datasource.hikari.maximum-pool-size", () -> 20);
	}

	@Autowired
	OAuthSignupTransaction oauthSignupTransaction;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	MemberSocialAccountRepository memberSocialAccountRepository;

	@Test
	@DisplayName("완전히 같은 신원으로 동시에 signUp()을 호출하면 정확히 1개만 성공하고, Member/MemberSocialAccount 각각 1건만 남는다")
	void concurrentSignUp_sameIdentity_onlyOneSucceeds() throws Exception {
		OAuthUserIdentity identity = new OAuthUserIdentity(OAuthProvider.KAKAO, "concurrent-provider-user-1", "concurrent@kakao.com");

		int attempts = 10;
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger conflictCount = new AtomicInteger();

		List<Callable<Void>> tasks = IntStream.range(0, attempts)
				.<Callable<Void>>mapToObj(i -> () -> {
					ready.countDown();
					start.await();
					try {
						oauthSignupTransaction.signUp(identity);
						successCount.incrementAndGet();
					} catch (DataIntegrityViolationException e) {
						conflictCount.incrementAndGet();
					}
					return null;
				})
				.collect(Collectors.toList());

		List<Future<Void>> futures = tasks.stream().map(executor::submit).collect(Collectors.toList());
		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		for (Future<Void> f : futures) {
			f.get(10, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(conflictCount.get()).isEqualTo(attempts - 1);
		assertThat(memberRepository.findByEmail("concurrent@kakao.com")).isPresent();
		assertThat(memberRepository.count()).isEqualTo(1L);
		assertThat(memberSocialAccountRepository
				.findByProviderAndProviderUserIdFetchMember(OAuthProvider.KAKAO, "concurrent-provider-user-1"))
				.isPresent();
		assertThat(memberSocialAccountRepository.count()).isEqualTo(1L);
	}

	@Test
	@DisplayName("signUp 실패 후 reconcileAfterConflict는 별도 트랜잭션에서 동작해, 승자가 만든 연동을 정상적으로 찾아낸다")
	void reconcileAfterConflict_afterConcurrentConflict_findsWinnersLink() throws Exception {
		OAuthUserIdentity identity = new OAuthUserIdentity(OAuthProvider.GOOGLE, "concurrent-provider-user-2", "concurrent2@gmail.com");

		int attempts = 5;
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		List<Future<Long>> futures = IntStream.range(0, attempts)
				.<Callable<Long>>mapToObj(i -> () -> {
					try {
						return oauthSignupTransaction.signUp(identity).getId();
					} catch (DataIntegrityViolationException e) {
						return oauthSignupTransaction.reconcileAfterConflict(OAuthProvider.GOOGLE, "concurrent-provider-user-2")
								.map(m -> m.getId())
								.orElse(null);
					}
				})
				.map(executor::submit)
				.collect(Collectors.toList());

		List<Long> memberIds = futures.stream().map(f -> {
			try {
				return f.get(10, TimeUnit.SECONDS);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
		executor.shutdown();

		// 승자든 패자든(재조회 성공) 결국 모두 같은 memberId를 봐야 한다 — 회원이 2개로 갈라지면 안 된다.
		assertThat(memberIds).doesNotContainNull();
		assertThat(memberIds).allSatisfy(id -> assertThat(id).isEqualTo(memberIds.get(0)));
	}
}
