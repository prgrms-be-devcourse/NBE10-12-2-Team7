package com.dongnemarket.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.dongnemarket.global.config.JpaAuditingConfig;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:member_location_repository_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class MemberLocationRepositoryTest {

	@Autowired
	MemberLocationRepository memberLocationRepository;

	@Autowired
	MemberRepository memberRepository;

	@Test
	@DisplayName("회원 동네 목록을 정렬 순서 오름차순으로 조회한다")
	void findsMemberLocationsOrderBySortOrderAsc() {
		Member member = saveMember("order");
		MemberLocation second = memberLocationRepository.save(MemberLocation.create(member, "서울 마포구", 1, false));
		MemberLocation first = memberLocationRepository.save(MemberLocation.create(member, "서울 강남구", 0, true));

		List<MemberLocation> locations = memberLocationRepository.findAllByMemberIdOrderBySortOrderAsc(member.getId());

		assertThat(locations).containsExactly(first, second);
	}

	@Test
	@DisplayName("회원 ID 기준으로 대상 회원 동네만 삭제하고 다른 회원 동네는 유지한다")
	void deletesOnlyTargetMemberLocationsByMemberId() {
		Member targetMember = saveMember("delete-target");
		Member otherMember = saveMember("delete-other");
		memberLocationRepository.save(MemberLocation.create(targetMember, "서울 강남구", 0, true));
		memberLocationRepository.save(MemberLocation.create(targetMember, "서울 마포구", 1, false));
		memberLocationRepository.save(MemberLocation.create(otherMember, "서울 송파구", 0, true));

		memberLocationRepository.deleteAllByMemberId(targetMember.getId());

		assertThat(memberLocationRepository.findAllByMemberIdOrderBySortOrderAsc(targetMember.getId())).isEmpty();
		List<MemberLocation> otherLocations = memberLocationRepository.findAllByMemberIdOrderBySortOrderAsc(otherMember.getId());
		assertThat(otherLocations).hasSize(1);
		assertThat(otherLocations.get(0).getRegion()).isEqualTo("서울 송파구");
		assertThat(otherLocations.get(0).isActive()).isTrue();
	}

	@Test
	@DisplayName("한 회원은 같은 지역을 중복 저장할 수 없다")
	void rejectsDuplicateRegionForSameMember() {
		Member member = saveMember("unique");
		memberLocationRepository.saveAndFlush(MemberLocation.create(member, "서울 강남구", 0, true));

		assertThatThrownBy(() -> {
			memberLocationRepository.save(MemberLocation.create(member, "서울 강남구", 1, false));
			memberLocationRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	private Member saveMember(String prefix) {
		String unique = UUID.randomUUID().toString();
		String email = prefix + "-" + unique + "@example.com";
		String nickname = prefix.substring(0, Math.min(prefix.length(), 8)) + unique.substring(0, 8);
		return memberRepository.save(Member.createUser(email, "encodedPassword", nickname));
	}
}
