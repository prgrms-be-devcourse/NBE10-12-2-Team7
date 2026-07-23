package com.dongnemarket.auth.dto;

/** 프론트가 그대로 리다이렉트할 수 있는 완성된 인가 URL과 발급된 state, state 만료까지 남은 시간. */
public record OAuthAuthorizationStart(String authorizationUrl, String state, long expiresInSeconds) {
}
