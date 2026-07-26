package com.dongnemarket.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.config.JpaAuditingConfig;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.ProductImage;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ProductImageRepositoryTest {

	@Autowired
	ProductImageRepository productImageRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	RegionRepository regionRepository;

	@Test
	@DisplayName("상품 이미지는 정렬 순서 오름차순으로 조회한다")
	void findsProductImagesByProductIdOrderBySortOrderAsc() {
		Product product = saveProduct("image-order@example.com");
		ProductImage secondImage = productImageRepository.save(ProductImage.create(product, "https://example.com/2.jpg", 1, false));
		ProductImage firstImage = productImageRepository.save(ProductImage.create(product, "https://example.com/1.jpg", 0, true));
		ProductImage thirdImage = productImageRepository.save(ProductImage.create(product, "https://example.com/3.jpg", 2, false));

		List<ProductImage> images = productImageRepository.findAllByProductIdOrderBySortOrderAsc(product.getId());

		assertThat(images).containsExactly(firstImage, secondImage, thirdImage);
	}

	@Test
	@DisplayName("상품 ID 기준으로 기존 이미지를 모두 삭제한다")
	void deletesProductImagesByProductId() {
		Product targetProduct = saveProduct("image-delete-target@example.com");
		Product otherProduct = saveProduct("image-delete-other@example.com");
		productImageRepository.save(ProductImage.create(targetProduct, "https://example.com/target-1.jpg", 0, true));
		productImageRepository.save(ProductImage.create(targetProduct, "https://example.com/target-2.jpg", 1, false));
		productImageRepository.save(ProductImage.create(otherProduct, "https://example.com/other-1.jpg", 0, true));

		productImageRepository.deleteAllByProductId(targetProduct.getId());

		assertThat(productImageRepository.findAllByProductIdOrderBySortOrderAsc(targetProduct.getId())).isEmpty();
		List<ProductImage> otherImages = productImageRepository.findAllByProductIdOrderBySortOrderAsc(otherProduct.getId());
		assertThat(otherImages).hasSize(1);
		assertThat(otherImages.get(0).getImageUrl()).isEqualTo("https://example.com/other-1.jpg");
		assertThat(otherImages.get(0).isRepresentative()).isTrue();
	}

	private Product saveProduct(String email) {
		Member member = memberRepository.save(Member.createUser(email, "encodedPassword", "판매자-" + Math.abs(email.hashCode())));
		Category category = categoryRepository.save(new Category("이미지-" + Math.abs(email.hashCode())));
		Region region = regionRepository.save(new Region(String.format("%010d", Math.abs(email.hashCode())), 3, null, "서울특별시 강남구 역삼동", "역삼동"));
		return productRepository.saveAndFlush(Product.create(
				member,
				category,
				"이미지 테스트 상품",
				"이미지 테스트 상품 설명입니다.",
				BigDecimal.valueOf(10000),
				region
		));
	}
}
