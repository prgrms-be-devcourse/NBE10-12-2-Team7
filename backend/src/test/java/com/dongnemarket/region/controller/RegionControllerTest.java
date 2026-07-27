package com.dongnemarket.region.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongnemarket.global.init.master.RegionSeeder;
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
	RegionSeeder regionSeeder;

	@BeforeEach
	void setUp() {
		regionSeeder.seed();
	}

	@Test
	@DisplayName("인증 없이 최상위 지역 목록을 조회할 수 있다")
	void getsRootRegionsWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/regions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.length()").value(16))
				.andExpect(jsonPath("$.data[?(@.code == '1100000000')].displayName").value("서울특별시"));
	}

	@Test
	@DisplayName("인증 없이 부모 지역의 하위 지역 목록을 조회할 수 있다")
	void getsChildRegionsWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/regions")
						.param("parentCode", "1100000000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data[0].level").value(2))
				.andExpect(jsonPath("$.data[?(@.code == '1168000000')].displayName").value("강남구"));
	}

	@Test
	@DisplayName("세종은 시도 바로 아래 읍면동을 하위 지역으로 반환한다")
	void getsSejongDirectDongChildren() throws Exception {
		mockMvc.perform(get("/api/regions")
						.param("parentCode", "3611000000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data[0].level").value(3));
	}
}
