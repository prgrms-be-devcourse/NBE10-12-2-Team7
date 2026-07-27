'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useEffect, useRef, useState } from 'react'
import { getAccessToken } from '@/lib/auth'
import { TRADE_STATUS_LABEL, type TradeStatus } from '@/lib/tradeStatus'
import { MEMBER_STATUS_LABEL, type MemberStatus } from '@/lib/memberStatus'
import styles from '../../admin.module.css'

interface Product {
  productId: number
  memberId: number
  categoryId: number
  title: string
  description: string
  price: number
  tradeStatus: TradeStatus
  regionFullName: string
  viewCount: number
  hidden: boolean
  deletedAt: string | null
  createdAt: string
}

interface Category { id: number; name: string }
interface Seller { nickname: string; status: MemberStatus }
interface Report { reportType: 'PRODUCT' | 'MEMBER'; targetProductId: number | null }

type Status = 'loading' | 'ready' | 'error'

function tradeTagCls(s: TradeStatus) {
  if (s === 'ON_SALE') return styles.tagGreen
  if (s === 'RESERVED') return styles.tagBlue
  return styles.tagNeut
}
function memberTagCls(s: MemberStatus) {
  if (s === 'ACTIVE') return styles.tagGreen
  if (s === 'SUSPENDED') return styles.tagAmber
  return styles.tagNeut
}

export default function AdminProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [status, setStatus] = useState<Status>(() => (getAccessToken() ? 'loading' : 'error'))
  const [product, setProduct] = useState<Product | null>(null)
  const [categoryName, setCategoryName] = useState('')
  const [seller, setSeller] = useState<Seller | null>(null)
  const [reportCount, setReportCount] = useState(0)
  const [processing, setProcessing] = useState(false)

  const [toastText, setToastText] = useState('')
  const [toastOn, setToastOn] = useState(false)
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  function showToast(msg: string) {
    setToastText(msg); setToastOn(true)
    if (toastTimer.current) clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToastOn(false), 1900)
  }

  useEffect(() => {
    const token = getAccessToken()
    if (!token) return
    const headers = { Authorization: `Bearer ${token}` }

    let cancelled = false
    fetch(`/api/admin/products/${id}`, { headers })
      .then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) }))
      .then(productRes => {
        if (cancelled) return
        if (!productRes.ok || !productRes.data?.data) { setStatus('error'); return }
        const p: Product = productRes.data.data
        setProduct(p)

        Promise.all([
          fetch('/api/categories').then(r => r.json()).catch(() => null),
          fetch(`/api/admin/members/${p.memberId}`, { headers }).then(r => r.json()).catch(() => null),
          fetch('/api/admin/reports', { headers }).then(r => r.json()).catch(() => null),
        ]).then(([categoriesRes, sellerRes, reportsRes]) => {
          if (cancelled) return
          const categories: Category[] = categoriesRes?.data ?? []
          setCategoryName(categories.find(c => c.id === p.categoryId)?.name ?? `카테고리 #${p.categoryId}`)
          setSeller(sellerRes?.data ?? null)
          const reports: Report[] = reportsRes?.data ?? []
          setReportCount(reports.filter(r => r.reportType === 'PRODUCT' && r.targetProductId === p.productId).length)
          setStatus('ready')
        })
      })
      .catch(() => { if (!cancelled) setStatus('error') })
    return () => { cancelled = true }
  }, [id])

  async function hideProduct() {
    if (!product) return
    if (!window.confirm('이 상품을 숨김 처리할까요? 되돌릴 수 없습니다.')) return
    const token = getAccessToken()
    if (!token) return
    setProcessing(true)
    try {
      const res = await fetch(`/api/admin/products/${product.productId}/hidden`, {
        method: 'PATCH', headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) { const data = await res.json().catch(() => null); showToast(data?.message ?? '숨김 처리 중 오류가 발생했습니다.'); return }
      setProduct(prev => prev ? { ...prev, hidden: true } : prev)
      showToast('상품을 숨김 처리했어요')
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    } finally {
      setProcessing(false)
    }
  }

  async function deleteProduct() {
    if (!product) return
    if (!window.confirm('이 상품을 삭제할까요? 되돌릴 수 없습니다.')) return
    const token = getAccessToken()
    if (!token) return
    setProcessing(true)
    try {
      const res = await fetch(`/api/admin/products/${product.productId}`, {
        method: 'DELETE', headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) { const data = await res.json().catch(() => null); showToast(data?.message ?? '삭제 중 오류가 발생했습니다.'); return }
      setProduct(prev => prev ? { ...prev, deletedAt: new Date().toISOString() } : prev)
      showToast('상품을 삭제했어요')
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    } finally {
      setProcessing(false)
    }
  }

  if (status === 'loading') return <div className={styles.empty}><p>불러오는 중...</p></div>
  if (status === 'error' || !product) {
    return (
      <div className={styles.empty}>
        <p>상품 정보를 불러오지 못했어요.</p>
        <Link href="/admin/products" className="btn ghost">목록으로</Link>
      </div>
    )
  }

  return (
    <>
      <div className={styles.ptitle}>상품 상세 / 숨김 처리</div>
      <div className={styles.cols2}>
        <div className={styles.panel}>
          <h3>상품 기본 정보</h3>
          <dl className={styles.def}>
            <dt>상품ID</dt><dd>{product.productId}</dd>
            <dt>상품명</dt><dd>{product.title}</dd>
            <dt>가격</dt><dd>{product.price === 0 ? '나눔' : `${product.price.toLocaleString('ko-KR')}원`}</dd>
            <dt>카테고리</dt><dd>{categoryName}</dd>
            <dt>지역</dt><dd>{product.regionFullName}</dd>
            <dt>거래상태</dt><dd><span className={`${styles.tag} ${tradeTagCls(product.tradeStatus)}`}>{TRADE_STATUS_LABEL[product.tradeStatus]}</span></dd>
            <dt>숨김여부</dt><dd><span className={`${styles.tag} ${product.hidden ? styles.tagRose : styles.tagNeut}`}>{product.hidden ? '숨김' : '노출'}</span></dd>
            <dt>신고 수</dt><dd>{reportCount > 0 ? <span className={`${styles.tag} ${styles.tagRose}`}>{reportCount}건</span> : '0건'}</dd>
            <dt>등록일</dt><dd>{product.createdAt.slice(0, 10)}</dd>
          </dl>

          <h3 style={{ marginTop: 18 }}>판매자 정보</h3>
          <dl className={styles.def}>
            <dt>판매자ID</dt><dd>{product.memberId}</dd>
            <dt>닉네임</dt><dd>{seller?.nickname ?? '-'}</dd>
            <dt>상태</dt><dd>{seller ? <span className={`${styles.tag} ${memberTagCls(seller.status)}`}>{MEMBER_STATUS_LABEL[seller.status]}</span> : '-'}</dd>
          </dl>
        </div>
        <div className={styles.panel}>
          <h3>운영 처리</h3>
          <div className={styles.btnRow}>
            <button type="button" className={styles.btnWarn} disabled={product.hidden || processing} onClick={hideProduct}>숨김 처리</button>
            <button type="button" className="btn danger" disabled={!!product.deletedAt || processing} onClick={deleteProduct}>삭제 처리</button>
          </div>
          {product.hidden && <p className={styles.pdesc} style={{ marginTop: 10, marginBottom: 0 }}>이미 숨김 처리된 상품입니다. (숨김 해제 API는 제공되지 않습니다.)</p>}
        </div>
      </div>
      <div className={`toast${toastOn ? ' show' : ''}`}>{toastText}</div>
    </>
  )
}
