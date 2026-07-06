package com.dongnemarket.auth.controller;

import com.dongnemarket.auth.mail.EmailSender;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SMTP 발송 대신 {@link EmailSender}를 Mock으로 대체해 코드 생성/저장/쿨다운 로직만 검증한다.
 * (실제 메일 발송은 자격증명이 필요하므로 이 테스트 범위 밖 — docs/postman 시나리오에서 별도 확인)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailVerificationControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	EmailVerificationRepository emailVerificationRepository;

	@MockitoBean
	EmailSender emailSender;

	@AfterEach
	void cleanUp() {
		emailVerificationRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("가입되지 않은 이메일로 요청하면 201과 발송 예정 정보를 반환한다")
	void requestVerification_success() throws Exception {
		String body = "{ \"email\": \"new@example.com\" }";

		mockMvc.perform(post("/api/auth/email-verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(201))
				.andExpect(jsonPath("$.data.email").value("new@example.com"))
				.andExpect(jsonPath("$.data.expiresAt").exists());

		verify(emailSender).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("이미 가입된 이메일로 요청하면 409와 DUPLICATE_EMAIL을 반환한다")
	void requestVerification_duplicateEmail_returns409() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"email\": \"taken@example.com\", \"password\": \"password123!\", \"nickname\": \"taken\" }"));

		String body = "{ \"email\": \"taken@example.com\" }";
		mockMvc.perform(post("/api/auth/email-verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"));

		verify(emailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("60초 이내에 같은 이메일로 재요청하면 429와 EMAIL_VERIFICATION_REQUEST_TOO_SOON을 반환한다")
	void requestVerification_withinCooldown_returns429() throws Exception {
		String body = "{ \"email\": \"cooldown@example.com\" }";

		mockMvc.perform(post("/api/auth/email-verifications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));

		mockMvc.perform(post("/api/auth/email-verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error").value("EMAIL_VERIFICATION_REQUEST_TOO_SOON"));
	}

	@Test
	@DisplayName("이메일 형식이 올바르지 않으면 400과 INVALID_INPUT_VALUE를 반환한다")
	void requestVerification_invalidEmailFormat_returns400() throws Exception {
		String body = "{ \"email\": \"not-an-email\" }";

		mockMvc.perform(post("/api/auth/email-verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}
}
