package com.dongnemarket.product.controller;

import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductImageControllerTest {

	@TempDir
	static Path productImageTempDir;

	@DynamicPropertySource
	static void productImageProperties(DynamicPropertyRegistry registry) {
		registry.add("file.storage.local.base-path", () -> productImageTempDir.toString());
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	MemberRepository memberRepository;

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증된 사용자는 상품 이미지 1장을 업로드할 수 있다")
	void uploadsOneProductImageWithAuthentication() throws Exception {
		String token = accessToken();
		MockMultipartFile file = imageFile("product.jpg", "image/jpeg", "product image");

		mockMvc.perform(multipart("/api/products/images")
						.file(file)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(201))
				.andExpect(jsonPath("$.message").value("상품 이미지가 업로드되었습니다."))
				.andExpect(jsonPath("$.data.imageUrls.length()").value(1))
				.andExpect(jsonPath("$.data.imageUrls[0]").value(org.hamcrest.Matchers.startsWith("/api/products/images/")));
	}

	@Test
	@DisplayName("인증된 사용자는 상품 이미지 5장을 업로드할 수 있다")
	void uploadsFiveProductImagesWithAuthentication() throws Exception {
		String token = accessToken();

		mockMvc.perform(multipart("/api/products/images")
						.file(imageFile("1.jpg", "image/jpeg", "1"))
						.file(imageFile("2.jpg", "image/jpeg", "2"))
						.file(imageFile("3.jpg", "image/jpeg", "3"))
						.file(imageFile("4.jpg", "image/jpeg", "4"))
						.file(imageFile("5.jpg", "image/jpeg", "5"))
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.imageUrls.length()").value(5));
	}

	@Test
	@DisplayName("상품 이미지를 6장 업로드하면 INVALID_INPUT_VALUE를 반환한다")
	void returnsInvalidInputWhenUploadingMoreThanFiveImages() throws Exception {
		String token = accessToken();

		mockMvc.perform(multipart("/api/products/images")
						.file(imageFile("1.jpg", "image/jpeg", "1"))
						.file(imageFile("2.jpg", "image/jpeg", "2"))
						.file(imageFile("3.jpg", "image/jpeg", "3"))
						.file(imageFile("4.jpg", "image/jpeg", "4"))
						.file(imageFile("5.jpg", "image/jpeg", "5"))
						.file(imageFile("6.jpg", "image/jpeg", "6"))
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("이미지가 아닌 파일을 업로드하면 INVALID_INPUT_VALUE를 반환한다")
	void returnsInvalidInputWhenUploadingNonImageFile() throws Exception {
		String token = accessToken();
		MockMultipartFile file = new MockMultipartFile("files", "memo.txt", "text/plain", "memo".getBytes());

		mockMvc.perform(multipart("/api/products/images")
						.file(file)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("인증 없이 상품 이미지를 업로드하면 401을 반환한다")
	void returnsUnauthorizedWhenUploadingWithoutAuthentication() throws Exception {
		MockMultipartFile file = imageFile("product.jpg", "image/jpeg", "product image");

		mockMvc.perform(multipart("/api/products/images").file(file))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("업로드 응답 URL로 상품 이미지를 조회할 수 있다")
	void getsUploadedProductImageByUrl() throws Exception {
		String token = accessToken();
		MockMultipartFile file = imageFile("product.png", "image/png", "product image");
		MvcResult uploadResult = mockMvc.perform(multipart("/api/products/images")
						.file(file)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isCreated())
				.andReturn();
		String content = uploadResult.getResponse().getContentAsString();
		String imageUrl = content.substring(content.indexOf("/api/products/images/"), content.indexOf("\"", content.indexOf("/api/products/images/")));
		String filename = imageUrl.substring("/api/products/images/".length());

		MvcResult imageResult = mockMvc.perform(get("/api/products/images/{filename}", filename)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andReturn();

		assertThat(imageResult.getResponse().getContentAsByteArray()).isEqualTo("product image".getBytes());
	}

	@Test
	@DisplayName("존재하지 않는 상품 이미지를 조회하면 PRODUCT_NOT_FOUND를 반환한다")
	void returnsProductNotFoundWhenProductImageDoesNotExist() throws Exception {
		String token = accessToken();

		mockMvc.perform(get("/api/products/images/{filename}", "missing.png")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
	}

	private String accessToken() {
		Member member = memberRepository.save(Member.createUser("product-image-uploader@example.com", "encodedPassword", "이미지업로더"));
		return jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
	}

	private MockMultipartFile imageFile(String filename, String contentType, String content) {
		return new MockMultipartFile("files", filename, contentType, content.getBytes());
	}
}
