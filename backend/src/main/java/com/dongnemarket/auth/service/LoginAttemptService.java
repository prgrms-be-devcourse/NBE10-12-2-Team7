package com.dongnemarket.auth.service;

import com.dongnemarket.auth.repository.LoginAttemptRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 로그인 실패 횟수를 세어 무차별 대입(brute-force) 시도를 차단한다.
 * 회원당(이메일당) 1개 카운터를 유지하며, 정해진 윈도우 내 실패 횟수가 임계값에 도달하면 그 윈도우가
 * 끝날 때까지 로그인 자체를 거부한다.
 */
@Service
public class LoginAttemptService {

	private static final long MAX_ATTEMPTS = 5;
	private static final Duration LOCK_WINDOW = Duration.ofMinutes(10);

	private final LoginAttemptRepository loginAttemptRepository;

	public LoginAttemptService(LoginAttemptRepository loginAttemptRepository) {
		this.loginAttemptRepository = loginAttemptRepository;
	}

	/** 임계값에 도달했으면 TOO_MANY_LOGIN_ATTEMPTS 예외를 던진다(실패 횟수를 추가로 늘리지 않는다). */
	public void assertNotBlocked(String email) {
		if (loginAttemptRepository.getFailureCount(email) >= MAX_ATTEMPTS) {
			throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
		}
	}

	/** 로그인 실패(이메일 없음/비밀번호 불일치) 시 호출: 실패 횟수를 1 증가시킨다. */
	public void recordFailure(String email) {
		loginAttemptRepository.incrementFailure(email, LOCK_WINDOW);
	}

	/** 로그인 성공 시 호출: 실패 횟수를 초기화한다. */
	public void recordSuccess(String email) {
		loginAttemptRepository.resetFailure(email);
	}
}
