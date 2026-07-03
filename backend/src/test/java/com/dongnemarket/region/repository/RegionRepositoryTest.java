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
	@DisplayName("지역 목록을 이름 오름차순으로 조회한다")
	void findsRegionsOrderByNameAsc() {
		Region songpa = regionRepository.save(new Region("서울 송파구"));
		Region gangnam = regionRepository.save(new Region("서울 강남구"));
		Region mapo = regionRepository.save(new Region("서울 마포구"));

		assertThat(regionRepository.findAllByOrderByNameAsc())
				.containsExactly(gangnam, mapo, songpa);
	}

	@Test
	@DisplayName("지역 이름 존재 여부를 확인한다")
	void checksRegionNameExists() {
		regionRepository.save(new Region("서울 강남구"));

		assertThat(regionRepository.existsByName("서울 강남구")).isTrue();
		assertThat(regionRepository.existsByName("서울시 강남구")).isFalse();
	}
}
