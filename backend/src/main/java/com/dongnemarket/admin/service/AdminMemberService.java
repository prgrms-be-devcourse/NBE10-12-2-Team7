package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminMemberResponse;
import com.dongnemarket.admin.dto.AdminMemberStatusUpdateRequest;
import com.dongnemarket.admin.repository.AdminMemberRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
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

    /** 회원 상태 변경 (ACTIVE/SUSPENDED/DELETED) */
    @Transactional
    public AdminMemberResponse changeMemberStatus(Long memberId, AdminMemberStatusUpdateRequest request) {
        Member member = adminMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.changeStatus(parseStatus(request));
        return AdminMemberResponse.from(member);
    }

    private MemberStatus parseStatus(AdminMemberStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
        }
        try {
            return MemberStatus.valueOf(request.getStatus().trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
        }
    }
}