package com.dongnemarket.region.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegionControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RegionRepository regionRepository;

	// 공유 H2가 다른 @SpringBootTest 컨텍스트에 의해 재생성되어 시드가 소실될 수 있으므로,
	// 최상위 지역이 비어 있으면 최소 계층을 직접 시드한다(멱등).
	@BeforeEach
	void seedRegionsIfEmpty() {
		if (regionRepository.findByParentIsNullOrderByCodeAsc().isEmpty()) {
			Region seoul = regionRepository.save(new Region("1100000000", 1, null, "서울특별시", "서울특별시"));
			Region gangnam = regionRepository.save(new Region("1168000000", 2, seoul, "서울특별시 강남구", "강남구"));
			regionRepository.save(new Region("1168010100", 3, gangnam, "서울특별시 강남구 역삼동", "역삼동"));
		}
	}

	@Test
	@DisplayName("인증 없이 최상위(시도) 지역 목록을 조회할 수 있다")
	void getsRootRegionsWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/regions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data[0].level").value(1))
				.andExpect(jsonPath("$.data[0].regionId").isNumber())
				.andExpect(jsonPath("$.data[0].displayName").isNotEmpty());
	}

	@Test
	@DisplayName("parentId를 지정하면 해당 지역의 자식 목록을 조회한다")
	void getsChildRegionsByParentId() throws Exception {
		Region root = regionRepository.findByParentIsNullOrderByCodeAsc().get(0);
		Region firstChild = regionRepository.findByParentIdOrderByCodeAsc(root.getId()).get(0);

		mockMvc.perform(get("/api/regions").param("parentId", String.valueOf(root.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data[0].regionId").value(firstChild.getId()))
				.andExpect(jsonPath("$.data[0].displayName").value(firstChild.getDisplayName()));
	}
}
