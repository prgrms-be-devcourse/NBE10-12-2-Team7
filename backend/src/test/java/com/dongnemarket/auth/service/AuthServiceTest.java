package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.SignupRequest;
import com.dongnemarket.auth.dto.SignupResponse;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	MemberRepository memberRepository;

	@Mock
	PasswordEncoder passwordEncoder;

	@InjectMocks
	AuthService authService;

	@Test
	@DisplayName("이메일·닉네임이 중복되지 않으면 회원가입에 성공한다")
	void signup_success() {
		SignupRequest request = new SignupRequest("test@example.com", "password123", "tester");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
		given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-password");
		given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = authService.signup(request);

		assertThat(response.getEmail()).isEqualTo(request.getEmail());
		assertThat(response.getNickname()).isEqualTo(request.getNickname());
	}

	@Test
	@DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL 예외가 발생한다")
	void signup_duplicateEmail_throwsException() {
		SignupRequest request = new SignupRequest("test@example.com", "password123", "tester");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

		verify(memberRepository, never()).save(any());
	}

	@Test
	@DisplayName("이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외가 발생한다")
	void signup_duplicateNickname_throwsException() {
		SignupRequest request = new SignupRequest("test@example.com", "password123", "tester");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);

		verify(memberRepository, never()).save(any());
	}

	@Test
	@DisplayName("중복 체크 통과 후 save() 시점에 이메일 unique 제약을 위반하면(race condition) DUPLICATE_EMAIL로 변환한다")
	void signup_raceConditionDuplicateEmail_throwsDuplicateEmail() {
		SignupRequest request = new SignupRequest("race@example.com", "password123", "racer");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false, true);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
		given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-password");
		given(memberRepository.save(any(Member.class))).willThrow(new DataIntegrityViolationException("duplicate entry"));

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
	}

	@Test
	@DisplayName("중복 체크 통과 후 save() 시점에 닉네임 unique 제약을 위반하면(race condition) DUPLICATE_NICKNAME으로 변환한다")
	void signup_raceConditionDuplicateNickname_throwsDuplicateNickname() {
		SignupRequest request = new SignupRequest("racer2@example.com", "password123", "raceNick");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(false, true);
		given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-password");
		given(memberRepository.save(any(Member.class))).willThrow(new DataIntegrityViolationException("duplicate entry"));

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
	}

	@Test
	@DisplayName("원인을 식별할 수 없는 무결성 제약 위반은 원본 예외를 그대로 던진다")
	void signup_unclassifiableIntegrityViolation_rethrowsOriginal() {
		SignupRequest request = new SignupRequest("unknown@example.com", "password123", "unknown");
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
		given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-password");
		DataIntegrityViolationException original = new DataIntegrityViolationException("unknown constraint");
		given(memberRepository.save(any(Member.class))).willThrow(original);

		assertThatThrownBy(() -> authService.signup(request))
				.isSameAs(original);
	}
}
