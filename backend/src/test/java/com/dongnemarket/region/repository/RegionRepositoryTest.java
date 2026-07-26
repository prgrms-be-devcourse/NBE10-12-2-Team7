package com.dongnemarket.region.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.dongnemarket.region.entity.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RegionRepositoryTest {

	@Autowired
	RegionRepository regionRepository;

	@Test
	@DisplayName("최상위(parent 없음) 지역을 code 오름차순으로 조회한다")
	void findsRootRegionsOrderByCode() {
		Region busan = regionRepository.save(new Region("2600000000", 1, null, "부산광역시", "부산광역시"));
		Region seoul = regionRepository.save(new Region("1100000000", 1, null, "서울특별시", "서울특별시"));
		regionRepository.save(new Region("1168000000", 2, seoul, "서울특별시 강남구", "강남구"));

		List<Region> roots = regionRepository.findByParentIsNullOrderByCodeAsc();

		assertThat(roots).containsExactly(seoul, busan);
	}

	@Test
	@DisplayName("특정 지역의 자식 목록을 code 오름차순으로 조회한다")
	void findsChildrenByParentIdOrderByCode() {
		Region seoul = regionRepository.save(new Region("1100000000", 1, null, "서울특별시", "서울특별시"));
		Region mapo = regionRepository.save(new Region("1144000000", 2, seoul, "서울특별시 마포구", "마포구"));
		Region gangnam = regionRepository.save(new Region("1168000000", 2, seoul, "서울특별시 강남구", "강남구"));
		Region gangnamDong = regionRepository.save(new Region("1168010100", 3, gangnam, "서울특별시 강남구 역삼동", "역삼동"));

		List<Region> seoulChildren = regionRepository.findByParentIdOrderByCodeAsc(seoul.getId());
		List<Region> gangnamChildren = regionRepository.findByParentIdOrderByCodeAsc(gangnam.getId());

		assertThat(seoulChildren).containsExactly(mapo, gangnam);
		assertThat(gangnamChildren).containsExactly(gangnamDong);
	}

	@Test
	@DisplayName("특정 level의 첫 지역을 code 오름차순으로 조회한다")
	void findsFirstRegionByLevel() {
		Region seoul = regionRepository.save(new Region("1100000000", 1, null, "서울특별시", "서울특별시"));
		Region gangnam = regionRepository.save(new Region("1168000000", 2, seoul, "서울특별시 강남구", "강남구"));
		Region gangnamDong = regionRepository.save(new Region("1168010100", 3, gangnam, "서울특별시 강남구 역삼동", "역삼동"));

		assertThat(regionRepository.findFirstByLevelOrderByCodeAsc(3)).contains(gangnamDong);
		assertThat(regionRepository.findFirstByLevelOrderByCodeAsc(1)).contains(seoul);
	}
}
