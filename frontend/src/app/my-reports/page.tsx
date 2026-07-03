'use client'

import { useEffect, useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import { REPORT_REASON_LABEL, type ReportReason } from '@/lib/reportReasons'
import styles from './page.module.css'

type ReportType = 'PRODUCT' | 'MEMBER'
type ReportStatus = 'RECEIVED' | 'REVIEWING' | 'COMPLETED' | 'REJECTED'
type FilterTab = '전체' | ReportStatus
type PageStatus = 'loading' | 'ready' | 'error'

interface MyReport {
  reportId: number
  reportType: ReportType
  targetId: number
  reason: ReportReason
  status: ReportStatus
  createdAt: string
}

const TABS: { key: FilterTab; label: string }[] = [
  { key: '전체', label: '전체' },
  { key: 'RECEIVED', label: '접수' },
  { key: 'REVIEWING', label: '처리중' },
  { key: 'COMPLETED', label: '처리완료' },
  { key: 'REJECTED', label: '반려' },
]

function typeTag(type: ReportType) {
  return type === 'MEMBER'
    ? { cls: styles.tagMember, label: '사용자 신고' }
    : { cls: styles.tagProduct, label: '상품 신고' }
}

function statusTag(status: ReportStatus) {
  if (status === 'RECEIVED') return { cls: styles.tagReceived, label: '접수' }
  if (status === 'REVIEWING') return { cls: styles.tagProcessing, label: '처리중' }
  if (status === 'REJECTED') return { cls: styles.tagRejected, label: '반려' }
  return { cls: styles.tagDone, label: '처리완료' }
}

function formatDate(iso: string) {
  return iso.slice(0, 10)
}

export default function MyReportsPage() {
  const [reports, setReports] = useState<MyReport[]>([])
  const [filter, setFilter] = useState<FilterTab>('전체')
  const [status, setStatus] = useState<PageStatus>('loading')
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    let cancelled = false

    apiFetch('/api/members/me/reports')
      .then(async res => {
        const data = await res.json().catch(() => null)
        if (!res.ok) throw new Error(data?.message ?? '신고 내역을 불러오지 못했습니다.')
        if (!cancelled) {
          setReports(data?.data ?? [])
          setStatus('ready')
        }
      })
      .catch(err => {
        if (cancelled) return
        setErrorMsg(err instanceof Error ? err.message : '신고 내역을 불러오지 못했습니다.')
        setStatus('error')
      })

    return () => { cancelled = true }
  }, [])

  const filtered = filter === '전체' ? reports : reports.filter(r => r.status === filter)

  return (
    <main className={styles.wrap}>
      {/* 헤더 */}
      <div className={styles.headRow}>
        <h1>내 신고 내역</h1>
        <p>내가 접수한 신고의 처리 상태를 확인할 수 있어요.</p>
      </div>

      {status === 'loading' && (
        <div className={styles.empty}><p>불러오는 중...</p></div>
      )}

      {status === 'error' && (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>⚠️</div>
          <p>{errorMsg}</p>
        </div>
      )}

      {status === 'ready' && (
        <>
          {/* 탭 필터 */}
          <div className={styles.tabs}>
            {TABS.map(tab => (
              <button
                key={tab.key}
                type="button"
                className={`${styles.tab}${filter === tab.key ? ' ' + styles.on : ''}`}
                onClick={() => setFilter(tab.key)}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* 신고 목록 */}
          {filtered.length > 0 ? (
            <div className={styles.list}>
              {filtered.map(report => {
                const tt = typeTag(report.reportType)
                const st = statusTag(report.status)
                const targetLabel = report.reportType === 'MEMBER' ? '회원' : '상품'
                return (
                  <div key={report.reportId} className={styles.rcard}>
                    <div className={styles.top}>
                      <span className={tt.cls}>{tt.label}</span>
                      <span className={st.cls}>{st.label}</span>
                      <span className={styles.rid}>#{report.reportId}</span>
                    </div>
                    <div className={styles.target}>{targetLabel} #{report.targetId}</div>
                    <div className={styles.reason}>
                      <b>사유</b>&nbsp; {REPORT_REASON_LABEL[report.reason] ?? report.reason}
                    </div>
                    <div className={styles.foot}>
                      <span className={styles.date}>{formatDate(report.createdAt)} 접수</span>
                    </div>
                  </div>
                )
              })}
            </div>
          ) : (
            <div className={styles.empty}>
              <div className={styles.emptyIcon}>🗂️</div>
              <p>해당 상태의 신고 내역이 없어요.</p>
            </div>
          )}
        </>
      )}

      <div className={styles.apiNote}>GET /api/members/me/reports</div>
    </main>
  )
}
