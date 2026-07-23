package com.dongnemarket.auth.service;

import com.dongnemarket.auth.client.OAuthUserIdentity;
import com.dongnemarket.auth.entity.MemberSocialAccount;
import com.dongnemarket.auth.entity.OAuthProvider;
import com.dongnemarket.auth.repository.MemberSocialAccountRepository;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * 소셜 로그인 최초 가입의 트랜잭션 경계를 {@link com.dongnemarket.auth.service.AuthService}(비-트랜잭션
 * orchestrator)로부터 분리한 컴포넌트. {@link #signUp}과 {@link #reconcileAfterConflict}는 서로 다른
 * Spring 트랜잭션으로 실행돼야 하므로 반드시 별도 빈으로 존재한다 — 같은 빈 안에서 self-invocation으로
 * 호출하면 `@Transactional` 프록시가 가로채지 못해 트랜잭션 경계가 분리되지 않는다.
 */
@Component
public class OAuthSignupTransaction {

	private static final int DUMMY_PASSWORD_BYTES = 32;
	private static final int NICKNAME_RANDOM_LENGTH = 10;
	private static final String NICKNAME_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

	private final MemberRepository memberRepository;
	private final MemberSocialAccountRepository memberSocialAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();

	public OAuthSignupTransaction(MemberRepository memberRepository,
			MemberSocialAccountRepository memberSocialAccountRepository, PasswordEncoder passwordEncoder) {
		this.memberRepository = memberRepository;
		this.memberSocialAccountRepository = memberSocialAccountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Member와 MemberSocialAccount를 하나의 신규 가입 트랜잭션으로 생성한다. 성공하거나 전체 롤백된다.
	 * <p>UNIQUE 위반(email, provider+provider_user_id, 극히 드물게 nickname)은 여기서 잡지 않고 그대로
	 * 던진다 — 이 메서드가 예외로 끝나면 Spring이 이 트랜잭션 전체를 롤백한 뒤 예외를 호출부로 전파한다.
	 * 호출부(트랜잭션 밖)가 {@link #reconcileAfterConflict}로 재조회를 이어간다.
	 */
	@Transactional
	public Member signUp(OAuthUserIdentity identity) {
		String dummyPassword = passwordEncoder.encode(generateDummySecret());
		String nickname = generateNickname(identity.provider());
		Member member = Member.createSocialUser(identity.email(), dummyPassword, nickname);
		memberRepository.save(member);
		memberSocialAccountRepository.save(MemberSocialAccount.of(member, identity.provider(), identity.providerUserId()));
		return member;
	}

	/**
	 * {@link #signUp}이 UNIQUE 위반으로 롤백된 뒤, 완전히 새로운 read-only 트랜잭션에서
	 * {@code (provider, provider_user_id)}를 재조회한다. 동시 요청이 먼저 만든 연동이면 그 회원을
	 * 반환하고(로그인으로 이어감), 없으면 다른 제약(이메일 등) 충돌이라는 뜻이라 empty를 반환한다
	 * (호출부가 {@code OAUTH_EMAIL_CONFLICT}로 처리).
	 */
	@Transactional(readOnly = true)
	public Optional<Member> reconcileAfterConflict(OAuthProvider provider, String providerUserId) {
		return memberSocialAccountRepository.findByProviderAndProviderUserIdFetchMember(provider, providerUserId)
				.map(MemberSocialAccount::getMember);
	}

	/** 로그인에 쓰이지 않는 더미 비밀번호. 원문은 encode() 직후 버려지고 저장·로그되지 않는다. */
	private String generateDummySecret() {
		byte[] bytes = new byte[DUMMY_PASSWORD_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** provider명 접두어 + 무작위 문자열. members.nickname UNIQUE(20자 제한)에 여유 있게 들어간다. */
	private String generateNickname(OAuthProvider provider) {
		StringBuilder random = new StringBuilder(NICKNAME_RANDOM_LENGTH);
		for (int i = 0; i < NICKNAME_RANDOM_LENGTH; i++) {
			random.append(NICKNAME_ALPHABET.charAt(secureRandom.nextInt(NICKNAME_ALPHABET.length())));
		}
		return provider.name().toLowerCase() + "_" + random;
	}
}
