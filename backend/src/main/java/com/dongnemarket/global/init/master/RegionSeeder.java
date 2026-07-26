package com.dongnemarket.global.init.master;

import com.dongnemarket.global.init.DataSeeder;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 기준데이터: 전국 법정동 지역 마스터(시–구–동 계층). 모든 환경에서 항상 실행(멱등).
 *
 * <p>region_seed.csv(code 오름차순)를 1-pass로 주입한다. 코드 자릿수 구조상 부모가 항상
 * 자식보다 먼저 나오므로, code→저장된 id 맵을 유지하며 parent_code로 부모를 연결한다.
 */
@Component
public class RegionSeeder implements DataSeeder {

	private static final String SEED_FILE = "region_seed.csv";

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
		// 멱등: 이미 적재돼 있으면 스킵.
		if (regionRepository.count() > 0) {
			return;
		}

		Map<String, Region> byCode = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ClassPathResource(SEED_FILE).getInputStream(), StandardCharsets.UTF_8))) {
			reader.readLine(); // 헤더 스킵
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				// code,level,parent_code,full_name,display_name
				String[] cols = line.split(",", -1);
				String code = cols[0];
				int level = Integer.parseInt(cols[1]);
				String parentCode = cols[2];
				String fullName = cols[3];
				String displayName = cols[4];

				Region parent = parentCode.isEmpty() ? null : byCode.get(parentCode);
				Region saved = regionRepository.save(new Region(code, level, parent, fullName, displayName));
				byCode.put(code, saved);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("지역 시드 파일을 읽을 수 없습니다: " + SEED_FILE, e);
		}
	}
}
