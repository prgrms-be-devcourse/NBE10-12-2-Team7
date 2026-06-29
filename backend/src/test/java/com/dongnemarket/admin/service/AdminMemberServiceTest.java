package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminMemberResponse;
import com.dongnemarket.admin.dto.AdminMemberStatusUpdateRequest;
import com.dongnemarket.admin.repository.AdminMemberRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock
    AdminMemberRepository adminMemberRepository;

    @InjectMocks
    AdminMemberService adminMemberService;

    @Test
    @DisplayName("회원 목록을 조회하면 상태와 무관하게 전체 회원을 반환한다")
    void getMembers_success() {
        Member active = Member.createUser("active@example.com", "encoded", "activeUser");
        Member deleted = Member.createUser("deleted@example.com", "encoded", "deletedUser");
        deleted.softDelete();
        given(adminMemberRepository.findAll()).willReturn(List.of(active, deleted));

        List<AdminMemberResponse> responses = adminMemberService.getMembers();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(AdminMemberResponse::getEmail)
                .containsExactly("active@example.com", "deleted@example.com");
    }

    @Test
    @DisplayName("회원이 없으면 빈 목록을 반환한다")
    void getMembers_empty_returnsEmptyList() {
        given(adminMemberRepository.findAll()).willReturn(List.of());

        List<AdminMemberResponse> responses = adminMemberService.getMembers();

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("존재하는 memberId로 상세 조회하면 해당 회원을 반환한다")
    void getMember_success() {
        Member member = Member.createUser("detail@example.com", "encoded", "detailUser");
        given(adminMemberRepository.findById(1L)).willReturn(Optional.of(member));

        AdminMemberResponse response = adminMemberService.getMember(1L);

        assertThat(response.getEmail()).isEqualTo("detail@example.com");
        assertThat(response.getNickname()).isEqualTo("detailUser");
    }

    @Test
    @DisplayName("존재하지 않는 memberId로 상세 조회하면 MEMBER_NOT_FOUND 예외가 발생한다")
    void getMember_notFound_throwsException() {
        given(adminMemberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminMemberService.getMember(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("회원 상태를 SUSPENDED로 변경하면 상태가 바뀐다")
    void changeMemberStatus_success() {
        Member member = Member.createUser("u@example.com", "encoded", "user");
        given(adminMemberRepository.findById(1L)).willReturn(Optional.of(member));

        AdminMemberResponse response =
                adminMemberService.changeMemberStatus(1L, new AdminMemberStatusUpdateRequest("SUSPENDED"));

        assertThat(response.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }

    @Test
    @DisplayName("회원 상태를 DELETED로 변경하면 deletedAt이 기록된다")
    void changeMemberStatus_toDeleted_setsDeletedAt() {
        Member member = Member.createUser("u@example.com", "encoded", "user");
        given(adminMemberRepository.findById(1L)).willReturn(Optional.of(member));

        adminMemberService.changeMemberStatus(1L, new AdminMemberStatusUpdateRequest("DELETED"));

        assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
        assertThat(member.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 회원의 상태 변경 시 MEMBER_NOT_FOUND 예외가 발생한다")
    void changeMemberStatus_notFound_throwsException() {
        given(adminMemberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminMemberService.changeMemberStatus(999L, new AdminMemberStatusUpdateRequest("SUSPENDED")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("잘못된 상태 값으로 변경 시 INVALID_MEMBER_STATUS 예외가 발생한다")
    void changeMemberStatus_invalidStatus_throwsException() {
        Member member = Member.createUser("u@example.com", "encoded", "user");
        given(adminMemberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> adminMemberService.changeMemberStatus(1L, new AdminMemberStatusUpdateRequest("INVALID")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MEMBER_STATUS);
    }
}