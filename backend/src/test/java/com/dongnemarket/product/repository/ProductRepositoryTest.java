package com.dongnemarket.product.repository;

import java.math.BigDecimal;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.config.JpaAuditingConfig;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.spec.ProductSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

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

	@Autowired
	EntityManager entityManager;

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
				BigDecimal.valueOf(800000),
				"서울 강남구"
		);

		Product savedProduct = productRepository.saveAndFlush(product);

		assertThat(savedProduct.getId()).isNotNull();
		assertThat(savedProduct.getMember().getId()).isEqualTo(member.getId());
		assertThat(savedProduct.getCategory().getId()).isEqualTo(category.getId());
		assertThat(savedProduct.getTitle()).isEqualTo("아이폰 15");
		assertThat(savedProduct.getDescription()).isEqualTo("상태 좋은 아이폰입니다.");
		assertThat(savedProduct.getPrice()).isEqualByComparingTo("800000");
		assertThat(savedProduct.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
		assertThat(savedProduct.getRegion()).isEqualTo("서울 강남구");
		assertThat(savedProduct.getViewCount()).isZero();
		assertThat(savedProduct.isHidden()).isFalse();
		assertThat(savedProduct.getDeletedAt()).isNull();
		assertThat(savedProduct.getCreatedAt()).isNotNull();
		assertThat(savedProduct.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("삭제되지 않고 숨김 처리되지 않은 상품이면 접근 가능한 상품으로 판단한다")
	void returnsTrueWhenProductIsAccessible() {
		Member member = memberRepository.save(Member.createUser("seller-accessible@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("생활가전"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"공기청정기",
				"상태 좋은 공기청정기입니다.",
				BigDecimal.valueOf(120000),
				"서울 강남구"
		));

		boolean exists = productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(product.getId());

		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("상품 저장 시 favoriteCount 기본값은 0이다")
	void savesProductWithDefaultFavoriteCount() {
		Member member = memberRepository.save(Member.createUser("favorite-default@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("디지털기기"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"아이패드",
				"깨끗한 아이패드입니다.",
				BigDecimal.valueOf(500000),
				"서울 강남구"
		));

		assertThat(product.getFavoriteCount()).isZero();
	}

	@Test
	@DisplayName("favoriteCount는 원자 UPDATE로 1 증가한다")
	void incrementsFavoriteCount() {
		Member member = memberRepository.save(Member.createUser("favorite-increment@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("생활가전"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"청소기",
				"상태 좋은 청소기입니다.",
				BigDecimal.valueOf(150000),
				"서울 서초구"
		));

		productRepository.incrementFavoriteCount(product.getId());
		productRepository.flush();
		entityManager.clear();

		Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
		assertThat(foundProduct.getFavoriteCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("favoriteCount는 원자 UPDATE로 1 감소한다")
	void decrementsFavoriteCount() {
		Member member = memberRepository.save(Member.createUser("favorite-decrement@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("가구/인테리어"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"책상",
				"튼튼한 책상입니다.",
				BigDecimal.valueOf(70000),
				"서울 송파구"
		));
		productRepository.incrementFavoriteCount(product.getId());
		productRepository.flush();
		entityManager.clear();

		productRepository.decrementFavoriteCount(product.getId());
		productRepository.flush();
		entityManager.clear();

		Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
		assertThat(foundProduct.getFavoriteCount()).isZero();
	}

	@Test
	@DisplayName("favoriteCount가 0이면 감소 요청을 해도 음수가 되지 않는다")
	void doesNotDecrementFavoriteCountBelowZero() {
		Member member = memberRepository.save(Member.createUser("favorite-zero@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("도서"));
		Product product = productRepository.saveAndFlush(Product.create(
				member,
				category,
				"자바 책",
				"깨끗한 자바 책입니다.",
				BigDecimal.valueOf(20000),
				"서울 마포구"
		));

		productRepository.decrementFavoriteCount(product.getId());
		productRepository.flush();
		entityManager.clear();

		Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
		assertThat(foundProduct.getFavoriteCount()).isZero();
	}

	@Test
	@DisplayName("삭제된 상품이면 접근 가능한 상품으로 판단하지 않는다")
	void returnsFalseWhenProductIsDeleted() {
		Member member = memberRepository.save(Member.createUser("seller-deleted@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("가구/인테리어"));
		Product product = Product.create(
				member,
				category,
				"의자",
				"사용감 있는 의자입니다.",
				BigDecimal.valueOf(30000),
				"서울 서초구"
		);
		product.softDelete();
		Product savedProduct = productRepository.saveAndFlush(product);

		boolean exists = productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(savedProduct.getId());

		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("숨김 상품이면 접근 가능한 상품으로 판단하지 않는다")
	void returnsFalseWhenProductIsHidden() {
		Member member = memberRepository.save(Member.createUser("seller-hidden@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("도서"));
		Product product = Product.create(
				member,
				category,
				"자바 책",
				"깨끗한 자바 책입니다.",
				BigDecimal.valueOf(15000),
				"서울 송파구"
		);
		product.hide();
		Product savedProduct = productRepository.saveAndFlush(product);

		boolean exists = productRepository.existsByIdAndDeletedAtIsNullAndHiddenFalse(savedProduct.getId());

		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("카테고리별 상품은 최신 등록순으로 조회하고 숨김·삭제 상품은 제외한다")
	void findsProductsByCategoryInLatestOrder() {
		Member member = memberRepository.save(Member.createUser("category-seller@example.com", "encodedPassword", "판매자"));
		Category targetCategory = categoryRepository.save(new Category("생활가전"));
		Category otherCategory = categoryRepository.save(new Category("도서"));
		Product oldProduct = productRepository.save(Product.create(
				member,
				targetCategory,
				"오래된 생활가전",
				"오래된 생활가전 설명",
				BigDecimal.valueOf(10000),
				"서울 강남구"
		));
		Product newProduct = productRepository.save(Product.create(
				member,
				targetCategory,
				"최신 생활가전",
				"최신 생활가전 설명",
				BigDecimal.valueOf(20000),
				"서울 서초구"
		));
		productRepository.save(Product.create(
				member,
				otherCategory,
				"다른 카테고리 상품",
				"다른 카테고리 상품 설명",
				BigDecimal.valueOf(30000),
				"서울 송파구"
		));
		Product hiddenProduct = Product.create(member, targetCategory, "숨김 상품", "숨김 상품 설명", BigDecimal.valueOf(40000), "서울 마포구");
		hiddenProduct.hide();
		productRepository.save(hiddenProduct);
		Product deletedProduct = Product.create(member, targetCategory, "삭제 상품", "삭제 상품 설명", BigDecimal.valueOf(50000), "서울 용산구");
		deletedProduct.softDelete();
		productRepository.saveAndFlush(deletedProduct);

		List<Product> products = productRepository.findAllByCategoryIdAndDeletedAtIsNullAndHiddenFalseOrderByIdDesc(targetCategory.getId());

		assertThat(products).containsExactly(newProduct, oldProduct);
	}

	@Test
	@DisplayName("내 상품 목록은 최신 등록순으로 조회하고 숨김 상품은 포함하며 삭제 상품은 제외한다")
	void findsMyProductsInLatestOrderIncludingHiddenProducts() {
		Member member = memberRepository.save(Member.createUser("my-seller@example.com", "encodedPassword", "판매자"));
		Member otherMember = memberRepository.save(Member.createUser("other-seller@example.com", "encodedPassword", "다른판매자"));
		Category category = categoryRepository.save(new Category("스포츠/레저"));
		Product oldProduct = productRepository.save(Product.create(
				member,
				category,
				"오래된 내 상품",
				"오래된 내 상품 설명",
				BigDecimal.valueOf(10000),
				"서울 강남구"
		));
		Product hiddenProduct = Product.create(member, category, "숨김 내 상품", "숨김 내 상품 설명", BigDecimal.valueOf(20000), "서울 서초구");
		hiddenProduct.hide();
		Product savedHiddenProduct = productRepository.save(hiddenProduct);
		productRepository.save(Product.create(
				otherMember,
				category,
				"다른 회원 상품",
				"다른 회원 상품 설명",
				BigDecimal.valueOf(30000),
				"서울 송파구"
		));
		Product deletedProduct = Product.create(member, category, "삭제 내 상품", "삭제 내 상품 설명", BigDecimal.valueOf(40000), "서울 마포구");
		deletedProduct.softDelete();
		productRepository.saveAndFlush(deletedProduct);

		List<Product> products = productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(member.getId());

		assertThat(products).containsExactly(savedHiddenProduct, oldProduct);
	}

	@Test
	@DisplayName("상품 검색은 키워드로 제목과 설명을 검색하고 숨김·삭제 상품은 제외한다")
	void searchesProductsByKeywordExcludingHiddenAndDeletedProducts() {
		Member member = memberRepository.save(Member.createUser("search-seller@example.com", "encodedPassword", "판매자"));
		Category category = categoryRepository.save(new Category("디지털기기"));
		Product titleMatchedProduct = productRepository.save(Product.create(
				member,
				category,
				"맥북 프로",
				"상태 좋은 노트북입니다.",
				BigDecimal.valueOf(1200000),
				"서울 강남구"
		));
		Product descriptionMatchedProduct = productRepository.save(Product.create(
				member,
				category,
				"노트북 거치대",
				"맥북과 함께 쓰기 좋습니다.",
				BigDecimal.valueOf(30000),
				"서울 서초구"
		));
		Product hiddenProduct = Product.create(member, category, "숨김 맥북", "숨김 상품입니다.", BigDecimal.valueOf(900000), "서울 송파구");
		hiddenProduct.hide();
		productRepository.save(hiddenProduct);
		Product deletedProduct = Product.create(member, category, "삭제 맥북", "삭제 상품입니다.", BigDecimal.valueOf(800000), "서울 마포구");
		deletedProduct.softDelete();
		productRepository.saveAndFlush(deletedProduct);

		List<Product> products = productRepository.findAll(
				ProductSpecification.search("맥북", null, (BigDecimal) null, null, null),
				Sort.by(Sort.Direction.DESC, "id")
		);

		assertThat(products).containsExactly(descriptionMatchedProduct, titleMatchedProduct);
	}

	@Test
	@DisplayName("상품 검색은 카테고리, 가격 범위, 거래 상태를 함께 필터링한다")
	void searchesProductsByCategoryPriceRangeAndTradeStatus() {
		Member member = memberRepository.save(Member.createUser("filter-seller@example.com", "encodedPassword", "판매자"));
		Category targetCategory = categoryRepository.save(new Category("생활가전"));
		Category otherCategory = categoryRepository.save(new Category("도서"));
		Product targetProduct = Product.create(
				member,
				targetCategory,
				"예약 중인 청소기",
				"상태 좋은 청소기입니다.",
				BigDecimal.valueOf(150000),
				"서울 강남구"
		);
		targetProduct.changeTradeStatus(TradeStatus.RESERVED);
		Product savedTargetProduct = productRepository.save(targetProduct);
		Product wrongStatusProduct = productRepository.save(Product.create(
				member,
				targetCategory,
				"판매 중인 청소기",
				"상태 좋은 청소기입니다.",
				BigDecimal.valueOf(160000),
				"서울 서초구"
		));
		Product wrongCategoryProduct = Product.create(member, otherCategory, "예약 중인 책", "청소기 설명이 있는 책입니다.", BigDecimal.valueOf(150000), "서울 송파구");
		wrongCategoryProduct.changeTradeStatus(TradeStatus.RESERVED);
		productRepository.save(wrongCategoryProduct);
		Product wrongPriceProduct = Product.create(member, targetCategory, "비싼 청소기", "비싼 청소기입니다.", BigDecimal.valueOf(500000), "서울 마포구");
		wrongPriceProduct.changeTradeStatus(TradeStatus.RESERVED);
		productRepository.saveAndFlush(wrongPriceProduct);

		List<Product> products = productRepository.findAll(
				ProductSpecification.search(
						"청소기",
						targetCategory.getId(),
						BigDecimal.valueOf(100000),
						BigDecimal.valueOf(200000),
						TradeStatus.RESERVED
				),
				Sort.by(Sort.Direction.DESC, "id")
		);

		assertThat(products).containsExactly(savedTargetProduct);
		assertThat(products).doesNotContain(wrongStatusProduct, wrongCategoryProduct, wrongPriceProduct);
	}
}
