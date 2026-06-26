package com.dongnemarket.auth.service;

import com.dongnemarket.auth.dto.LoginRequest;
import com.dongnemarket.auth.dto.LoginResponse;
import com.dongnemarket.auth.dto.SignupRequest;
import com.dongnemarket.auth.dto.SignupResponse;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		if (memberRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}
		if (memberRepository.existsByNickname(request.getNickname())) {
			throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
		}

		String encodedPassword = passwordEncoder.encode(request.getPassword());
		Member member = Member.createUser(request.getEmail(), encodedPassword, request.getNickname());

		try {
			Member savedMember = memberRepository.save(member);
			return SignupResponse.from(savedMember);
		} catch (DataIntegrityViolationException e) {
			throw resolveDuplicateException(request, e);
		}
	}

	public LoginResponse login(LoginRequest request) {
		Member member = memberRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		if (member.getStatus() == MemberStatus.DELETED) {
			throw new BusinessException(ErrorCode.DELETED_MEMBER);
		}
		if (member.getStatus() == MemberStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.SUSPENDED_MEMBER);
		}
		if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_PASSWORD);
		}

		String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
		return LoginResponse.of(token);
	}

	/** 중복 체크 이후 save() 사이의 race condition으로 unique 제약을 위반한 경우, 원인을 재조회해 알맞은 BusinessException으로 변환한다. */
	private BusinessException resolveDuplicateException(SignupRequest request, DataIntegrityViolationException e) {
		if (memberRepository.existsByEmail(request.getEmail())) {
			return new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}
		if (memberRepository.existsByNickname(request.getNickname())) {
			return new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
		}
		throw e;
	}
}
