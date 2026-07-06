package com.dongnemarket.auth.controller;

import com.dongnemarket.auth.entity.EmailVerification;
import com.dongnemarket.auth.mail.EmailSender;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.auth.repository.PasswordResetTokenRepository;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SMTP 발송 대신 {@link EmailSender}를 Mock으로 대체해 토큰 생성/저장/쿨다운/확인 로직만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	EmailVerificationRepository emailVerificationRepository;

	@Autowired
	PasswordResetTokenRepository passwordResetTokenRepository;

	@MockitoBean
	EmailSender emailSender;

	@AfterEach
	void cleanUp() {
		passwordResetTokenRepository.deleteAll();
		emailVerificationRepository.deleteAll();
		memberRepository.deleteAll();
	}

	private void verifyEmail(String email) {
		EmailVerification verification = EmailVerification.issue(
				email, "000000", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
		verification.verify(LocalDateTime.now());
		emailVerificationRepository.save(verification);
	}

	private void signup(String email, String password, String nickname) throws Exception {
		verifyEmail(email);
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("{\"email\":\"%s\",\"password\":\"%s\",\"nickname\":\"%s\"}", email, password, nickname)));
	}

	// ===== POST /api/auth/password-resets =====

	@Test
	@DisplayName("가입된 이메일로 요청하면 200과 중립 메시지를 반환하고, 응답 body에는 토큰을 전혀 포함하지 않는다")
	void requestReset_registeredEmail_returns200AndSends() throws Exception {
		signup("reset-target@example.com", "password123!", "resetTarget");

		mockMvc.perform(post("/api/auth/password-resets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"email\": \"reset-target@example.com\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(emailSender).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("가입되지 않은 이메일로 요청해도 200과 동일한 중립 메시지를 반환하고 발송하지 않는다(계정 존재 노출 방지)")
	void requestReset_unregisteredEmail_returns200SameMessageWithoutSending() throws Exception {
		mockMvc.perform(post("/api/auth/password-resets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"email\": \"never-signed-up@example.com\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("해당 이메일로 가입된 계정이 있다면 비밀번호 재설정 메일을 발송했습니다."));

		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("이메일 형식이 올바르지 않으면 400과 INVALID_INPUT_VALUE를 반환한다")
	void requestReset_invalidEmailFormat_returns400() throws Exception {
		mockMvc.perform(post("/api/auth/password-resets")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"email\": \"not-an-email\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	// ===== POST /api/auth/password-resets/confirm =====

	/** DB에는 해시만 저장되므로, Mock으로 대체한 EmailSender에 실제로 전달된 메일 본문의 링크에서 원문 토큰을 꺼낸다. */
	private String requestResetAndGetToken(String email) throws Exception {
		mockMvc.perform(post("/api/auth/password-resets")
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("{ \"email\": \"%s\" }", email)));

		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailSender).send(anyString(), anyString(), bodyCaptor.capture());
		String body = bodyCaptor.getValue();
		int index = body.indexOf("?token=");
		return body.substring(index + "?token=".length()).split("\\s", 2)[0];
	}

	@Test
	@DisplayName("유효한 토큰으로 확인하면 200을 반환하고 새 비밀번호로 로그인할 수 있다")
	void confirmReset_validToken_success() throws Exception {
		signup("reset-confirm@example.com", "password123!", "resetConfirm");
		String token = requestResetAndGetToken("reset-confirm@example.com");

		mockMvc.perform(post("/api/auth/password-resets/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format("{ \"token\": \"%s\", \"newPassword\": \"newPassword123!\" }", token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").doesNotExist());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"email\": \"reset-confirm@example.com\", \"password\": \"newPassword123!\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists());
	}

	@Test
	@DisplayName("같은 토큰으로 다시 확인하면(1회용 소진) 400과 INVALID_RESET_TOKEN을 반환한다")
	void confirmReset_reusedToken_returns400() throws Exception {
		signup("reset-reuse@example.com", "password123!", "resetReuse");
		String token = requestResetAndGetToken("reset-reuse@example.com");
		mockMvc.perform(post("/api/auth/password-resets/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("{ \"token\": \"%s\", \"newPassword\": \"newPassword123!\" }", token)));

		mockMvc.perform(post("/api/auth/password-resets/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format("{ \"token\": \"%s\", \"newPassword\": \"anotherPassword123!\" }", token)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_RESET_TOKEN"));
	}

	@Test
	@DisplayName("존재하지 않는 토큰으로 확인하면 400과 INVALID_RESET_TOKEN을 반환한다")
	void confirmReset_unknownToken_returns400() throws Exception {
		mockMvc.perform(post("/api/auth/password-resets/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"token\": \"unknown-token\", \"newPassword\": \"newPassword123!\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_RESET_TOKEN"));
	}

	@Test
	@DisplayName("새 비밀번호가 정책에 맞지 않으면 400과 INVALID_INPUT_VALUE를 반환한다")
	void confirmReset_invalidNewPasswordFormat_returns400() throws Exception {
		mockMvc.perform(post("/api/auth/password-resets/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"token\": \"any-token\", \"newPassword\": \"nospecialchar123\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}
}
