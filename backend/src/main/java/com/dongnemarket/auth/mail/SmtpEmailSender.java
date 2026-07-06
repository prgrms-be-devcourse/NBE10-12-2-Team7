package com.dongnemarket.auth.mail;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);
	private static final String SENDER_DISPLAY_NAME = "마켓온";

	private final JavaMailSender javaMailSender;
	private final String fromAddress;

	public SmtpEmailSender(JavaMailSender javaMailSender, @Value("${spring.mail.username}") String fromAddress) {
		this.javaMailSender = javaMailSender;
		this.fromAddress = fromAddress;
	}

	@Override
	public void send(String to, String subject, String content) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(SENDER_DISPLAY_NAME + " <" + fromAddress + ">");
		message.setTo(to);
		message.setSubject(subject);
		message.setText(content);

		try {
			javaMailSender.send(message);
		} catch (MailException e) {
			log.error("이메일 발송 실패: to={}", to, e);
			throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
		}
	}
}
