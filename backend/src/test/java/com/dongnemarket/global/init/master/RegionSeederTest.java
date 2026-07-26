package com.dongnemarket.global.init.master;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RegionSeederTest {

	// 세종특별자치시 법정동코드. 시(level1) 바로 아래에 동(level3)이 직속으로 붙는다.
	private static final String SEJONG_CODE = "3611000000";

	@Autowired
	RegionRepository regionRepository;

	@Autowired
	RegionSeeder regionSeeder;

	@Test
	@DisplayName("전국 지역 마스터가 계층별 정확한 개수로 저장된다")
	void savesFullHierarchy() {
		List<Region> all = regionRepository.findAll();
		Map<Integer, Long> byLevel = all.stream()
				.collect(Collectors.groupingBy(Region::getLevel, Collectors.counting()));

		assertThat(all).hasSize(5338);
		assertThat(byLevel.get(1)).isEqualTo(16);
		assertThat(byLevel.get(2)).isEqualTo(255);
		assertThat(byLevel.get(3)).isEqualTo(5067);
	}

	@Test
	@DisplayName("최상위(시도)만 parent가 없고, 나머지는 모두 유효한 parent를 가진다")
	void hasNoOrphans() {
		List<Region> all = regionRepository.findAll();

		long roots = all.stream().filter(r -> r.getParent() == null).count();
		assertThat(roots).isEqualTo(16);

		// 최상위가 아닌 모든 지역은 parent가 연결되어 있고, parent의 level이 더 작다.
		assertThat(all.stream()
				.filter(r -> r.getLevel() != 1)
				.allMatch(r -> r.getParent() != null && r.getParent().getLevel() < r.getLevel()))
				.isTrue();
	}

	@Test
	@DisplayName("세종은 시(level1) 바로 아래에 동(level3) 33개가 직속으로 붙는다")
	void sejongHasDirectDongChildren() {
		Region sejong = regionRepository.findAll().stream()
				.filter(r -> r.getCode().equals(SEJONG_CODE))
				.findFirst()
				.orElseThrow();
		assertThat(sejong.getLevel()).isEqualTo(1);

		List<Region> children = regionRepository.findByParentIdOrderByCodeAsc(sejong.getId());
		assertThat(children).hasSize(33);
		assertThat(children).allMatch(r -> r.getLevel() == 3);
	}

	@Test
	@DisplayName("시더를 다시 실행해도 지역이 중복 저장되지 않는다")
	void doesNotDuplicateOnReseed() {
		long beforeCount = regionRepository.count();

		regionSeeder.seed();

		assertThat(regionRepository.count()).isEqualTo(beforeCount);
	}
}
