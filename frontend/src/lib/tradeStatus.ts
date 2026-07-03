export type TradeStatus = 'ON_SALE' | 'RESERVED' | 'COMPLETED'

export const TRADE_STATUS_LABEL: Record<TradeStatus, string> = {
  ON_SALE: '판매중',
  RESERVED: '예약중',
  COMPLETED: '거래완료',
}
