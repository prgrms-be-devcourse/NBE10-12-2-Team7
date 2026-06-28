package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminMemberResponse;
import com.dongnemarket.admin.repository.AdminMemberRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminMemberService {

    private final AdminMemberRepository adminMemberRepository;

    public AdminMemberService(AdminMemberRepository adminMemberRepository) {
        this.adminMemberRepository = adminMemberRepository;
    }

    /** 전체 회원 목록 (상태 무관) */
    public List<AdminMemberResponse> getMembers() {
        return adminMemberRepository.findAll().stream()
                .map(AdminMemberResponse::from)
                .toList();
    }

    /** 회원 단건 상세 */
    public AdminMemberResponse getMember(Long memberId) {
        Member member = adminMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return AdminMemberResponse.from(member);
    }
}