package com.dongnemarket.member.service;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.dto.MemberResponse;
import com.dongnemarket.member.dto.MemberUpdateRequest;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@Mock
	MemberRepository memberRepository;

	@InjectMocks
	MemberService memberService;

	// ===== getMyInfo =====

	@Test
	@DisplayName("유효한 memberId로 조회하면 내 정보를 반환한다")
	void getMyInfo_success() {
		Member member = Member.createUser("test@example.com", "encoded-password", "tester");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		MemberResponse response = memberService.getMyInfo(1L);

		assertThat(response.getEmail()).isEqualTo("test@example.com");
		assertThat(response.getNickname()).isEqualTo("tester");
	}

	@Test
	@DisplayName("존재하지 않는 memberId로 조회하면 MEMBER_NOT_FOUND 예외가 발생한다")
	void getMyInfo_memberNotFound_throwsException() {
		given(memberRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> memberService.getMyInfo(999L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("탈퇴 회원이 내 정보를 조회하면 DELETED_MEMBER 예외가 발생한다")
	void getMyInfo_deletedMember_throwsException() {
		Member member = Member.createUser("test@example.com", "encoded", "nick");
		member.softDelete();
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> memberService.getMyInfo(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_MEMBER);
	}

	@Test
	@DisplayName("정지 회원이 내 정보를 조회하면 SUSPENDED_MEMBER 예외가 발생한다")
	void getMyInfo_suspendedMember_throwsException() {
		Member suspendedMember = Member.createUser("test@example.com", "encoded", "nick");
		suspendedMember.changeStatus(MemberStatus.SUSPENDED);
		given(memberRepository.findById(1L)).willReturn(Optional.of(suspendedMember));

		assertThatThrownBy(() -> memberService.getMyInfo(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUSPENDED_MEMBER);
	}

	// ===== updateMyInfo =====

	@Test
	@DisplayName("중복되지 않는 닉네임으로 수정하면 변경된 내 정보를 반환한다")
	void updateMyInfo_success() {
		Member member = Member.createUser("test@example.com", "encoded-password", "oldNick");
		MemberUpdateRequest request = new MemberUpdateRequest("newNick");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(memberRepository.existsByNicknameAndIdNot("newNick", 1L)).willReturn(false);

		MemberResponse response = memberService.updateMyInfo(1L, request);

		assertThat(response.getNickname()).isEqualTo("newNick");
		assertThat(response.getEmail()).isEqualTo("test@example.com");
	}

	@Test
	@DisplayName("존재하지 않는 memberId로 수정하면 MEMBER_NOT_FOUND 예외가 발생한다")
	void updateMyInfo_memberNotFound_throwsException() {
		MemberUpdateRequest request = new MemberUpdateRequest("newNick");
		given(memberRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> memberService.updateMyInfo(999L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("다른 회원이 사용 중인 닉네임으로 수정하면 DUPLICATE_NICKNAME 예외가 발생한다")
	void updateMyInfo_duplicateNickname_throwsException() {
		Member member = Member.createUser("test@example.com", "encoded-password", "myNick");
		MemberUpdateRequest request = new MemberUpdateRequest("takenNick");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(memberRepository.existsByNicknameAndIdNot("takenNick", 1L)).willReturn(true);

		assertThatThrownBy(() -> memberService.updateMyInfo(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
	}

	@Test
	@DisplayName("탈퇴 회원이 내 정보를 수정하면 DELETED_MEMBER 예외가 발생한다")
	void updateMyInfo_deletedMember_throwsException() {
		Member member = Member.createUser("test@example.com", "encoded", "nick");
		member.softDelete();
		MemberUpdateRequest request = new MemberUpdateRequest("newNick");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> memberService.updateMyInfo(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_MEMBER);
	}

	@Test
	@DisplayName("정지 회원이 내 정보를 수정하면 SUSPENDED_MEMBER 예외가 발생한다")
	void updateMyInfo_suspendedMember_throwsException() {
		Member suspendedMember = Member.createUser("test@example.com", "encoded", "nick");
		suspendedMember.changeStatus(MemberStatus.SUSPENDED);
		MemberUpdateRequest request = new MemberUpdateRequest("newNick");
		given(memberRepository.findById(1L)).willReturn(Optional.of(suspendedMember));

		assertThatThrownBy(() -> memberService.updateMyInfo(1L, request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUSPENDED_MEMBER);
	}

	// ===== deleteMyInfo =====

	@Test
	@DisplayName("회원 탈퇴 시 status가 DELETED로 변경되고 deletedAt이 설정된다")
	void deleteMyInfo_success() {
		Member member = Member.createUser("test@example.com", "encoded-password", "tester");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		memberService.deleteMyInfo(1L);

		assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
		assertThat(member.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("존재하지 않는 memberId로 탈퇴하면 MEMBER_NOT_FOUND 예외가 발생한다")
	void deleteMyInfo_memberNotFound_throwsException() {
		given(memberRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> memberService.deleteMyInfo(999L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("이미 탈퇴한 회원이 탈퇴 요청하면 DELETED_MEMBER 예외가 발생한다")
	void deleteMyInfo_alreadyDeleted_throwsException() {
		Member member = Member.createUser("test@example.com", "encoded", "nick");
		member.softDelete();
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> memberService.deleteMyInfo(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_MEMBER);
	}

	@Test
	@DisplayName("정지 회원이 탈퇴 요청하면 SUSPENDED_MEMBER 예외가 발생한다")
	void deleteMyInfo_suspendedMember_throwsException() {
		Member suspendedMember = Member.createUser("test@example.com", "encoded", "nick");
		suspendedMember.changeStatus(MemberStatus.SUSPENDED);
		given(memberRepository.findById(1L)).willReturn(Optional.of(suspendedMember));

		assertThatThrownBy(() -> memberService.deleteMyInfo(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUSPENDED_MEMBER);
	}
}
