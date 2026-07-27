package com.dongnemarket.member.init;

import java.util.ArrayList;
import java.util.List;

import com.dongnemarket.global.init.DataSeeder;
import com.dongnemarket.member.entity.MemberLocation;
import com.dongnemarket.member.repository.MemberLocationRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 기존 문자열 회원 동네를 계층형 Region FK로 연결하는 일회성/멱등 백필 시더. */
@Component
public class MemberLocationRegionBackfillSeeder implements DataSeeder {

	private final MemberLocationRepository memberLocationRepository;
	private final RegionRepository regionRepository;

	public MemberLocationRegionBackfillSeeder(MemberLocationRepository memberLocationRepository,
											  RegionRepository regionRepository) {
		this.memberLocationRepository = memberLocationRepository;
		this.regionRepository = regionRepository;
	}

	@Override
	public int order() {
		return 13;
	}

	@Override
	@Transactional
	public void seed() {
		List<MemberLocation> memberLocations = memberLocationRepository.findAllByRegionRefIsNull();
		List<BackfillFailure> failures = new ArrayList<>();
		for (MemberLocation memberLocation : memberLocations) {
			Region region = regionRepository.findByName(memberLocation.getRegion()).orElse(null);
			if (region == null) {
				failures.add(new BackfillFailure(
						memberLocation.getMember().getId(),
						memberLocation.getRegion(),
						"Region.name 매칭 실패"
				));
				continue;
			}
			memberLocation.backfillRegion(region);
		}

		if (!failures.isEmpty()) {
			throw new IllegalStateException("회원 동네 지역 백필 실패: " + failures);
		}
	}

	private record BackfillFailure(Long memberId, String region, String reason) {
	}
}
