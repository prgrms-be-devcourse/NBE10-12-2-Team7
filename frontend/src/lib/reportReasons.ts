export type ReportReason =
  | 'FAKE_ITEM'
  | 'FRAUD_SUSPECTED'
  | 'PROHIBITED_ITEM'
  | 'INAPPROPRIATE_CONTENT'
  | 'ETC'

export const REPORT_REASONS: { value: ReportReason; label: string; description: string }[] = [
  { value: 'FAKE_ITEM', label: '가짜·위조 상품', description: '가짜·위조 상품' },
  { value: 'FRAUD_SUSPECTED', label: '사기 의심', description: '사기 의심 (허위 매물·거래 사기)' },
  { value: 'PROHIBITED_ITEM', label: '판매 금지 품목', description: '판매 금지 품목 (위조품 등)' },
  { value: 'INAPPROPRIATE_CONTENT', label: '부적절한 언행', description: '욕설·비방 등 부적절한 언행' },
  { value: 'ETC', label: '기타', description: '기타' },
]

export const REPORT_REASON_LABEL: Record<ReportReason, string> = Object.fromEntries(
  REPORT_REASONS.map(r => [r.value, r.label])
) as Record<ReportReason, string>
