package com.dongnemarket.auth.client;

import com.dongnemarket.auth.entity.OAuthProvider;

/**
 * 소셜 로그인 제공자로부터 확인한 사용자 신원. {@code email}은 제공자가 검증 완료로 표시한 값만 여기
 * 담긴다(검증되지 않은 이메일은 클라이언트 구현체가 애초에 이 객체를 만들지 않고 예외를 던진다).
 */
public record OAuthUserIdentity(OAuthProvider provider, String providerUserId, String email) {
}
