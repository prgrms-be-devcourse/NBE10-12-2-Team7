package com.dongnemarket.trade.service;

import com.dongnemarket.chat.entity.ChatRoom;
import com.dongnemarket.chat.repository.ChatRoomRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.trade.dto.MonthlyTradeStatsResponse;
import com.dongnemarket.trade.dto.TradePurchaseResponse;
import com.dongnemarket.trade.dto.TradeSaleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 거래내역(판매/구매/월별 통계) 조회. 별도 "거래" 엔티티 없이 Product·ChatRoom을 읽기 전용으로만 참조한다.
 * <p>판매내역은 내 상품 중 거래완료(COMPLETED)분, 구매내역은 스키마상 구매자를 식별할 수 있는 유일한 경로인
 * 채팅방(내가 buyer인 방) 중 상품이 거래완료된 것을 기준으로 한다 — 채팅 없이 완료 처리된 거래는 구매내역에
 * 잡히지 않는 구조적 한계가 있다.
 */
@Service
@Transactional(readOnly = true)
public class TradeService {

    private final ProductRepository productRepository;
    private final ChatRoomRepository chatRoomRepository;

    public TradeService(ProductRepository productRepository, ChatRoomRepository chatRoomRepository) {
        this.productRepository = productRepository;
        this.chatRoomRepository = chatRoomRepository;
    }

    /** 내가 판매자로서 거래완료한 상품 목록(최근 완료순). */
    public List<TradeSaleResponse> getSales(Long memberId) {
        return completedSaleProducts(memberId).stream()
                .map(TradeSaleResponse::from)
                .toList();
    }

    /** 내가 구매자로서 거래완료한 상품 목록(최근 완료순). */
    public List<TradePurchaseResponse> getPurchases(Long memberId) {
        return completedPurchaseRooms(memberId).stream()
                .map(TradePurchaseResponse::from)
                .toList();
    }

    /** 월별(yyyy-MM) 판매/구매 건수·금액 통계. 거래가 있던 달만 포함하며 최신 달이 먼저 온다. */
    public List<MonthlyTradeStatsResponse> getMonthlyStats(Long memberId) {
        Map<YearMonth, MonthlyAgg> byMonth = new TreeMap<>(Comparator.reverseOrder());

        for (Product product : completedSaleProducts(memberId)) {
            MonthlyAgg agg = byMonth.computeIfAbsent(YearMonth.from(product.getCompletedAt()), m -> new MonthlyAgg());
            agg.salesCount++;
            agg.salesAmount = agg.salesAmount.add(product.getPrice());
        }
        for (ChatRoom room : completedPurchaseRooms(memberId)) {
            MonthlyAgg agg = byMonth.computeIfAbsent(
                    YearMonth.from(room.getProduct().getCompletedAt()), m -> new MonthlyAgg());
            agg.purchasesCount++;
            agg.purchasesAmount = agg.purchasesAmount.add(room.getProduct().getPrice());
        }

        return byMonth.entrySet().stream()
                .map(entry -> MonthlyTradeStatsResponse.of(
                        entry.getKey().toString(),
                        entry.getValue().salesCount,
                        entry.getValue().salesAmount,
                        entry.getValue().purchasesCount,
                        entry.getValue().purchasesAmount
                ))
                .toList();
    }

    private List<Product> completedSaleProducts(Long memberId) {
        return productRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdDesc(memberId).stream()
                .filter(product -> product.getTradeStatus() == TradeStatus.COMPLETED)
                .sorted(Comparator.comparing(Product::getCompletedAt).reversed())
                .toList();
    }

    private List<ChatRoom> completedPurchaseRooms(Long memberId) {
        return chatRoomRepository.findMyChatRooms(memberId).stream()
                .filter(room -> room.getBuyerId().equals(memberId))
                .filter(room -> room.getProduct().getTradeStatus() == TradeStatus.COMPLETED)
                .sorted(Comparator.comparing((ChatRoom room) -> room.getProduct().getCompletedAt()).reversed())
                .toList();
    }

    private static class MonthlyAgg {
        long salesCount = 0;
        BigDecimal salesAmount = BigDecimal.ZERO;
        long purchasesCount = 0;
        BigDecimal purchasesAmount = BigDecimal.ZERO;
    }
}
