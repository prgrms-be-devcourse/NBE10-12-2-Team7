package com.dongnemarket.member.entity;

/**
 * 회원 권한. JwtTokenProvider 에 전달될 때는 {@code name()} 그대로 ("ROLE_USER"/"ROLE_ADMIN") 사용한다.
 */
public enum Role {
	ROLE_USER,
	ROLE_ADMIN
}
