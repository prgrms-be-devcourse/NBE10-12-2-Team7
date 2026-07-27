package com.dongnemarket.trade.service;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.chat.entity.ChatRoom;
import com.dongnemarket.chat.repository.ChatRoomRepository;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.trade.dto.MonthlyTradeStatsResponse;
import com.dongnemarket.trade.dto.TradePurchaseResponse;
import com.dongnemarket.trade.dto.TradeSaleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeService 단위 테스트")
class TradeServiceTest {

    private static final Long ME_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @Mock
    ProductRepository productRepository;

    @Mock
    ChatRoomRepository chatRoomRepository;

    @InjectMocks
    TradeService tradeService;

    @Nested
    @DisplayName("판매내역 조회")
    class GetSales {

        @Test
        @DisplayName("거래완료된 내 상품만 판매내역에 포함된다")
        void includesOnlyCompletedProducts() {
            Product onSale = product(1L, "판매중 상품", BigDecimal.valueOf(10000), null);
            Product completed = completedProduct(2L, "완료 상품", BigDecimal.valueOf(20000), daysAgo(1));
            given(productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(ME_ID))
                    .willReturn(List.of(onSale, completed));

	            List<TradeSaleResponse> sales = tradeService.getSales(ME_ID);
	
	            assertThat(sales).extracting(TradeSaleResponse::getProductId).containsExactly(2L);
	            assertThat(sales.get(0).getRegion()).isEqualTo("서울특별시 강남구 역삼동");
	            assertThat(sales.get(0).getRegionCode()).isEqualTo("1168010100");
	            assertThat(sales.get(0).getRegionName()).isEqualTo("역삼동");
	            assertThat(sales.get(0).getRegionFullName()).isEqualTo("서울특별시 강남구 역삼동");
        }

        @Test
        @DisplayName("판매내역은 최근 완료순으로 정렬된다")
        void sortsByCompletedAtDesc() {
            Product older = completedProduct(1L, "먼저 완료", BigDecimal.valueOf(10000), daysAgo(5));
            Product newer = completedProduct(2L, "나중 완료", BigDecimal.valueOf(20000), daysAgo(1));
            given(productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(ME_ID))
                    .willReturn(List.of(older, newer));

            List<TradeSaleResponse> sales = tradeService.getSales(ME_ID);

            assertThat(sales).extracting(TradeSaleResponse::getProductId).containsExactly(2L, 1L);
        }
    }

    @Nested
    @DisplayName("구매내역 조회")
    class GetPurchases {

        @Test
        @DisplayName("내가 구매자이면서 상품이 거래완료된 방만 구매내역에 포함된다")
        void includesOnlyMyCompletedPurchases() {
            Product completed = completedProduct(1L, "완료 상품", BigDecimal.valueOf(10000), daysAgo(1));
            Product notCompleted = product(2L, "미완료 상품", BigDecimal.valueOf(20000), null);
            ChatRoom asBuyerCompleted = chatRoom(1L, completed, ME_ID, OTHER_ID);
            ChatRoom asBuyerNotCompleted = chatRoom(2L, notCompleted, ME_ID, OTHER_ID);
            ChatRoom asSeller = chatRoom(3L, completedProduct(3L, "내가 판 상품", BigDecimal.valueOf(30000), daysAgo(1)), OTHER_ID, ME_ID);
            given(chatRoomRepository.findMyChatRooms(ME_ID))
                    .willReturn(List.of(asBuyerCompleted, asBuyerNotCompleted, asSeller));

            List<TradePurchaseResponse> purchases = tradeService.getPurchases(ME_ID);

            assertThat(purchases).extracting(TradePurchaseResponse::getProductId).containsExactly(1L);
        }

        @Test
        @DisplayName("구매내역은 최근 완료순으로 정렬된다")
        void sortsByCompletedAtDesc() {
            Product older = completedProduct(1L, "먼저 완료", BigDecimal.valueOf(10000), daysAgo(5));
            Product newer = completedProduct(2L, "나중 완료", BigDecimal.valueOf(20000), daysAgo(1));
            ChatRoom olderRoom = chatRoom(1L, older, ME_ID, OTHER_ID);
            ChatRoom newerRoom = chatRoom(2L, newer, ME_ID, OTHER_ID);
            given(chatRoomRepository.findMyChatRooms(ME_ID)).willReturn(List.of(olderRoom, newerRoom));

            List<TradePurchaseResponse> purchases = tradeService.getPurchases(ME_ID);

            assertThat(purchases).extracting(TradePurchaseResponse::getProductId).containsExactly(2L, 1L);
        }
    }

