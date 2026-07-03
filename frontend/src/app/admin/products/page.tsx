'use client'

import Link from 'next/link'
import { useEffect, useMemo, useState } from 'react'
import { getAccessToken } from '@/lib/auth'
import { TRADE_STATUS_LABEL, type TradeStatus } from '@/lib/tradeStatus'
import styles from '../admin.module.css'

interface Product {
  productId: number
  memberId: number
  categoryId: number
  title: string
  price: number
  tradeStatus: TradeStatus
  region: string
  viewCount: number
  hidden: boolean
  deletedAt: string | null
  createdAt: string
}

interface Report { reportType: 'PRODUCT' | 'MEMBER'; targetProductId: number | null }

type Status = 'loading' | 'ready' | 'error'

function tradeTagCls(s: TradeStatus) {
  if (s === 'ON_SALE') return styles.tagGreen
  if (s === 'RESERVED') return styles.tagBlue
  return styles.tagNeut
}

export default function AdminProductsPage() {
  const [status, setStatus] = useState<Status>('loading')
  const [products, setProducts] = useState<Product[]>([])
  const [reportCounts, setReportCounts] = useState<Record<number, number>>({})

  const [keyword, setKeyword] = useState('')
  const [query, setQuery] = useState('')
  const [tradeFilter, setTradeFilter] = useState<'ALL' | TradeStatus>('ALL')
  const [hiddenFilter, setHiddenFilter] = useState<'ALL' | 'VISIBLE' | 'HIDDEN'>('ALL')

  useEffect(() => {
    const token = getAccessToken()
    if (!token) { setStatus('error'); return }
    const headers = { Authorization: `Bearer ${token}` }

    let cancelled = false
    Promise.all([
      fetch('/api/admin/products', { headers }).then(r => r.json()),
      fetch('/api/admin/reports', { headers }).then(r => r.json()),
    ]).then(([productsRes, reportsRes]) => {
      if (cancelled) return
      setProducts(productsRes?.data ?? [])
      const reports: Report[] = reportsRes?.data ?? []
      const counts: Record<number, number> = {}
      reports.forEach(r => {
        if (r.reportType === 'PRODUCT' && r.targetProductId != null) {
          counts[r.targetProductId] = (counts[r.targetProductId] ?? 0) + 1
        }
      })
      setReportCounts(counts)
      setStatus('ready')
    }).catch(() => { if (!cancelled) setStatus('error') })
    return () => { cancelled = true }
  }, [])

  async function hideProduct(productId: number) {
    if (!window.confirm('이 상품을 숨김 처리할까요? 되돌릴 수 없습니다.')) return
    const token = getAccessToken()
    if (!token) return
    try {
      const res = await fetch(`/api/admin/products/${productId}/hidden`, {
        method: 'PATCH', headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) return
      setProducts(prev => prev.map(p => p.productId === productId ? { ...p, hidden: true } : p))
    } catch {}
  }

  async function deleteProduct(productId: number) {
    if (!window.confirm('이 상품을 삭제할까요? 되돌릴 수 없습니다.')) return
    const token = getAccessToken()
    if (!token) return
    try {
      const res = await fetch(`/api/admin/products/${productId}`, {
        method: 'DELETE', headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) return
      setProducts(prev => prev.map(p => p.productId === productId ? { ...p, deletedAt: new Date().toISOString() } : p))
    } catch {}
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    setQuery(keyword.trim().toLowerCase())
  }

  const filtered = useMemo(() => {
    let list = products
    if (tradeFilter !== 'ALL') list = list.filter(p => p.tradeStatus === tradeFilter)
    if (hiddenFilter === 'VISIBLE') list = list.filter(p => !p.hidden)
    if (hiddenFilter === 'HIDDEN') list = list.filter(p => p.hidden)
    if (query) list = list.filter(p => p.title.toLowerCase().includes(query))
    return list
  }, [products, tradeFilter, hiddenFilter, query])

  if (status === 'loading') return <div className={styles.empty}><p>불러오는 중...</p></div>
  if (status === 'error') return <div className={styles.empty}><p>상품 목록을 불러오지 못했어요.</p></div>

  return (
    <>
      <div className={styles.ptitle}>상품 목록 관리</div>
      <div className={styles.pdesc}>전체 상품 검색, 숨김 / 삭제 처리 · GET /api/admin/products</div>
      <div className={styles.panel}>
        <form className={styles.filter} onSubmit={handleSearch}>
          <div className={styles.field}>
            <label>검색어 (상품명)</label>
            <input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="상품명 검색" />
          </div>
          <div className={styles.field}>
            <label>거래 상태</label>
            <select value={tradeFilter} onChange={e => setTradeFilter(e.target.value as 'ALL' | TradeStatus)}>
              <option value="ALL">전체</option>
              <option value="ON_SALE">판매중</option>
              <option value="RESERVED">예약중</option>
              <option value="COMPLETED">거래완료</option>
            </select>
          </div>
          <div className={styles.field}>
            <label>숨김 여부</label>
            <select value={hiddenFilter} onChange={e => setHiddenFilter(e.target.value as 'ALL' | 'VISIBLE' | 'HIDDEN')}>
              <option value="ALL">전체</option>
              <option value="VISIBLE">노출</option>
              <option value="HIDDEN">숨김</option>
            </select>
          </div>
          <button type="submit" className="btn">검색</button>
        </form>

        <div className={styles.tablewrap}>
          <table>
            <thead>
              <tr><th>상품ID</th><th>판매자ID</th><th>상품명</th><th>가격</th><th>거래상태</th><th>숨김여부</th><th>신고수</th><th>등록일</th><th>관리</th></tr>
            </thead>
            <tbody>
              {filtered.map(p => (
                <tr key={p.productId}>
                  <td>{p.productId}</td>
                  <td>{p.memberId}</td>
                  <td>{p.title}</td>
                  <td>{p.price.toLocaleString('ko-KR')}</td>
                  <td><span className={`${styles.tag} ${tradeTagCls(p.tradeStatus)}`}>{TRADE_STATUS_LABEL[p.tradeStatus]}</span></td>
                  <td><span className={`${styles.tag} ${p.hidden ? styles.tagRose : styles.tagNeut}`}>{p.hidden ? '숨김' : '노출'}</span></td>
                  <td>{reportCounts[p.productId] ?? 0}</td>
                  <td>{p.createdAt.slice(0, 10)}</td>
                  <td>
                    <div className={styles.btnRow}>
                      <Link href={`/admin/products/${p.productId}`} className="btn ghost">상세</Link>
                      <button type="button" className={styles.btnWarn} disabled={p.hidden} onClick={() => hideProduct(p.productId)}>숨김</button>
                      <button type="button" className="btn danger" disabled={!!p.deletedAt} onClick={() => deleteProduct(p.productId)}>삭제</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
