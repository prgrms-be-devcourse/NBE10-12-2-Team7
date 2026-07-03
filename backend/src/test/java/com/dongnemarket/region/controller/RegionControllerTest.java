package com.dongnemarket.region.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
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

	@Test
	@DisplayName("인증 없이 지역 목록을 조회할 수 있다")
	void getsRegionsWithoutAuthentication() throws Exception {
		if (!regionRepository.existsByName("서울 강남구")) {
			regionRepository.save(new Region("서울 강남구"));
		}

		mockMvc.perform(get("/api/regions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data[0].name").value("서울 강남구"));
	}
}
