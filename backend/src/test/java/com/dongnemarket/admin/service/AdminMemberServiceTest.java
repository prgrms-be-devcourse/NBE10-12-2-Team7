package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminMemberResponse;
import com.dongnemarket.admin.repository.AdminMemberRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
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
}