    @Nested
    @DisplayName("월별 거래 통계")
    class GetMonthlyStats {

        @Test
        @DisplayName("판매와 구매를 월별로 건수·금액을 집계한다")
        void aggregatesByMonth() {
            Product saleJune = completedProduct(1L, "6월 판매", BigDecimal.valueOf(10000), atMonth(2026, 6));
            Product saleJuly = completedProduct(2L, "7월 판매", BigDecimal.valueOf(20000), atMonth(2026, 7));
            Product purchaseProduct = completedProduct(3L, "7월 구매", BigDecimal.valueOf(5000), atMonth(2026, 7));
            given(productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(ME_ID))
                    .willReturn(List.of(saleJune, saleJuly));
            given(chatRoomRepository.findMyChatRooms(ME_ID))
                    .willReturn(List.of(chatRoom(1L, purchaseProduct, ME_ID, OTHER_ID)));

            List<MonthlyTradeStatsResponse> stats = tradeService.getMonthlyStats(ME_ID);

            assertThat(stats).hasSize(2);
            MonthlyTradeStatsResponse july = stats.get(0);
            assertThat(july.getYearMonth()).isEqualTo("2026-07");
            assertThat(july.getSalesCount()).isEqualTo(1);
            assertThat(july.getSalesAmount()).isEqualByComparingTo("20000");
            assertThat(july.getPurchasesCount()).isEqualTo(1);
            assertThat(july.getPurchasesAmount()).isEqualByComparingTo("5000");

            MonthlyTradeStatsResponse june = stats.get(1);
            assertThat(june.getYearMonth()).isEqualTo("2026-06");
            assertThat(june.getSalesCount()).isEqualTo(1);
            assertThat(june.getSalesAmount()).isEqualByComparingTo("10000");
            assertThat(june.getPurchasesCount()).isZero();
            assertThat(june.getPurchasesAmount()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("최신 달이 먼저 온다")
        void sortsMonthsDescending() {
            given(productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(ME_ID)).willReturn(List.of(
                    completedProduct(1L, "1월", BigDecimal.valueOf(1000), atMonth(2026, 1)),
                    completedProduct(2L, "3월", BigDecimal.valueOf(1000), atMonth(2026, 3)),
                    completedProduct(3L, "2월", BigDecimal.valueOf(1000), atMonth(2026, 2))
            ));
            given(chatRoomRepository.findMyChatRooms(ME_ID)).willReturn(List.of());

            List<MonthlyTradeStatsResponse> stats = tradeService.getMonthlyStats(ME_ID);

            assertThat(stats).extracting(MonthlyTradeStatsResponse::getYearMonth)
                    .containsExactly("2026-03", "2026-02", "2026-01");
        }
    }

    private LocalDateTime daysAgo(int days) {
        return LocalDateTime.now().minusDays(days);
    }

    private LocalDateTime atMonth(int year, int month) {
        return LocalDateTime.of(year, month, 15, 0, 0);
    }

    private Member member(Long id) {
        Member member = Member.createUser("member" + id + "@example.com", "encodedPassword", "회원" + id);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Product product(Long id, String title, BigDecimal price, LocalDateTime completedAt) {
	        Product product = Product.create(member(ME_ID), new Category("디지털기기"), title, "설명", price, yeoksam());
	        ReflectionTestUtils.setField(product, "id", id);
        if (completedAt != null) {
            ReflectionTestUtils.setField(product, "completedAt", completedAt);
        }
        return product;
    }

    private Product completedProduct(Long id, String title, BigDecimal price, LocalDateTime completedAt) {
        Product product = product(id, title, price, null);
        product.complete();
        ReflectionTestUtils.setField(product, "completedAt", completedAt);
        return product;
    }

	    private ChatRoom chatRoom(Long id, Product product, Long buyerId, Long sellerId) {
	        ChatRoom room = ChatRoom.of(product, member(buyerId), member(sellerId));
	        ReflectionTestUtils.setField(room, "id", id);
	        return room;
	    }

	    private Region yeoksam() {
	        Region seoul = Region.root("1100000000", "서울특별시", "서울특별시");
	        Region gangnam = Region.child("1168000000", 2, seoul, "서울특별시 강남구", "강남구");
	        return Region.child("1168010100", 3, gangnam, "서울특별시 강남구 역삼동", "역삼동");
	    }
	}
