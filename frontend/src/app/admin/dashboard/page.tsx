'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { getAccessToken } from '@/lib/auth'
import { REPORT_STATUS_LABEL, REPORT_TYPE_LABEL, type ReportStatus, type ReportType } from '@/lib/reportStatus'
import styles from '../admin.module.css'

interface Dashboard {
  totalMembers: number
  totalProducts: number
  totalReports: number
  pendingReports: number
  totalComments: number
}

interface MemberRow { memberId: number; nickname: string; createdAt: string }
interface ProductRow { productId: number; title: string; createdAt: string }
interface ReportRow {
  reportId: number
  reportType: ReportType
  targetMemberId: number | null
  targetProductId: number | null
  status: ReportStatus
  createdAt: string
}

type Status = 'loading' | 'ready' | 'error'

function reportStatusTagCls(s: ReportStatus) {
  if (s === 'RECEIVED') return styles.tagGreen
  if (s === 'REVIEWING') return styles.tagAmber
  if (s === 'REJECTED') return styles.tagRose
  return styles.tagNeut
}
function reportTypeTagCls(t: ReportType) {
  return t === 'PRODUCT' ? styles.tagPink : styles.tagRose
}

export default function AdminDashboardPage() {
  const [status, setStatus] = useState<Status>('loading')
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [recentMembers, setRecentMembers] = useState<MemberRow[]>([])
  const [recentProducts, setRecentProducts] = useState<ProductRow[]>([])
  const [recentReports, setRecentReports] = useState<ReportRow[]>([])

  useEffect(() => {
    const token = getAccessToken()
    if (!token) { setStatus('error'); return }
    const headers = { Authorization: `Bearer ${token}` }

    let cancelled = false
    Promise.all([
      fetch('/api/admin/dashboard', { headers }).then(r => r.json()),
      fetch('/api/admin/members', { headers }).then(r => r.json()),
      fetch('/api/admin/products', { headers }).then(r => r.json()),
      fetch('/api/admin/reports', { headers }).then(r => r.json()),
    ]).then(([dashRes, membersRes, productsRes, reportsRes]) => {
      if (cancelled) return
      setDashboard(dashRes?.data ?? null)
      const members: MemberRow[] = (membersRes?.data ?? []) as MemberRow[]
      const products: ProductRow[] = (productsRes?.data ?? []) as ProductRow[]
      const reports: ReportRow[] = (reportsRes?.data ?? []) as ReportRow[]
      setRecentMembers([...members].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5))
      setRecentProducts([...products].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5))
      setRecentReports([...reports].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5))
      setStatus('ready')
    }).catch(() => { if (!cancelled) setStatus('error') })
    return () => { cancelled = true }
  }, [])

  if (status === 'loading') return <div className={styles.empty}><p>불러오는 중...</p></div>
  if (status === 'error' || !dashboard) return <div className={styles.empty}><p>대시보드를 불러오지 못했어요.</p></div>

  return (
    <>
      <div className={styles.ptitle}>운영 대시보드</div>
      <div className={styles.pdesc}>서비스 전체 현황 요약 · GET /api/admin/dashboard</div>

      <div className={styles.statGrid}>
        <div className={styles.stat}><div className={styles.statL}>전체 회원 수</div><div className={styles.statN}>{dashboard.totalMembers.toLocaleString()}</div></div>
        <div className={styles.stat}><div className={styles.statL}>전체 상품 수</div><div className={styles.statN}>{dashboard.totalProducts.toLocaleString()}</div></div>
        <div className={styles.stat}><div className={styles.statL}>전체 신고 수</div><div className={styles.statN}>{dashboard.totalReports.toLocaleString()}</div></div>
        <div className={styles.stat}><div className={styles.statL}>처리 대기 신고 수</div><div className={styles.statN}>{dashboard.pendingReports.toLocaleString()}</div></div>
        <div className={styles.stat}><div className={styles.statL}>전체 댓글 수</div><div className={styles.statN}>{dashboard.totalComments.toLocaleString()}</div></div>
      </div>

      <div className={styles.cols2}>
        <div className={styles.panel}>
          <h3>최근 가입 회원</h3>
          <div className={styles.tablewrap}>
            <table>
              <thead><tr><th>회원ID</th><th>닉네임</th><th>가입일</th></tr></thead>
              <tbody>
                {recentMembers.map(m => (
                  <tr key={m.memberId}>
                    <td><Link href={`/admin/members/${m.memberId}`}>{m.memberId}</Link></td>
                    <td>{m.nickname}</td>
                    <td>{m.createdAt.slice(0, 10)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className={styles.panel}>
          <h3>최근 등록 상품</h3>
          <div className={styles.tablewrap}>
            <table>
              <thead><tr><th>상품ID</th><th>상품명</th><th>등록일</th></tr></thead>
              <tbody>
                {recentProducts.map(p => (
                  <tr key={p.productId}>
                    <td><Link href={`/admin/products/${p.productId}`}>{p.productId}</Link></td>
                    <td>{p.title}</td>
                    <td>{p.createdAt.slice(0, 10)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div className={styles.panel}>
        <h3>최근 신고 목록</h3>
        <div className={styles.tablewrap}>
          <table>
            <thead><tr><th>#</th><th>유형</th><th>대상</th><th>상태</th><th>신고일</th></tr></thead>
            <tbody>
              {recentReports.map(r => (
                <tr key={r.reportId}>
                  <td><Link href={`/admin/reports/${r.reportId}`}>{r.reportId}</Link></td>
                  <td><span className={`${styles.tag} ${reportTypeTagCls(r.reportType)}`}>{REPORT_TYPE_LABEL[r.reportType]}</span></td>
                  <td>{r.reportType === 'PRODUCT' ? `상품 #${r.targetProductId}` : `회원 #${r.targetMemberId}`}</td>
                  <td><span className={`${styles.tag} ${reportStatusTagCls(r.status)}`}>{REPORT_STATUS_LABEL[r.status]}</span></td>
                  <td>{r.createdAt.slice(0, 10)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
