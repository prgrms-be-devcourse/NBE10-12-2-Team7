export type ReportStatus = 'RECEIVED' | 'REVIEWING' | 'COMPLETED' | 'REJECTED'

export const REPORT_STATUS_LABEL: Record<ReportStatus, string> = {
  RECEIVED: '접수',
  REVIEWING: '처리중',
  COMPLETED: '처리완료',
  REJECTED: '반려',
}

export type ReportType = 'PRODUCT' | 'MEMBER'

export const REPORT_TYPE_LABEL: Record<ReportType, string> = {
  PRODUCT: '상품',
  MEMBER: '사용자',
}
