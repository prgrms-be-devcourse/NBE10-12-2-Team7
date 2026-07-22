export type MannerScoreReason =
  | 'RATING_RECEIVED'
  | 'TRADE_COMPLETED'
  | 'REPORT_CONFIRMED'
  | 'FALSE_REPORT_PENALTY'
  | 'TIME_RECOVERY'

export const MANNER_SCORE_REASON_LABEL: Record<MannerScoreReason, string> = {
  RATING_RECEIVED: '별점 후기 반영',
  TRADE_COMPLETED: '거래 완료 보너스',
  REPORT_CONFIRMED: '신고 처리 결과 반영',
  FALSE_REPORT_PENALTY: '무고성 신고 페널티',
  TIME_RECOVERY: '시간 경과 회복',
}
