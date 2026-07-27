package com.dongnemarket.global.init.master;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class RegionSeederTest {

	private static final List<String> REPRESENTATIVE_REGION_NAMES = List.of(
			"서울특별시",
			"부산광역시",
			"세종특별자치시",
			"서울특별시 강남구",
			"부산광역시 해운대구",
			"제주특별자치도 제주시",
			"서울특별시 강남구 역삼동"
	);

	@Autowired
	RegionRepository regionRepository;

	@Autowired
	RegionSeeder regionSeeder;

	@Test
	@DisplayName("애플리케이션 시작 시 계층형 전국 지역 마스터가 저장된다")
	void savesDefaultRegions() {
		List<String> regionNames = regionRepository.findAll()
				.stream()
				.map(Region::getFullName)
				.toList();

		assertThat(regionNames).isNotEmpty();
		assertThat(regionNames).containsAll(REPRESENTATIVE_REGION_NAMES);
		assertThat(regionRepository.countByLevel(1)).isEqualTo(16);
		assertThat(regionRepository.countByLevel(2)).isEqualTo(255);
		assertThat(regionRepository.countByLevel(3)).isEqualTo(5067);
	}

	@Test
	@DisplayName("시더를 다시 실행해도 지역이 중복 저장되지 않는다")
	void doesNotDuplicateRegions() {
		long beforeCount = regionRepository.count();

		regionSeeder.seed();

		assertThat(regionRepository.count()).isEqualTo(beforeCount);
	}

	@Test
	@Transactional
	@DisplayName("일부 지역만 저장되어 있으면 누락된 지역만 보충한다")
	void fillsOnlyMissingRegions() throws Exception {
		regionRepository.deleteByLevel(3);
		regionRepository.deleteByLevel(2);
		regionRepository.deleteByLevel(1);
		regionRepository.save(Region.root("1100000000", "서울특별시", "서울특별시"));

		regionSeeder.seed();

		List<String> regionNames = regionRepository.findAll()
				.stream()
				.map(Region::getFullName)
				.toList();
		assertThat(regionNames).containsAll(REPRESENTATIVE_REGION_NAMES);
		assertThat(regionNames)
				.filteredOn(regionName -> regionName.equals("서울특별시"))
				.hasSize(1);
		assertThat(regionNames)
				.filteredOn(regionName -> regionName.equals("서울특별시 강남구"))
				.hasSize(1);
	}

	@Test
	@DisplayName("세종은 시도 바로 아래에 읍면동이 연결된다")
	void savesSejongDongChildrenUnderRoot() {
		List<Region> children = regionRepository.findAllByParentCodeOrderByDisplayNameAsc("3611000000");

		assertThat(children).isNotEmpty();
		assertThat(children).allMatch(region -> region.getLevel() == 3);
	}
}
