package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.PasswordResetConfirmRequest;
import com.dongnemarket.auth.dto.PasswordResetRequest;
import com.dongnemarket.auth.entity.PasswordResetToken;
import com.dongnemarket.auth.mail.EmailSender;
import com.dongnemarket.auth.repository.PasswordResetTokenRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

	private static final String FRONTEND_BASE_URL = "http://localhost:3000";

	@Mock
	PasswordResetTokenRepository passwordResetTokenRepository;

	@Mock
	MemberRepository memberRepository;

	@Mock
	RefreshTokenService refreshTokenService;

	@Mock
	EmailSender emailSender;

	PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	PasswordResetService passwordResetService;

	private PasswordResetService service() {
		return new PasswordResetService(
				passwordResetTokenRepository, memberRepository, passwordEncoder, refreshTokenService, emailSender,
				FRONTEND_BASE_URL);
	}

	private static String sha256(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/** 발송된 메일 본문의 링크(?token=...)에서 원문 토큰을 꺼낸다. */
	private String captureSentRawToken() {
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailSender).send(anyString(), anyString(), bodyCaptor.capture());
		String body = bodyCaptor.getValue();
		int index = body.indexOf("?token=");
		String afterToken = body.substring(index + "?token=".length());
		return afterToken.split("\\s", 2)[0];
	}

	// ===== requestReset =====

	@Test
	@DisplayName("가입된 이메일이면 재설정 토큰 해시를 저장하고, 이메일에는 원문 토큰이 담긴 링크를 발송한다")
	void requestReset_registeredEmail_issuesAndSendsToken() {
		passwordResetService = service();
		Member member = Member.createUser("test@example.com", "encoded", "tester");
		ReflectionTestUtils.setField(member, "id", 1L);
		given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
		given(passwordResetTokenRepository.findByMemberId(1L)).willReturn(Optional.empty());

		passwordResetService.requestReset(new PasswordResetRequest("test@example.com"));

		ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(passwordResetTokenRepository).save(tokenCaptor.capture());
		String rawToken = captureSentRawToken();
		assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(sha256(rawToken));
	}

	@Test
	@DisplayName("메일 본문의 재설정 링크는 프론트 고정 주소 + query parameter(?token=)로 구성된다")
	void requestReset_emailBody_containsQueryParamBasedLink() {
		passwordResetService = service();
		Member member = Member.createUser("link@example.com", "encoded", "tester");
		ReflectionTestUtils.setField(member, "id", 10L);
		given(memberRepository.findByEmail("link@example.com")).willReturn(Optional.of(member));
		given(passwordResetTokenRepository.findByMemberId(10L)).willReturn(Optional.empty());

		passwordResetService.requestReset(new PasswordResetRequest("link@example.com"));

		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailSender).send(anyString(), anyString(), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue()).contains(FRONTEND_BASE_URL + "/password-reset?token=");
	}

	@Test
	@DisplayName("가입되지 않은 이메일이면 예외 없이 조용히 종료하고 아무것도 저장·발송하지 않는다")
	void requestReset_unregisteredEmail_doesNothingSilently() {
		passwordResetService = service();
		given(memberRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

		passwordResetService.requestReset(new PasswordResetRequest("none@example.com"));

		verify(passwordResetTokenRepository, never()).save(any());
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("탈퇴한 회원이면 예외 없이 조용히 종료하고 아무것도 저장·발송하지 않는다")
	void requestReset_deletedMember_doesNothingSilently() {
		passwordResetService = service();
		Member member = Member.createUser("deleted@example.com", "encoded", "tester");
		member.softDelete();
		given(memberRepository.findByEmail("deleted@example.com")).willReturn(Optional.of(member));

		passwordResetService.requestReset(new PasswordResetRequest("deleted@example.com"));

		verify(passwordResetTokenRepository, never()).save(any());
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("정지된 회원이어도 재설정 토큰을 저장하고 발송한다")
	void requestReset_suspendedMember_issuesAndSendsToken() {
		passwordResetService = service();
		Member member = Member.createUser("suspended@example.com", "encoded", "tester");
		member.changeStatus(MemberStatus.SUSPENDED);
		ReflectionTestUtils.setField(member, "id", 2L);
		given(memberRepository.findByEmail("suspended@example.com")).willReturn(Optional.of(member));
		given(passwordResetTokenRepository.findByMemberId(2L)).willReturn(Optional.empty());

		passwordResetService.requestReset(new PasswordResetRequest("suspended@example.com"));

		verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
		verify(emailSender).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("60초 이내에 재요청하면 예외 없이 조용히 종료하고 재발송하지 않는다")
	void requestReset_withinCooldown_doesNotResend() {
		passwordResetService = service();
		Member member = Member.createUser("cooldown@example.com", "encoded", "tester");
		ReflectionTestUtils.setField(member, "id", 3L);
		given(memberRepository.findByEmail("cooldown@example.com")).willReturn(Optional.of(member));
		PasswordResetToken existing = PasswordResetToken.issue(
				3L, "old-hash", LocalDateTime.now().minusSeconds(10), LocalDateTime.now().plusMinutes(30));
		given(passwordResetTokenRepository.findByMemberId(3L)).willReturn(Optional.of(existing));

		passwordResetService.requestReset(new PasswordResetRequest("cooldown@example.com"));

		assertThat(existing.getTokenHash()).isEqualTo("old-hash");
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("쿨다운이 지난 뒤 재요청하면 기존 토큰 해시를 교체하고 재발송한다")
	void requestReset_afterCooldown_replacesAndResends() {
		passwordResetService = service();
		Member member = Member.createUser("resend@example.com", "encoded", "tester");
		ReflectionTestUtils.setField(member, "id", 4L);
		given(memberRepository.findByEmail("resend@example.com")).willReturn(Optional.of(member));
		String oldHash = "old-hash";
		PasswordResetToken existing = PasswordResetToken.issue(
				4L, oldHash, LocalDateTime.now().minusSeconds(61), LocalDateTime.now().plusMinutes(29));
		given(passwordResetTokenRepository.findByMemberId(4L)).willReturn(Optional.of(existing));

		passwordResetService.requestReset(new PasswordResetRequest("resend@example.com"));

		assertThat(existing.getTokenHash()).isNotEqualTo(oldHash);
		verify(passwordResetTokenRepository, never()).save(any());
		verify(emailSender).send(anyString(), anyString(), anyString());
	}

	// ===== confirmReset =====

	@Test
	@DisplayName("유효한 토큰이면 비밀번호를 변경하고 토큰을 즉시 삭제하며 Refresh Token을 삭제한다")
	void confirmReset_validToken_success() {
		passwordResetService = service();
		Member member = Member.createUser("confirm@example.com", "old-encoded", "tester");
		ReflectionTestUtils.setField(member, "id", 5L);
		String rawToken = "valid-raw-token";
		PasswordResetToken resetToken = PasswordResetToken.issue(
				5L, sha256(rawToken), LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(29));
		given(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).willReturn(Optional.of(resetToken));
		given(memberRepository.findById(5L)).willReturn(Optional.of(member));

		passwordResetService.confirmReset(new PasswordResetConfirmRequest(rawToken, "newPassword123!"));

		assertThat(passwordEncoder.matches("newPassword123!", member.getPassword())).isTrue();
		verify(passwordResetTokenRepository).delete(resetToken);
		verify(refreshTokenService).deleteByMemberId(5L);
	}

	@Test
	@DisplayName("존재하지 않는 토큰이면 INVALID_RESET_TOKEN 예외가 발생한다")
	void confirmReset_tokenNotFound_throwsException() {
		passwordResetService = service();
		given(passwordResetTokenRepository.findByTokenHash(sha256("unknown-token"))).willReturn(Optional.empty());

		assertThatThrownBy(() -> passwordResetService.confirmReset(
				new PasswordResetConfirmRequest("unknown-token", "newPassword123!")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESET_TOKEN);

		verify(refreshTokenService, never()).deleteByMemberId(anyLong());
	}

	@Test
	@DisplayName("이미 사용되어 폐기된(삭제된) 토큰으로 재시도하면 INVALID_RESET_TOKEN 예외가 발생한다")
	void confirmReset_alreadyUsedAndDeletedToken_throwsException() {
		passwordResetService = service();
		String rawToken = "used-raw-token";
		given(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).willReturn(Optional.empty());

		assertThatThrownBy(() -> passwordResetService.confirmReset(
				new PasswordResetConfirmRequest(rawToken, "newPassword123!")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESET_TOKEN);

		verify(refreshTokenService, never()).deleteByMemberId(anyLong());
	}

	@Test
	@DisplayName("만료된 토큰이면 EXPIRED_RESET_TOKEN 예외가 발생한다")
	void confirmReset_expiredToken_throwsException() {
		passwordResetService = service();
		String rawToken = "expired-raw-token";
		PasswordResetToken resetToken = PasswordResetToken.issue(
				7L, sha256(rawToken), LocalDateTime.now().minusMinutes(40), LocalDateTime.now().minusMinutes(10));
		given(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).willReturn(Optional.of(resetToken));

		assertThatThrownBy(() -> passwordResetService.confirmReset(
				new PasswordResetConfirmRequest(rawToken, "newPassword123!")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_RESET_TOKEN);

		verify(refreshTokenService, never()).deleteByMemberId(anyLong());
	}
}
