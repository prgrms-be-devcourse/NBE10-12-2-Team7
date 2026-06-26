package com.dongnemarket.product.repository;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.config.JpaAuditingConfig;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ProductRepositoryTest {

	@Autowired
	ProductRepository productRepository;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Test
	@DisplayName("상품을 저장하고 기본 필드와 시간 필드를 조회할 수 있다")
	void savesProductWithBaseFields() {
		Member member = memberRepository.save(Member.createUser("seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("디지털기기"));

		Product product = Product.create(
				member,
				category,
				"아이폰 15",
				"상태 좋은 아이폰입니다.",
				800000,
				"서울 강남구"
		);

		Product savedProduct = productRepository.saveAndFlush(product);

		assertThat(savedProduct.getId()).isNotNull();
		assertThat(savedProduct.getMember().getId()).isEqualTo(member.getId());
		assertThat(savedProduct.getCategory().getId()).isEqualTo(category.getId());
		assertThat(savedProduct.getTitle()).isEqualTo("아이폰 15");
		assertThat(savedProduct.getDescription()).isEqualTo("상태 좋은 아이폰입니다.");
		assertThat(savedProduct.getPrice()).isEqualTo(800000);
		assertThat(savedProduct.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
		assertThat(savedProduct.getRegion()).isEqualTo("서울 강남구");
		assertThat(savedProduct.getViewCount()).isZero();
		assertThat(savedProduct.isHidden()).isFalse();
		assertThat(savedProduct.getDeletedAt()).isNull();
		assertThat(savedProduct.getCreatedAt()).isNotNull();
		assertThat(savedProduct.getUpdatedAt()).isNotNull();
	}
}
