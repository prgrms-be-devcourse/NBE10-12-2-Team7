package com.dongnemarket.trade.dto;

import java.math.BigDecimal;

/** 월별(yyyy-MM) 거래 통계 한 행. 판매/구매 각각 건수와 금액 합계를 담는다. */
public class MonthlyTradeStatsResponse {

    private final String yearMonth;
    private final long salesCount;
    private final BigDecimal salesAmount;
    private final long purchasesCount;
    private final BigDecimal purchasesAmount;

    private MonthlyTradeStatsResponse(String yearMonth, long salesCount, BigDecimal salesAmount,
                                      long purchasesCount, BigDecimal purchasesAmount) {
        this.yearMonth = yearMonth;
        this.salesCount = salesCount;
        this.salesAmount = salesAmount;
        this.purchasesCount = purchasesCount;
        this.purchasesAmount = purchasesAmount;
    }

    public static MonthlyTradeStatsResponse of(String yearMonth, long salesCount, BigDecimal salesAmount,
                                               long purchasesCount, BigDecimal purchasesAmount) {
        return new MonthlyTradeStatsResponse(yearMonth, salesCount, salesAmount, purchasesCount, purchasesAmount);
    }

    public String getYearMonth() { return yearMonth; }
    public long getSalesCount() { return salesCount; }
    public BigDecimal getSalesAmount() { return salesAmount; }
    public long getPurchasesCount() { return purchasesCount; }
    public BigDecimal getPurchasesAmount() { return purchasesAmount; }
}
