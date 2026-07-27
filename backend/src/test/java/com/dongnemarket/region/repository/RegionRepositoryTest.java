package com.dongnemarket.region.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
	@DisplayName("최상위 지역을 표시명 오름차순으로 조회한다")
	void findsRootRegionsOrderByDisplayNameAsc() {
		Region seoul = regionRepository.save(Region.root("1100000000", "서울특별시", "서울특별시"));
		Region busan = regionRepository.save(Region.root("2600000000", "부산광역시", "부산광역시"));
		Region gyeonggi = regionRepository.save(Region.root("4100000000", "경기도", "경기도"));

		assertThat(regionRepository.findAllByParentIsNullOrderByDisplayNameAsc())
				.containsExactly(gyeonggi, busan, seoul);
	}

	@Test
	@DisplayName("부모 코드 기준으로 직접 하위 지역을 표시명 오름차순 조회한다")
	void findsChildrenByParentCodeOrderByDisplayNameAsc() {
		Region seoul = regionRepository.save(Region.root("1100000000", "서울특별시", "서울특별시"));
		Region gangnam = regionRepository.save(Region.child("1168000000", 2, seoul, "서울특별시 강남구", "강남구"));
		Region jongno = regionRepository.save(Region.child("1111000000", 2, seoul, "서울특별시 종로구", "종로구"));
		regionRepository.save(Region.root("2600000000", "부산광역시", "부산광역시"));

		assertThat(regionRepository.findAllByParentCodeOrderByDisplayNameAsc("1100000000"))
				.containsExactly(gangnam, jongno);
	}

	@Test
	@DisplayName("지역 코드를 기준으로 존재 여부를 확인한다")
	void checksRegionCodeExists() {
		regionRepository.save(Region.root("1100000000", "서울특별시", "서울특별시"));

		assertThat(regionRepository.existsByCode("1100000000")).isTrue();
		assertThat(regionRepository.existsByCode("9999999999")).isFalse();
	}
}
