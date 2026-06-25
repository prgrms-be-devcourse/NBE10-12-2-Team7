package com.dongnemarket.category.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("카테고리 목록을 인증 없이 조회할 수 있다")
	void getsCategoriesWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(8))
				.andExpect(jsonPath("$.data[0].id").isNumber())
				.andExpect(jsonPath("$.data[0].name").value("디지털기기"))
				.andExpect(jsonPath("$.data[7].name").value("기타"));
	}
}
