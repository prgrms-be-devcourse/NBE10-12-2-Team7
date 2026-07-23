package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.MemberSocialAccount;
import com.dongnemarket.auth.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

	/** fetch join으로 Member까지 한 번에 가져온다 — 소셜 로그인마다(가장 흔한 경로) 매번 호출되므로 N+1을 피한다. */
	@Query("select msa from MemberSocialAccount msa join fetch msa.member "
			+ "where msa.provider = :provider and msa.providerUserId = :providerUserId")
	Optional<MemberSocialAccount> findByProviderAndProviderUserIdFetchMember(
			@Param("provider") OAuthProvider provider, @Param("providerUserId") String providerUserId);
}
