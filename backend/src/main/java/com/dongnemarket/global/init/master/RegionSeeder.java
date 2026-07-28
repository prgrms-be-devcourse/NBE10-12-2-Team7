package com.dongnemarket.global.init.master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dongnemarket.global.init.DataSeeder;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 기준데이터: 계층형 전국 지역 마스터. 모든 환경에서 항상 실행(멱등). */
@Component
public class RegionSeeder implements DataSeeder {

	private static final String REGION_SEED_PATH = "seed/region_seed.csv";

	private final RegionRepository regionRepository;

	public RegionSeeder(RegionRepository regionRepository) {
		this.regionRepository = regionRepository;
	}

	@Override
	public int order() {
		return 11;
	}

	@Override
	@Transactional
	public void seed() {
		List<RegionSeedRow> rows = readRows();
		Map<String, Region> savedRegionsByCode = regionRepository.findAllByCodeIn(
						rows.stream()
								.map(RegionSeedRow::code)
								.toList()
				)
				.stream()
				.collect(Collectors.toMap(Region::getCode, Function.identity()));
		Set<String> existingCodes = new HashSet<>(savedRegionsByCode.keySet());

		for (int level = 1; level <= 3; level++) {
			List<Region> regions = new ArrayList<>();
			for (RegionSeedRow row : rowsByLevel(rows, level)) {
				if (existingCodes.contains(row.code())) {
					continue;
				}
				Region parent = null;
				if (row.parentCode() != null) {
					parent = savedRegionsByCode.get(row.parentCode());
				}
				regions.add(createRegion(row, parent));
			}
			List<Region> savedRegions = regionRepository.saveAll(regions);
			for (Region region : savedRegions) {
				savedRegionsByCode.put(region.getCode(), region);
				existingCodes.add(region.getCode());
			}
		}
	}

	private List<RegionSeedRow> rowsByLevel(List<RegionSeedRow> rows, int level) {
		return rows.stream()
				.filter(row -> row.level() == level)
				.sorted(Comparator.comparing(RegionSeedRow::code))
				.toList();
	}

	private Region createRegion(RegionSeedRow row, Region parent) {
		if (row.level() == 1) {
			return Region.root(row.code(), row.fullName(), row.displayName());
		}
		return Region.child(row.code(), row.level(), parent, row.fullName(), row.displayName());
	}

	private List<RegionSeedRow> readRows() {
		ClassPathResource resource = new ClassPathResource(REGION_SEED_PATH);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				resource.getInputStream(), StandardCharsets.UTF_8))) {
			return reader.lines()
					.skip(1)
					.filter(line -> !line.isBlank())
					.map(this::parseRow)
					.toList();
		} catch (IOException e) {
			throw new IllegalStateException("지역 시드 CSV를 읽을 수 없습니다: " + REGION_SEED_PATH, e);
		}
	}

	private RegionSeedRow parseRow(String line) {
		String[] values = line.split(",", -1);
		if (values.length != 5) {
			throw new IllegalStateException("지역 시드 CSV 형식이 올바르지 않습니다: " + line);
		}
		String parentCode = values[2].isBlank() ? null : values[2];
		return new RegionSeedRow(
				values[0],
				Integer.parseInt(values[1]),
				parentCode,
				values[3],
				values[4]
		);
	}

	private record RegionSeedRow(
			String code,
			int level,
			String parentCode,
			String fullName,
			String displayName
	) {
	}
}
