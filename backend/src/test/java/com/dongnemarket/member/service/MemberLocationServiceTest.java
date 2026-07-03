package com.dongnemarket.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.dto.MemberLocationResponse;
import com.dongnemarket.member.dto.MemberLocationUpdateRequest;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberLocation;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberLocationRepository;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.region.repository.RegionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberLocationServiceTest {

	@Mock
	MemberRepository memberRepository;

	@Mock
	MemberLocationRepository memberLocationRepository;

	@Mock
	RegionRepository regionRepository;

	@InjectMocks
	MemberLocationService memberLocationService;

	@Test
	@DisplayName("동네 1개를 설정하면 해당 동네가 활성 동네로 저장된다")
	void updatesOneLocation() {
		Member member = Member.createUser("one@example.com", "encodedPassword", "oneUser");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(regionRepository.existsByName("서울 강남구")).willReturn(true);
		given(memberLocationRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		List<MemberLocationResponse> responses = memberLocationService.updateMyLocations(
				1L,
				new MemberLocationUpdateRequest(List.of("서울 강남구"))
		);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getRegion()).isEqualTo("서울 강남구");
		assertThat(responses.get(0).getSortOrder()).isZero();
		assertThat(responses.get(0).isActive()).isTrue();
		then(memberLocationRepository).should().deleteAllByMemberId(1L);
	}

	@Test
	@DisplayName("동네 2개를 설정하면 첫 번째 동네만 활성 동네로 저장된다")
	void updatesTwoLocations() {
		Member member = Member.createUser("two@example.com", "encodedPassword", "twoUser");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(regionRepository.existsByName("서울 강남구")).willReturn(true);
		given(regionRepository.existsByName("서울 마포구")).willReturn(true);
		given(memberLocationRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		List<MemberLocationResponse> responses = memberLocationService.updateMyLocations(
				1L,
				new MemberLocationUpdateRequest(List.of("서울 강남구", "서울 마포구"))
		);

		assertThat(responses).extracting(MemberLocationResponse::getRegion)
				.containsExactly("서울 강남구", "서울 마포구");
		assertThat(responses).extracting(MemberLocationResponse::getSortOrder)
				.containsExactly(0, 1);
		assertThat(responses).extracting(MemberLocationResponse::isActive)
				.containsExactly(true, false);
	}

	@Test
	@DisplayName("동네를 재설정하면 기존 동네를 전부 삭제하고 새 동네로 교체한다")
	void replacesExistingLocations() {
		Member member = Member.createUser("replace@example.com", "encodedPassword", "replaceUser");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(regionRepository.existsByName("서울 송파구")).willReturn(true);
		given(memberLocationRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		memberLocationService.updateMyLocations(
				1L,
				new MemberLocationUpdateRequest(List.of("서울 송파구"))
		);

		ArgumentCaptor<List<MemberLocation>> captor = ArgumentCaptor.forClass(List.class);
		then(memberLocationRepository).should().deleteAllByMemberId(1L);
		then(memberLocationRepository).should().saveAll(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).getRegion()).isEqualTo("서울 송파구");
	}

	@Test
	@DisplayName("설정한 동네가 없으면 빈 목록을 반환한다")
	void getsEmptyLocations() {
		Member member = Member.createUser("empty@example.com", "encodedPassword", "emptyUser");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(memberLocationRepository.findAllByMemberIdOrderBySortOrderAsc(1L)).willReturn(List.of());

		List<MemberLocationResponse> responses = memberLocationService.getMyLocations(1L);

		assertThat(responses).isEmpty();
	}

	@Test
	@DisplayName("같은 값으로 재설정해도 전체 교체 방식으로 정상 처리된다")
	void updatesSameLocationAgain() {
		Member member = Member.createUser("same@example.com", "encodedPassword", "sameUser");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(regionRepository.existsByName("서울 강남구")).willReturn(true);
		given(memberLocationRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		List<MemberLocationResponse> responses = memberLocationService.updateMyLocations(
				1L,
				new MemberLocationUpdateRequest(List.of("서울 강남구"))
		);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getRegion()).isEqualTo("서울 강남구");
		then(memberLocationRepository).should().deleteAllByMemberId(1L);
	}

	@Test
	@DisplayName("리스트 안에 중복 지역이 있으면 동네를 설정할 수 없다")
	void rejectsDuplicateRegions() {
		Member member = Member.createUser("duplicate@example.com", "encodedPassword", "duplicateUser");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> memberLocationService.updateMyLocations(
				1L,
				new MemberLocationUpdateRequest(List.of("서울 강남구", "서울 강남구"))
		)).isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	@DisplayName("지역 마스터에 없는 지역이면 동네를 설정할 수 없다")
	void rejectsUnknownRegion() {
		Member member = Member.createUser("unknown@example.com", "encodedPassword", "unknownUser");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(regionRepository.existsByName("강남")).willReturn(false);

		assertThatThrownBy(() -> memberLocationService.updateMyLocations(
				1L,
				new MemberLocationUpdateRequest(List.of("강남"))
		)).isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	@DisplayName("존재하지 않는 회원이면 동네를 설정할 수 없다")
	void rejectsMissingMemberOnUpdate() {
		given(memberRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> memberLocationService.updateMyLocations(
				1L,
				new MemberLocationUpdateRequest(List.of("서울 강남구"))
		)).isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("탈퇴 회원이면 동네를 조회할 수 없다")
	void rejectsDeletedMemberOnGet() {
		Member member = Member.createUser("deleted@example.com", "encodedPassword", "deletedUser");
		member.softDelete();
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> memberLocationService.getMyLocations(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELETED_MEMBER);
	}

	@Test
	@DisplayName("정지 회원이면 동네를 조회할 수 없다")
	void rejectsSuspendedMemberOnGet() {
		Member member = Member.createUser("suspended@example.com", "encodedPassword", "suspendedUser");
		member.changeStatus(MemberStatus.SUSPENDED);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> memberLocationService.getMyLocations(1L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUSPENDED_MEMBER);
	}
}
