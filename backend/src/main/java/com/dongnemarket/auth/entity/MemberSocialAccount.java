package com.dongnemarket.auth.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import com.dongnemarket.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 회원과 소셜 로그인 제공자 계정의 연동 정보. 한 회원이 provider별로 최대 1개까지 연동할 수 있다
 * (동일 provider 중복 연동은 DB unique 제약으로 차단). 연동 시각은 {@link BaseTimeEntity#getCreatedAt()}로 갈음한다.
 * <p>unique 제약을 엔티티에도 선언해 dev(ddl-auto=update)와 prod(Flyway, {@code V3__add_social_login_support.sql})
 * 양쪽에서 동일하게 DB 레벨로 강제되도록 한다 — 한쪽에만 있으면 동시성 처리(UNIQUE 위반 재조회)가
 * 환경별로 다르게 동작한다.
 */
@Entity
@Table(name = "member_social_accounts", uniqueConstraints = {
		@UniqueConstraint(name = "uk_social_account_provider_provider_user_id", columnNames = {"provider", "provider_user_id"}),
		@UniqueConstraint(name = "uk_social_account_member_provider", columnNames = {"member_id", "provider"})
})
public class MemberSocialAccount extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OAuthProvider provider;

	@Column(name = "provider_user_id", nullable = false, length = 255)
	private String providerUserId;

	protected MemberSocialAccount() {
	}

	private MemberSocialAccount(Member member, OAuthProvider provider, String providerUserId) {
		this.member = member;
		this.provider = provider;
		this.providerUserId = providerUserId;
	}

	public static MemberSocialAccount of(Member member, OAuthProvider provider, String providerUserId) {
		return new MemberSocialAccount(member, provider, providerUserId);
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public OAuthProvider getProvider() {
		return provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}
}
