package com.dongnemarket.region.init;

import java.util.List;

import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegionInitializer implements ApplicationRunner {

	private static final List<String> DEFAULT_REGION_NAMES = List.of(
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

	private final RegionRepository regionRepository;

	public RegionInitializer(RegionRepository regionRepository) {
		this.regionRepository = regionRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		DEFAULT_REGION_NAMES.stream()
				.filter(regionName -> !regionRepository.existsByName(regionName))
				.map(Region::new)
				.forEach(regionRepository::save);
	}
}
