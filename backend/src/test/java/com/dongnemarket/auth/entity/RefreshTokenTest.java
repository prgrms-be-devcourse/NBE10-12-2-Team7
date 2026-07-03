package com.dongnemarket.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

	// ===== issue =====

	@Test
	@DisplayName("issue로 생성하면 memberId·token·expiresAt이 그대로 설정된다")
	void issue_setsFields() {
		LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

		RefreshToken refreshToken = RefreshToken.issue(1L, "token-value", expiresAt);

		assertThat(refreshToken.getMemberId()).isEqualTo(1L);
		assertThat(refreshToken.getToken()).isEqualTo("token-value");
		assertThat(refreshToken.getExpiresAt()).isEqualTo(expiresAt);
	}

	// ===== replace =====

	@Test
	@DisplayName("replace를 호출하면 token과 expiresAt이 새 값으로 갱신된다")
	void replace_updatesTokenAndExpiresAt() {
		RefreshToken refreshToken = RefreshToken.issue(1L, "old-token", LocalDateTime.now());
		LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(7);

		refreshToken.replace("new-token", newExpiresAt);

		assertThat(refreshToken.getToken()).isEqualTo("new-token");
		assertThat(refreshToken.getExpiresAt()).isEqualTo(newExpiresAt);
	}

	// ===== matches =====

	@Test
	@DisplayName("matches는 저장된 토큰과 같은 문자열이면 true를 반환한다")
	void matches_sameToken_returnsTrue() {
		RefreshToken refreshToken = RefreshToken.issue(1L, "token-value", LocalDateTime.now().plusDays(7));

		assertThat(refreshToken.matches("token-value")).isTrue();
	}

	@Test
	@DisplayName("matches는 저장된 토큰과 다른 문자열이면 false를 반환한다")
	void matches_differentToken_returnsFalse() {
		RefreshToken refreshToken = RefreshToken.issue(1L, "token-value", LocalDateTime.now().plusDays(7));

		assertThat(refreshToken.matches("other-value")).isFalse();
	}
}
