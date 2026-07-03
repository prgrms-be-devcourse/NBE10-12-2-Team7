export type MemberStatus = 'ACTIVE' | 'SUSPENDED' | 'DELETED'

export const MEMBER_STATUS_LABEL: Record<MemberStatus, string> = {
  ACTIVE: '정상',
  SUSPENDED: '정지',
  DELETED: '탈퇴',
}
