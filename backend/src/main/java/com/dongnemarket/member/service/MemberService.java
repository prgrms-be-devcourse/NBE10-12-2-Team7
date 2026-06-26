package com.dongnemarket.member.service;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.dto.MemberResponse;
import com.dongnemarket.member.dto.MemberUpdateRequest;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public MemberResponse getMyInfo(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		return MemberResponse.from(member);
	}

	@Transactional
	public MemberResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		if (memberRepository.existsByNicknameAndIdNot(request.getNickname(), memberId)) {
			throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
		}

		member.update(request.getNickname());
		return MemberResponse.from(member);
	}

	@Transactional
	public void deleteMyInfo(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		member.softDelete();
	}
}
