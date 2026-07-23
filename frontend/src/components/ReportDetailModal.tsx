'use client'

import { useEffect, useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import { REPORT_REASON_LABEL, type ReportReason } from '@/lib/reportReasons'
import styles from './ReportDetailModal.module.css'

type ReportType = 'PRODUCT' | 'MEMBER'
type ReportStatus = 'RECEIVED' | 'REVIEWING' | 'COMPLETED' | 'REJECTED'
type ModalStatus = 'loading' | 'ready' | 'error'

interface ReportDetail {
  reportId: number
  reportType: ReportType
  targetId: number
  reason: ReportReason
  content: string
  status: ReportStatus
  evidenceImageUrl: string | null
  createdAt: string
}

const STATUS_LABEL: Record<ReportStatus, string> = {
  RECEIVED: '접수', REVIEWING: '처리중', COMPLETED: '처리완료', REJECTED: '반려',
}

interface Props {
  reportId: number
  targetLabel: string
  onClose: () => void
}

export default function ReportDetailModal({ reportId, targetLabel, onClose }: Props) {
  const [status, setStatus] = useState<ModalStatus>('loading')
  const [report, setReport] = useState<ReportDetail | null>(null)

  useEffect(() => {
    let cancelled = false
    apiFetch(`/api/members/me/reports/${reportId}`)
      .then(async res => {
        const data = await res.json().catch(() => null)
        if (cancelled) return
        if (!res.ok || !data?.data) { setStatus('error'); return }
        setReport(data.data)
        setStatus('ready')
      })
      .catch(() => { if (!cancelled) setStatus('error') })
    return () => { cancelled = true }
  }, [reportId])

  return (
    <div className={styles.overlay} onClick={e => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <div className={styles.head}>
          <h3>신고 상세</h3>
          <button type="button" className={styles.closeBtn} onClick={onClose} aria-label="닫기">✕</button>
        </div>

        {status === 'loading' && <p className={styles.msg}>불러오는 중...</p>}
        {status === 'error' && <p className={styles.msg}>신고 상세 정보를 불러오지 못했어요.</p>}

        {status === 'ready' && report && (
          <>
            <dl className={styles.def}>
              <dt>신고번호</dt><dd>#{report.reportId}</dd>
              <dt>신고 대상</dt><dd>{targetLabel}</dd>
              <dt>신고 사유</dt><dd>{REPORT_REASON_LABEL[report.reason] ?? report.reason}</dd>
              <dt>처리 상태</dt><dd>{STATUS_LABEL[report.status]}</dd>
              <dt>접수일</dt><dd>{report.createdAt.slice(0, 10)}</dd>
            </dl>

            <div className={styles.contentBox}>
              <div className={styles.contentLabel}>상세 내용</div>
              <p className={styles.contentText}>{report.content || '작성된 상세 내용이 없어요.'}</p>
            </div>

            {report.evidenceImageUrl && (
              <a href={report.evidenceImageUrl} target="_blank" rel="noreferrer" className={styles.evidenceLink}>
                <img src={report.evidenceImageUrl} alt="증빙 이미지" />
              </a>
            )}
          </>
        )}
      </div>
    </div>
  )
}
