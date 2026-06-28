package com.dongnemarket.admin.dto;

import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.entity.Role;

import java.time.LocalDateTime;

/**
 * 관리자용 회원 응답. 관리 목적상 deletedAt 까지 포함한다.
 */
public class AdminMemberResponse {

    private final Long memberId;
    private final String email;
    private final String nickname;
    private final Role role;
    private final MemberStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;

    private AdminMemberResponse(Long memberId, String email, String nickname, Role role,
                                MemberStatus status, LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.memberId = memberId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getDeletedAt()
        );
    }

    public Long getMemberId() { return memberId; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public Role getRole() { return role; }
    public MemberStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}