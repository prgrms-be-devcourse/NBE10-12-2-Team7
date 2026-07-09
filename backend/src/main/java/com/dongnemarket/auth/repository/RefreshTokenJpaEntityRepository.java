package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 실제 JPA 접근을 담당하는 내부 인터페이스. {@link JpaRefreshTokenRepository}({@code test} 프로파일 전용)만
 * 이 인터페이스에 위임한다. 프로파일 제한이 없어 다른 프로파일에서도 빈은 생성되지만, 아무도 주입받지
 * 않으므로 무해하다(Spring Data JPA 프록시 생성 비용만 있고 실제 쿼리는 발생하지 않는다).
 */
public interface RefreshTokenJpaEntityRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByMemberId(Long memberId);

	void deleteByMemberId(Long memberId);
}
