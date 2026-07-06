package com.dongnemarket.member.service;

import com.dongnemarket.auth.service.RefreshTokenService;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.dto.MemberResponse;
import com.dongnemarket.member.dto.MemberUpdateRequest;
import com.dongnemarket.member.dto.PasswordChangeRequest;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;

	public MemberService(
			MemberRepository memberRepository,
			PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokenService) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
	}

	public MemberResponse getMyInfo(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		validateActiveMember(member);
		return MemberResponse.from(member);
	}

	@Transactional
	public MemberResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		validateActiveMember(member);

		if (memberRepository.existsByNicknameAndIdNot(request.getNickname(), memberId)) {
			throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
		}

		member.update(request.getNickname());
		return MemberResponse.from(member);
	}

	/** 비밀번호 변경 성공 시 저장된 Refresh Token을 삭제해 다른 세션에서도 재로그인을 유도한다(단일 세션 정책이라 사실상 모든 세션이 갱신됨). */
	@Transactional
	public void changePassword(Long memberId, PasswordChangeRequest request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		validateActiveMember(member);

		if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_PASSWORD);
		}
		if (passwordEncoder.matches(request.getNewPassword(), member.getPassword())) {
			throw new BusinessException(ErrorCode.SAME_AS_OLD_PASSWORD);
		}

		member.changePassword(passwordEncoder.encode(request.getNewPassword()));
		refreshTokenService.deleteByMemberId(memberId);
	}

	@Transactional
	public void deleteMyInfo(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		validateActiveMember(member);
		member.softDelete();
	}

	private void validateActiveMember(Member member) {
		if (member.getStatus() == MemberStatus.DELETED) {
			throw new BusinessException(ErrorCode.DELETED_MEMBER);
		}
		if (member.getStatus() == MemberStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.SUSPENDED_MEMBER);
		}
	}
}
