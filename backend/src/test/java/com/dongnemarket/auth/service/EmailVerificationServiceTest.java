package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.EmailVerificationRequest;
import com.dongnemarket.auth.dto.EmailVerificationResponse;
import com.dongnemarket.auth.entity.EmailVerification;
import com.dongnemarket.auth.mail.EmailSender;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

	@Mock
	EmailVerificationRepository emailVerificationRepository;

	@Mock
	MemberRepository memberRepository;

	@Mock
	EmailSender emailSender;

	EmailVerificationService emailVerificationService;

	@Test
	@DisplayName("가입되지 않은 이메일이고 기존 요청 내역이 없으면 신규 저장 후 발송한다")
	void requestVerification_newEmail_success() {
		emailVerificationService = new EmailVerificationService(emailVerificationRepository, memberRepository, emailSender);
		EmailVerificationRequest request = new EmailVerificationRequest("new@example.com");
		given(memberRepository.existsByEmail("new@example.com")).willReturn(false);
		given(emailVerificationRepository.findByEmail("new@example.com")).willReturn(Optional.empty());

		EmailVerificationResponse response = emailVerificationService.requestVerification(request);

		assertThat(response.getEmail()).isEqualTo("new@example.com");
		assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
		verify(emailVerificationRepository).save(any(EmailVerification.class));
		verify(emailSender).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL 예외가 발생하고 발송하지 않는다")
	void requestVerification_alreadyRegisteredEmail_throwsException() {
		emailVerificationService = new EmailVerificationService(emailVerificationRepository, memberRepository, emailSender);
		EmailVerificationRequest request = new EmailVerificationRequest("taken@example.com");
		given(memberRepository.existsByEmail("taken@example.com")).willReturn(true);

		assertThatThrownBy(() -> emailVerificationService.requestVerification(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

		verify(emailVerificationRepository, never()).save(any());
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("60초 이내에 재요청하면 EMAIL_VERIFICATION_REQUEST_TOO_SOON 예외가 발생하고 발송하지 않는다")
	void requestVerification_withinCooldown_throwsException() {
		emailVerificationService = new EmailVerificationService(emailVerificationRepository, memberRepository, emailSender);
		EmailVerificationRequest request = new EmailVerificationRequest("cooldown@example.com");
		given(memberRepository.existsByEmail("cooldown@example.com")).willReturn(false);
		EmailVerification existing = EmailVerification.issue(
				"cooldown@example.com", "111111", LocalDateTime.now().minusSeconds(10), LocalDateTime.now().plusMinutes(5));
		given(emailVerificationRepository.findByEmail("cooldown@example.com")).willReturn(Optional.of(existing));

		assertThatThrownBy(() -> emailVerificationService.requestVerification(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_VERIFICATION_REQUEST_TOO_SOON);

		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("쿨다운이 지난 뒤 재요청하면 기존 코드를 교체하고 재발송한다")
	void requestVerification_afterCooldown_replacesAndResends() {
		emailVerificationService = new EmailVerificationService(emailVerificationRepository, memberRepository, emailSender);
		EmailVerificationRequest request = new EmailVerificationRequest("resend@example.com");
		given(memberRepository.existsByEmail("resend@example.com")).willReturn(false);
		String oldCode = "111111";
		EmailVerification existing = EmailVerification.issue(
				"resend@example.com", oldCode, LocalDateTime.now().minusSeconds(61), LocalDateTime.now().plusMinutes(4));
		given(emailVerificationRepository.findByEmail("resend@example.com")).willReturn(Optional.of(existing));

		emailVerificationService.requestVerification(request);

		assertThat(existing.getCode()).isNotEqualTo(oldCode);
		verify(emailVerificationRepository, never()).save(any());
		verify(emailSender).send(anyString(), anyString(), anyString());
	}
}
