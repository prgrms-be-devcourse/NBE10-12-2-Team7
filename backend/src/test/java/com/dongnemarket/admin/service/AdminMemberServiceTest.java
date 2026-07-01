package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminMemberResponse;
import com.dongnemarket.admin.dto.AdminMemberStatusUpdateRequest;
import com.dongnemarket.admin.repository.AdminMemberRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * [단위] AdminMemberService.changeMemberStatus — 서비스 고유 로직만 검증.
 *  - 검증 대상: parseStatus(파싱·검증) + NOT_FOUND 예외.
 *  - 제외: deletedAt 전이(Member.changeStatus → MemberTest 커버), getMembers/getMember 위임(통합 커버).
 */
@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock
    AdminMemberRepository adminMemberRepository;

    @InjectMocks
    AdminMemberService adminMemberService;

    private Member existingMember() {
        return Member.createUser("u@example.com", "encoded", "user");
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("앞뒤 공백이 있어도 trim 후 파싱되어 상태가 변경된다")
        void trimmedStatus_success() {
            Member member = existingMember();
            given(adminMemberRepository.findById(1L)).willReturn(Optional.of(member));

            AdminMemberResponse response =
                    adminMemberService.changeMemberStatus(1L, new AdminMemberStatusUpdateRequest("  SUSPENDED  "));

            assertThat(response.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        /**
         * null·공백·정의되지 않은 값·대소문자 불일치는 모두 같은 분기(INVALID_MEMBER_STATUS)로 귀결.
         * 같은 결과라 메서드를 쪼개지 않고 파라미터로 묶는다.
         * 주의: changeMemberStatus가 findById를 먼저 호출하므로 회원 존재를 스텁해야
         *       NOT_FOUND가 먼저 터지지 않는다.
         */
        @ParameterizedTest(name = "[{index}] status=\"{0}\" → INVALID_MEMBER_STATUS")
        @NullSource
        @ValueSource(strings = {"", "   ", "FOO", "suspended", "Active"})
        @DisplayName("상태값이 null·공백·오타·대소문자 불일치면 INVALID_MEMBER_STATUS")
        void invalidStatusValue_throwsInvalid(String status) {
            given(adminMemberRepository.findById(1L)).willReturn(Optional.of(existingMember()));

            assertThatThrownBy(() ->
                    adminMemberService.changeMemberStatus(1L, new AdminMemberStatusUpdateRequest(status)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MEMBER_STATUS);
        }

        @Test
        @DisplayName("요청 객체 자체가 null이어도 INVALID_MEMBER_STATUS로 방어한다")
        void nullRequest_throwsInvalid() {
            given(adminMemberRepository.findById(1L)).willReturn(Optional.of(existingMember()));

            assertThatThrownBy(() ->
                    adminMemberService.changeMemberStatus(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MEMBER_STATUS);
        }

        @Test
        @DisplayName("존재하지 않는 회원의 상태를 변경하면 MEMBER_NOT_FOUND 예외가 발생한다")
        void notFound_throwsException() {
            given(adminMemberRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    adminMemberService.changeMemberStatus(999L, new AdminMemberStatusUpdateRequest("SUSPENDED")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}