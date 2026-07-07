package com.dongnemarket.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    // ===== createAdmin =====

    @Test
    @DisplayName("createAdmin으로 생성한 회원은 ROLE_ADMIN, ACTIVE 상태다")
    void createAdmin_success() {
        Member admin = Member.createAdmin("admin@example.com", "encoded-password", "관리자");

        assertThat(admin.getEmail()).isEqualTo("admin@example.com");
        assertThat(admin.getNickname()).isEqualTo("관리자");
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(admin.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(admin.getDeletedAt()).isNull();
    }

    // ===== changeStatus =====

    @Test
    @DisplayName("changeStatus(DELETED)는 status를 DELETED로 변경하고 deletedAt을 기록한다")
    void changeStatus_toDeleted_setsDeletedAt() {
        Member member = Member.createUser("user@example.com", "encoded-password", "tester");

        member.changeStatus(MemberStatus.DELETED);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
        assertThat(member.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("changeStatus(SUSPENDED)는 status를 SUSPENDED로 변경하고 deletedAt을 null로 초기화한다")
    void changeStatus_toSuspended_clearsDeletedAt() {
        Member member = Member.createUser("user@example.com", "encoded-password", "tester");
        member.changeStatus(MemberStatus.DELETED);

        member.changeStatus(MemberStatus.SUSPENDED);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(member.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("changeStatus(ACTIVE)는 status를 ACTIVE로 변경하고 deletedAt을 null로 초기화한다")
    void changeStatus_toActive_clearsDeletedAt() {
        Member member = Member.createUser("user@example.com", "encoded-password", "tester");
        member.changeStatus(MemberStatus.DELETED);

        member.changeStatus(MemberStatus.ACTIVE);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getDeletedAt()).isNull();
    }

    // ===== getDisplayNickname =====

    @Test
    @DisplayName("DELETED 회원의 getDisplayNickname()은 실제 닉네임 대신 \"탈퇴한 회원입니다\"를 반환한다")
    void getDisplayNickname_deleted_returnsFixedText() {
        Member member = Member.createUser("user@example.com", "encoded-password", "tester");
        member.changeStatus(MemberStatus.DELETED);

        assertThat(member.getDisplayNickname()).isEqualTo("탈퇴한 회원입니다");
    }

    @Test
    @DisplayName("ACTIVE/SUSPENDED 회원의 getDisplayNickname()은 실제 닉네임을 그대로 반환한다")
    void getDisplayNickname_activeOrSuspended_returnsRealNickname() {
        Member member = Member.createUser("user@example.com", "encoded-password", "tester");

        assertThat(member.getDisplayNickname()).isEqualTo("tester");

        member.changeStatus(MemberStatus.SUSPENDED);

        assertThat(member.getDisplayNickname()).isEqualTo("tester");
    }
}
