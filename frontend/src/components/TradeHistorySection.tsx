'use client'

import { useEffect, useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import styles from './TradeHistorySection.module.css'

type Tab = 'SALES' | 'PURCHASES' | 'STATS'
type SectionStatus = 'loading' | 'ready' | 'error'

interface TradeSale {
  productId: number
  title: string
  thumbnailUrl: string | null
  price: number
  region: string
  completedAt: string
}

interface TradePurchase {
  productId: number
  title: string
  thumbnailUrl: string | null
  price: number
  sellerNickname: string
  roomId: number
  completedAt: string
}

interface MonthlyStat {
  yearMonth: string
  salesCount: number
  salesAmount: number
  purchasesCount: number
  purchasesAmount: number
}

const TABS: { key: Tab; label: string }[] = [
  { key: 'SALES', label: '판매내역' },
  { key: 'PURCHASES', label: '구매내역' },
  { key: 'STATS', label: '월별 통계' },
]

function formatPrice(price: number) {
  return price === 0 ? '나눔' : price.toLocaleString('ko-KR') + '원'
}

/* 월별 통계는 개별 상품가가 아니라 합계 금액이라, 0원을 "나눔"으로 표시하는 formatPrice의 관례를 쓰지 않는다. */
function formatAmount(amount: number) {
  return amount.toLocaleString('ko-KR') + '원'
}

function formatDate(iso: string) {
  return iso.slice(0, 10)
}

export default function TradeHistorySection() {
  const [tab, setTab] = useState<Tab>('SALES')
  const [status, setStatus] = useState<SectionStatus>('loading')
  const [sales, setSales] = useState<TradeSale[]>([])
  const [purchases, setPurchases] = useState<TradePurchase[]>([])
  const [stats, setStats] = useState<MonthlyStat[]>([])

  useEffect(() => {
    let cancelled = false
    Promise.all([
      apiFetch('/api/members/me/trades/sales').then(r => r.ok ? r.json() : null),
      apiFetch('/api/members/me/trades/purchases').then(r => r.ok ? r.json() : null),
      apiFetch('/api/members/me/trades/monthly-stats').then(r => r.ok ? r.json() : null),
    ]).then(([salesRes, purchasesRes, statsRes]) => {
      if (cancelled) return
      if (!salesRes || !purchasesRes || !statsRes) { setStatus('error'); return }
      setSales(salesRes.data ?? [])
      setPurchases(purchasesRes.data ?? [])
      setStats(statsRes.data ?? [])
      setStatus('ready')
    }).catch(() => {
      if (!cancelled) setStatus('error')
    })
    return () => { cancelled = true }
  }, [])

  return (
    <div className={styles.card}>
      <h2>거래내역</h2>
      <p className={styles.desc}>완료된 거래의 판매·구매 내역과 월별 통계를 확인할 수 있어요.</p>

      <div className={styles.tabs}>
        {TABS.map(t => (
          <button
            key={t.key}
            type="button"
            className={`${styles.tab}${tab === t.key ? ' ' + styles.on : ''}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {status === 'loading' && <p className={styles.msg}>불러오는 중...</p>}
      {status === 'error' && <p className={styles.msg}>거래내역을 불러오지 못했어요.</p>}

      {status === 'ready' && tab === 'SALES' && (
        sales.length === 0 ? (
          <p className={styles.msg}>아직 판매 완료한 내역이 없어요.</p>
        ) : (
          <div className={styles.list}>
            {sales.map(item => (
              <div key={item.productId} className={styles.item}>
                <div className={styles.thumb}>
                  {item.thumbnailUrl && <img src={item.thumbnailUrl} alt={item.title} />}
                </div>
                <div className={styles.itemBody}>
                  <div className={styles.itemTitle}>{item.title}</div>
                  <div className={styles.itemMeta}>{item.region} · {formatDate(item.completedAt)}</div>
                </div>
                <div className={styles.itemPrice}>{formatPrice(item.price)}</div>
              </div>
            ))}
          </div>
        )
      )}

      {status === 'ready' && tab === 'PURCHASES' && (
        purchases.length === 0 ? (
          <p className={styles.msg}>아직 구매 완료한 내역이 없어요.</p>
        ) : (
          <div className={styles.list}>
            {purchases.map(item => (
              <div key={item.productId} className={styles.item}>
                <div className={styles.thumb}>
                  {item.thumbnailUrl && <img src={item.thumbnailUrl} alt={item.title} />}
                </div>
                <div className={styles.itemBody}>
                  <div className={styles.itemTitle}>{item.title}</div>
                  <div className={styles.itemMeta}>{item.sellerNickname}님 · {formatDate(item.completedAt)}</div>
                </div>
                <div className={styles.itemPrice}>{formatPrice(item.price)}</div>
              </div>
            ))}
          </div>
        )
      )}

      {status === 'ready' && tab === 'STATS' && (
        stats.length === 0 ? (
          <p className={styles.msg}>아직 통계로 보여드릴 거래 내역이 없어요.</p>
        ) : (
          <div className={styles.statsTable}>
            <div className={styles.statsHeadRow}>
              <span>월</span>
              <span>판매</span>
              <span>구매</span>
            </div>
            {stats.map(row => (
              <div key={row.yearMonth} className={styles.statsRow}>
                <span className={styles.statsMonth}>{row.yearMonth}</span>
                <span className={styles.statsCell}>
                  {row.salesCount}건<small>{formatAmount(row.salesAmount)}</small>
                </span>
                <span className={styles.statsCell}>
                  {row.purchasesCount}건<small>{formatAmount(row.purchasesAmount)}</small>
                </span>
              </div>
            ))}
          </div>
        )
      )}
    </div>
  )
}
