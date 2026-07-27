package com.dongnemarket.member.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberLocation;
import com.dongnemarket.member.repository.MemberLocationRepository;
import com.dongnemarket.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberLocationRegionBackfillSeederTest {

	@Autowired
	MemberLocationRegionBackfillSeeder memberLocationRegionBackfillSeeder;

	@Autowired
	MemberLocationRepository memberLocationRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	EntityManager entityManager;

	@AfterEach
	void cleanUp() {
		memberLocationRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("region_id가 없는 기존 회원 동네는 문자열 지역을 Region.name으로 매칭해 백필한다")
	void backfillsMemberLocationRegionByLegacyRegionName() {
		MemberLocation memberLocation = memberLocationRepository.saveAndFlush(memberLocation("서울 강남구"));
		entityManager.clear();

		memberLocationRegionBackfillSeeder.seed();
		memberLocationRepository.flush();
		entityManager.clear();

		MemberLocation foundLocation = memberLocationRepository.findById(memberLocation.getId()).orElseThrow();
		assertThat(foundLocation.getRegionRef()).isNotNull();
		assertThat(foundLocation.getRegionCode()).isEqualTo("1168000000");
		assertThat(foundLocation.getRegion()).isEqualTo("서울특별시 강남구");
	}

	@Test
	@DisplayName("세종 기존 회원 동네는 level 1 세종 지역으로 백필한다")
	void backfillsSejongMemberLocationToRootRegion() {
		MemberLocation memberLocation = memberLocationRepository.saveAndFlush(memberLocation("세종"));
		entityManager.clear();

		memberLocationRegionBackfillSeeder.seed();
		memberLocationRepository.flush();
		entityManager.clear();

		MemberLocation foundLocation = memberLocationRepository.findById(memberLocation.getId()).orElseThrow();
		assertThat(foundLocation.getRegionRef()).isNotNull();
		assertThat(foundLocation.getRegionCode()).isEqualTo("3611000000");
		assertThat(foundLocation.getRegion()).isEqualTo("세종특별자치시");
	}

	@Test
	@DisplayName("백필할 수 없는 회원 동네가 있으면 실패 목록을 포함해 중단한다")
	void throwsWhenMemberLocationRegionCannotBeBackfilled() {
		MemberLocation memberLocation = memberLocationRepository.saveAndFlush(memberLocation("서울시 강남구"));
		entityManager.clear();

		assertThatThrownBy(() -> memberLocationRegionBackfillSeeder.seed())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(String.valueOf(memberLocation.getMember().getId()))
				.hasMessageContaining("서울시 강남구")
				.hasMessageContaining("Region.name 매칭 실패");
	}

	private MemberLocation memberLocation(String region) {
		Member member = memberRepository.save(Member.createUser(
				"member-location-backfill-" + region + "@example.com",
				"encodedPassword",
				"백필회원" + region
		));
		return MemberLocation.create(member, region, 0, true);
	}
}
