package com.dongnemarket.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.common.event.FavoriteAddedEvent;
import com.dongnemarket.global.common.event.FavoriteRemovedEvent;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.support.BaseIntegrationTest;

@DisplayName("[통합] 상품 관심 수 이벤트 리스너")
@Transactional
class ProductFavoriteCountHandlerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	ApplicationEventPublisher eventPublisher;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	ProductRepository productRepository;

	@Test
	@DisplayName("FavoriteAddedEvent를 수신하면 상품 관심 수를 1 증가시킨다")
	void incrementsFavoriteCountWhenFavoriteAddedEventPublished() {
		Product product = saveProduct("added-event@example.com", "이벤트 증가 상품");

		eventPublisher.publishEvent(new FavoriteAddedEvent(product.getId()));

		Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
		assertThat(foundProduct.getFavoriteCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("FavoriteRemovedEvent를 수신하면 상품 관심 수를 1 감소시킨다")
	void decrementsFavoriteCountWhenFavoriteRemovedEventPublished() {
		Product product = saveProduct("removed-event@example.com", "이벤트 감소 상품");
		productRepository.incrementFavoriteCount(product.getId());

		eventPublisher.publishEvent(new FavoriteRemovedEvent(product.getId()));

		Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
		assertThat(foundProduct.getFavoriteCount()).isZero();
	}

	@Nested
	@DisplayName("관심 수 감소")
	class DecreaseFavoriteCount {

		@Test
		@DisplayName("FavoriteRemovedEvent를 0인 상품에 발행해도 관심 수가 음수가 되지 않는다")
		void doesNotDecreaseFavoriteCountBelowZeroWhenFavoriteRemovedEventPublished() {
			Product product = saveProduct("removed-zero-event@example.com", "이벤트 감소 하한 상품");

			eventPublisher.publishEvent(new FavoriteRemovedEvent(product.getId()));

			Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
			assertThat(foundProduct.getFavoriteCount()).isZero();
		}
	}

	@Nested
	@DisplayName("오염된 입력")
	class DirtyInput {

		@Test
		@DisplayName("존재하지 않는 상품 이벤트를 발행해도 예외가 발생하지 않고 정상 상품은 변경되지 않는다")
		void doesNotThrowAndDoesNotChangeNormalProductWhenMissingProductEventsPublished() {
			Product product = saveProduct("missing-event@example.com", "오염된 입력 기준 상품");
			Long missingProductId = Long.MAX_VALUE;

			assertThatCode(() -> {
				eventPublisher.publishEvent(new FavoriteAddedEvent(missingProductId));
				eventPublisher.publishEvent(new FavoriteRemovedEvent(missingProductId));
			}).doesNotThrowAnyException();

			Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
			assertThat(foundProduct.getFavoriteCount()).isZero();
		}
	}

	private Product saveProduct(String email, String title) {
		Member member = memberRepository.save(Member.createUser(uniqueEmail(email), "encodedPassword", uniqueNickname()));
		Category category = categoryRepository.findAll().get(0);
		return productRepository.saveAndFlush(Product.create(
				member,
				category,
				title,
				"관심 수 이벤트 테스트 상품입니다.",
				BigDecimal.valueOf(10000),
				"서울 강남구"
		));
	}

	private String uniqueEmail(String prefix) {
		return UUID.randomUUID() + "-" + prefix;
	}

	private String uniqueNickname() {
		return "판매자-" + UUID.randomUUID().toString().substring(0, 8);
	}
}
