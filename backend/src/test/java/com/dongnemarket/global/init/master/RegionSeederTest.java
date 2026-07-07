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

@SpringBootTest
@ActiveProfiles("test")
class RegionSeederTest {

	private static final List<String> REPRESENTATIVE_REGION_NAMES = List.of(
			"서울 강남구",
			"부산 해운대구",
			"대구 군위군",
			"인천 강화군",
			"광주 광산구",
			"대전 유성구",
			"울산 울주군",
			"세종",
			"경기 성남시",
			"강원 춘천시",
			"충북 청주시",
			"충남 천안시",
			"전북 전주시",
			"전남 여수시",
			"경북 포항시",
			"경남 창원시",
			"제주 제주시"
	);

	@Autowired
	RegionRepository regionRepository;

	@Autowired
	RegionSeeder regionSeeder;

	@Test
	@DisplayName("애플리케이션 시작 시 전국 지역 마스터가 저장된다")
	void savesDefaultRegions() {
		List<String> regionNames = regionRepository.findAllByOrderByNameAsc()
				.stream()
				.map(Region::getName)
				.toList();

		assertThat(regionNames).isNotEmpty();
		assertThat(regionNames).containsAll(REPRESENTATIVE_REGION_NAMES);
	}

	@Test
	@DisplayName("시더를 다시 실행해도 지역이 중복 저장되지 않는다")
	void doesNotDuplicateRegions() {
		long beforeCount = regionRepository.count();

		regionSeeder.seed();

		assertThat(regionRepository.count()).isEqualTo(beforeCount);
	}

	@Test
	@DisplayName("일부 지역만 저장되어 있으면 누락된 지역만 보충한다")
	void fillsOnlyMissingRegions() throws Exception {
		regionRepository.deleteAll();
		regionRepository.save(new Region("서울 강남구"));
		regionRepository.save(new Region("서울 마포구"));

		regionSeeder.seed();

		List<String> regionNames = regionRepository.findAllByOrderByNameAsc()
				.stream()
				.map(Region::getName)
				.toList();
		assertThat(regionNames).containsAll(REPRESENTATIVE_REGION_NAMES);
		assertThat(regionNames)
				.filteredOn(regionName -> regionName.equals("서울 강남구"))
				.hasSize(1);
		assertThat(regionNames)
				.filteredOn(regionName -> regionName.equals("서울 마포구"))
				.hasSize(1);
	}
}
