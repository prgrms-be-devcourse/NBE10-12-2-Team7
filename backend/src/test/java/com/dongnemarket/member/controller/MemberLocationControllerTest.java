package com.dongnemarket.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongnemarket.auth.entity.EmailVerification;
import com.dongnemarket.auth.repository.EmailVerificationRepository;
import com.dongnemarket.member.repository.MemberAgreementRepository;
import com.dongnemarket.member.repository.MemberLocationRepository;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberLocationControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	EmailVerificationRepository emailVerificationRepository;

	@Autowired
	MemberLocationRepository memberLocationRepository;

	@Autowired
	RegionRepository regionRepository;

	@Autowired
	MemberAgreementRepository memberAgreementRepository;

	@AfterEach
	void cleanUp() {
		memberLocationRepository.deleteAll();
		memberAgreementRepository.deleteAll();
		memberRepository.deleteAll();
		emailVerificationRepository.deleteAll();
	}

	/** 회원가입은 이메일 인증 완료를 전제로 하므로, signup을 호출하기 전에 인증 완료 상태를 만들어둔다. */
	private void verifyEmail(String email) {
		emailVerificationRepository.save(EmailVerification.verified(email, LocalDateTime.now()));
	}

	@Test
	@DisplayName("유효한 토큰으로 동네 2개를 설정하면 200과 저장된 동네 목록을 반환한다")
	void updatesMyLocations() throws Exception {
		saveRegionIfAbsent("서울 강남구");
		saveRegionIfAbsent("서울 마포구");
		String token = getAccessToken("locations-put@example.com", "password123!", "locPutUser");

		mockMvc.perform(put("/api/members/me/locations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"regions\":[\"서울 강남구\",\"서울 마포구\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data[0].region").value("서울 강남구"))
				.andExpect(jsonPath("$.data[0].sortOrder").value(0))
				.andExpect(jsonPath("$.data[0].active").value(true))
				.andExpect(jsonPath("$.data[1].region").value("서울 마포구"))
				.andExpect(jsonPath("$.data[1].sortOrder").value(1))
				.andExpect(jsonPath("$.data[1].active").value(false));
	}

	@Test
	@DisplayName("설정 후 조회하면 정렬 순서대로 동네 목록을 반환한다")
	void getsMyLocations() throws Exception {
		saveRegionIfAbsent("서울 강남구");
		saveRegionIfAbsent("서울 마포구");
		String token = getAccessToken("locations-get@example.com", "password123!", "locGetUser");
		mockMvc.perform(put("/api/members/me/locations")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"regions\":[\"서울 강남구\",\"서울 마포구\"]}"));

		mockMvc.perform(get("/api/members/me/locations")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data[0].region").value("서울 강남구"))
				.andExpect(jsonPath("$.data[0].active").value(true))
				.andExpect(jsonPath("$.data[1].region").value("서울 마포구"))
				.andExpect(jsonPath("$.data[1].active").value(false));
	}

	@Test
	@DisplayName("설정한 동네가 없으면 빈 목록을 반환한다")
	void getsEmptyMyLocations() throws Exception {
		String token = getAccessToken("locations-empty@example.com", "password123!", "locEmptyUser");

		mockMvc.perform(get("/api/members/me/locations")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	@DisplayName("토큰 없이 동네 설정을 요청하면 401을 반환한다")
	void updateMyLocationsWithoutTokenReturns401() throws Exception {
		mockMvc.perform(put("/api/members/me/locations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"regions\":[\"서울 강남구\"]}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("토큰 없이 동네 조회를 요청하면 401을 반환한다")
	void getMyLocationsWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/members/me/locations"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("동네 목록이 빈 리스트이면 400과 INVALID_INPUT_VALUE를 반환한다")
	void rejectsEmptyRegions() throws Exception {
		String token = getAccessToken("locations-empty-list@example.com", "password123!", "locEmptyListUser");

		mockMvc.perform(put("/api/members/me/locations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"regions\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("동네 목록이 null이면 400과 INVALID_INPUT_VALUE를 반환한다")
	void rejectsNullRegions() throws Exception {
		String token = getAccessToken("locations-null@example.com", "password123!", "locNullUser");

		mockMvc.perform(put("/api/members/me/locations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"regions\":null}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("동네가 3개이면 400과 INVALID_INPUT_VALUE를 반환한다")
	void rejectsThreeRegions() throws Exception {
		String token = getAccessToken("locations-three@example.com", "password123!", "locThreeUser");

		mockMvc.perform(put("/api/members/me/locations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"regions\":[\"서울 강남구\",\"서울 마포구\",\"서울 송파구\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("동네 원소가 공백이면 400과 INVALID_INPUT_VALUE를 반환한다")
	void rejectsBlankRegion() throws Exception {
		String token = getAccessToken("locations-blank@example.com", "password123!", "locBlankUser");

		mockMvc.perform(put("/api/members/me/locations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"regions\":[\" \"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("리스트 안에 중복 지역이 있으면 400과 INVALID_INPUT_VALUE를 반환한다")
	void rejectsDuplicateRegions() throws Exception {
		saveRegionIfAbsent("서울 강남구");
		String token = getAccessToken("locations-duplicate@example.com", "password123!", "locDuplicateUser");

		mockMvc.perform(put("/api/members/me/locations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"regions\":[\"서울 강남구\",\"서울 강남구\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("지역 마스터에 없는 지역이면 400과 INVALID_INPUT_VALUE를 반환한다")
	void rejectsUnknownRegion() throws Exception {
		String token = getAccessToken("locations-unknown@example.com", "password123!", "locUnknownUser");

		mockMvc.perform(put("/api/members/me/locations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"regions\":[\"강남\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	private String getAccessToken(String email, String password, String nickname) throws Exception {
		verifyEmail(email);
		String signup = String.format(
				"{\"email\":\"%s\",\"password\":\"%s\",\"nickname\":\"%s\",\"termsAgreed\":true,\"personalInfoCollectionAgreed\":true}",
				email, password, nickname);
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signup));

		String login = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(login))
				.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	private void saveRegionIfAbsent(String name) {
		if (!regionRepository.existsByName(name)) {
			regionRepository.save(new Region(name));
		}
	}
}
