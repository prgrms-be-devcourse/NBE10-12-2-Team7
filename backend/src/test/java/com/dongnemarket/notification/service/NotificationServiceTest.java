package com.dongnemarket.notification.service;

import java.math.BigDecimal;
import java.util.List;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.notification.entity.Notification;
import com.dongnemarket.notification.entity.NotificationType;
import com.dongnemarket.notification.repository.NotificationRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림 저장 로직의 비자명 분기(코얼레싱 레이스 수렴)에 대한 통합 테스트.
 * <p>동시성으로 안읽은 알림이 2행 이상 생긴 상황은 HTTP로 결정적으로 재현하기 어려워,
 * 서비스 메서드를 직접 호출해 <b>1행으로 수렴</b>하는지 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("알림 저장(코얼레싱 수렴) 통합 테스트")
class NotificationServiceTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    RegionRepository regionRepository;

    private Member recipient;
    private Long productId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        recipient = memberRepository.save(Member.createUser("owner@example.com", "encoded-pw", "owner"));
        Category category = categoryRepository.save(new Category("알림서비스테스트전용카테고리"));
        Product product = productRepository.save(
                Product.create(recipient, category, "맥북 프로", "상태 좋음", BigDecimal.valueOf(1_500_000), findRegion("1168010100")));
        categoryId = category.getId();
        productId = product.getId();
    }

    private Region findRegion(String code) {
        return regionRepository.findByCode(code).orElseThrow();
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
        categoryRepository.deleteById(categoryId);
    }

    @Test
    @DisplayName("레이스로 안읽은 알림이 2행 생겨도 이후 알림 처리 시 1행으로 수렴한다")
    void notifyComment_dedupesDuplicateUnread() {
        // given: 동시 이벤트 레이스로 같은 (수신자, 상품, COMMENT) 안읽은 알림이 2행 생긴 상황
        notificationRepository.save(
                Notification.of(recipient, NotificationType.COMMENT, "\"맥북 프로\" 글에 새로운 댓글이 작성되었습니다.", productId));
        notificationRepository.save(
                Notification.of(recipient, NotificationType.COMMENT, "\"맥북 프로\" 글에 새로운 댓글이 작성되었습니다.", productId));

        // when: 또 댓글 알림이 들어오면
        notificationService.notifyComment(recipient.getId(), productId, "맥북 프로");

        // then: 안읽은 알림은 1행으로 수렴한다
        List<Notification> unread = notificationRepository
                .findByRecipient_IdAndProductIdAndTypeAndIsReadFalseOrderByLastNotifiedAtDesc(
                        recipient.getId(), productId, NotificationType.COMMENT);
        assertThat(unread).hasSize(1);
    }

    @Test
    @DisplayName("안읽은 알림이 없으면 새 알림을 저장한다")
    void notifyComment_savesWhenNone() {
        // when
        notificationService.notifyComment(recipient.getId(), productId, "맥북 프로");

        // then
        List<Notification> unread = notificationRepository
                .findByRecipient_IdAndProductIdAndTypeAndIsReadFalseOrderByLastNotifiedAtDesc(
                        recipient.getId(), productId, NotificationType.COMMENT);
        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).getMessage()).contains("맥북 프로");
    }
}
