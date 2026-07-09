package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.RefreshToken;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code test} 프로파일 전용 구현체. 기존 JPA 동작(회원당 1행, 재로그인 시 기존 row를 dirty checking으로
 * in-place 교체)을 그대로 유지해 {@code ./gradlew test}가 외부 Redis 없이 H2만으로 계속 통과하게 한다.
 */
@Repository
@Profile("test")
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

	private final RefreshTokenJpaEntityRepository jpaRepository;

	public JpaRefreshTokenRepository(RefreshTokenJpaEntityRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<RefreshToken> findByMemberId(Long memberId) {
		return jpaRepository.findByMemberId(memberId);
	}

	/** 기존 row가 있으면 mutate해 영속성 컨텍스트 dirty checking으로 flush되게 하고, 없으면 새로 저장한다. */
	@Override
	public RefreshToken save(RefreshToken refreshToken) {
		return jpaRepository.findByMemberId(refreshToken.getMemberId())
				.map(existing -> {
					existing.replace(refreshToken.getToken(), refreshToken.getExpiresAt());
					return existing;
				})
				.orElseGet(() -> jpaRepository.save(refreshToken));
	}

	@Override
	public void deleteByMemberId(Long memberId) {
		jpaRepository.deleteByMemberId(memberId);
	}
}
