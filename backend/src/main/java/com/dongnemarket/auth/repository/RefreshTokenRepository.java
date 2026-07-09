package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.RefreshToken;

import java.util.Optional;

/**
 * Refresh Token 저장소 추상화. 구현체는 프로파일에 따라 갈린다.
 * <p>{@code test} → {@link JpaRefreshTokenRepository}(H2, 외부 인프라 불필요)
 * <p>그 외(dev/prod) → {@link RedisRefreshTokenRepository}(TTL 기반, {@code auth:refresh:{memberId}})
 * <p>{@link com.dongnemarket.auth.service.RefreshTokenService}는 이 인터페이스에만 의존하므로
 * 저장소를 교체해도 서비스/컨트롤러는 영향받지 않는다.
 */
public interface RefreshTokenRepository {

	Optional<RefreshToken> findByMemberId(Long memberId);

	/** 이미 같은 memberId의 row/키가 있으면 교체하고, 없으면 새로 만든다(upsert). */
	RefreshToken save(RefreshToken refreshToken);

	void deleteByMemberId(Long memberId);
}
