package com.dongnemarket.auth.mail;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

	@Mock
	JavaMailSender javaMailSender;

	SmtpEmailSender emailSender;

	@Test
	@DisplayName("발송에 성공하면 발신자 표시 이름 포함 발신자·수신자·제목·본문이 담긴 메일을 JavaMailSender로 전달한다")
	void send_success() {
		emailSender = new SmtpEmailSender(javaMailSender, "noreply@example.com");

		emailSender.send("test@example.com", "인증 코드", "코드: 123456");

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(javaMailSender).send(captor.capture());
		SimpleMailMessage sent = captor.getValue();
		assertThat(sent.getFrom()).isEqualTo("마켓온 <noreply@example.com>");
		assertThat(sent.getTo()).containsExactly("test@example.com");
		assertThat(sent.getSubject()).isEqualTo("인증 코드");
		assertThat(sent.getText()).isEqualTo("코드: 123456");
	}

	@Test
	@DisplayName("JavaMailSender가 발송에 실패하면 EMAIL_SEND_FAILED 예외로 변환한다")
	void send_mailServerError_throwsEmailSendFailed() {
		emailSender = new SmtpEmailSender(javaMailSender, "noreply@example.com");
		willThrow(new MailSendException("SMTP connection failed"))
				.given(javaMailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

		assertThatThrownBy(() -> emailSender.send("test@example.com", "인증 코드", "코드: 123456"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_SEND_FAILED);
	}
}
