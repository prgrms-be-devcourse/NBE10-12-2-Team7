package com.dongnemarket.product.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductImageRepository;
import com.dongnemarket.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductRegionBackfillSeederTest {

	@Autowired
	ProductRegionBackfillSeeder productRegionBackfillSeeder;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	ProductImageRepository productImageRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	EntityManager entityManager;

	@AfterEach
	void cleanUp() {
		productImageRepository.deleteAll();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("region_id가 없는 기존 상품은 문자열 지역을 Region.name으로 매칭해 백필한다")
	void backfillsProductRegionByLegacyRegionName() {
		Product product = productRepository.saveAndFlush(product("서울 강남구"));
		entityManager.clear();

		productRegionBackfillSeeder.seed();
		productRepository.flush();
		entityManager.clear();

		Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
		assertThat(foundProduct.getRegionRef()).isNotNull();
		assertThat(foundProduct.getRegionCode()).isEqualTo("1168000000");
		assertThat(foundProduct.getRegion()).isEqualTo("서울특별시 강남구");
	}

	@Test
	@DisplayName("세종 기존 상품은 level 1 세종 지역으로 백필한다")
	void backfillsSejongProductToRootRegion() {
		Product product = productRepository.saveAndFlush(product("세종"));
		entityManager.clear();

		productRegionBackfillSeeder.seed();
		productRepository.flush();
		entityManager.clear();

		Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
		assertThat(foundProduct.getRegionRef()).isNotNull();
		assertThat(foundProduct.getRegionCode()).isEqualTo("3611000000");
		assertThat(foundProduct.getRegion()).isEqualTo("세종특별자치시");
	}

	@Test
	@DisplayName("백필할 수 없는 상품이 있으면 실패 목록을 포함해 중단한다")
	void throwsWhenProductRegionCannotBeBackfilled() {
		Product product = productRepository.saveAndFlush(product("서울시 강남구"));
		entityManager.clear();

		assertThatThrownBy(() -> productRegionBackfillSeeder.seed())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(String.valueOf(product.getId()))
				.hasMessageContaining("서울시 강남구")
				.hasMessageContaining("Region.name 매칭 실패");
	}

	private Product product(String region) {
		Member member = memberRepository.save(Member.createUser("backfill-" + region + "@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("백필-" + region));
		return Product.create(
				member,
				category,
				"백필 상품",
				"백필 상품 설명",
				BigDecimal.valueOf(10000),
				region
		);
	}
}
