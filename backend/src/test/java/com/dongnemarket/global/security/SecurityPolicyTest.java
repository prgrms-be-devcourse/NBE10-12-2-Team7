package com.dongnemarket.global.security;

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

/**
 * 공통 보안 정책 스모크 테스트.
 * 인증 없는 보호 API 가 공통 {@link com.dongnemarket.global.response.ErrorResponse} 포맷의 401 을 주는지,
 * 공개 경로(Swagger)가 인증 없이 열려 있는지 검증한다. (H2, MySQL 불필요)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityPolicyTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("인증 없이 보호 API 접근 시 401 + 공통 ErrorResponse(JSON)")
	void protectedApi_withoutToken_returns401ErrorResponse() throws Exception {
		mockMvc.perform(get("/api/admin/members"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	@DisplayName("Swagger api-docs 는 인증 없이 접근 가능(200)")
	void swaggerApiDocs_isPublic() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk());
	}
}
