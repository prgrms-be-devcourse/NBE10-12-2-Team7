package com.dongnemarket.region.init;

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
class RegionInitializerTest {

	private static final List<String> SEOUL_REGION_NAMES = List.of(
			"서울 강남구",
			"서울 강동구",
			"서울 강북구",
			"서울 강서구",
			"서울 관악구",
			"서울 광진구",
			"서울 구로구",
			"서울 금천구",
			"서울 노원구",
			"서울 도봉구",
			"서울 동대문구",
			"서울 동작구",
			"서울 마포구",
			"서울 서대문구",
			"서울 서초구",
			"서울 성동구",
			"서울 성북구",
			"서울 송파구",
			"서울 양천구",
			"서울 영등포구",
			"서울 용산구",
			"서울 은평구",
			"서울 종로구",
			"서울 중구",
			"서울 중랑구"
	);

	@Autowired
	RegionRepository regionRepository;

	@Autowired
	RegionInitializer regionInitializer;

	@Test
	@DisplayName("애플리케이션 시작 시 서울 25개 구가 저장된다")
	void savesTwentyFiveSeoulRegions() {
		List<String> regionNames = regionRepository.findAllByOrderByNameAsc()
				.stream()
				.map(Region::getName)
				.toList();

		assertThat(regionNames).containsExactlyInAnyOrderElementsOf(SEOUL_REGION_NAMES);
	}

	@Test
	@DisplayName("초기화기를 다시 실행해도 지역이 중복 저장되지 않는다")
	void doesNotDuplicateRegions() {
		regionInitializer.run(null);

		assertThat(regionRepository.count()).isEqualTo(SEOUL_REGION_NAMES.size());
	}

	@Test
	@DisplayName("일부 지역만 저장되어 있으면 누락된 지역만 보충한다")
	void fillsOnlyMissingRegions() throws Exception {
		regionRepository.deleteAll();
		regionRepository.save(new Region("서울 강남구"));
		regionRepository.save(new Region("서울 마포구"));

		regionInitializer.run(null);

		List<String> regionNames = regionRepository.findAllByOrderByNameAsc()
				.stream()
				.map(Region::getName)
				.toList();
		assertThat(regionNames).containsExactlyInAnyOrderElementsOf(SEOUL_REGION_NAMES);
		assertThat(regionRepository.count()).isEqualTo(SEOUL_REGION_NAMES.size());
	}
}
