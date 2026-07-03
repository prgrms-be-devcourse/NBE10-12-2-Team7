'use client'

import Link from 'next/link'
import { useEffect, useRef, useState } from 'react'
import { getAccessToken } from '@/lib/auth'
import { TRADE_STATUS_LABEL, type TradeStatus } from '@/lib/tradeStatus'
import styles from './page.module.css'

type FilterTab = '전체' | TradeStatus
type PageStatus = 'loading' | 'ready' | 'unauthenticated' | 'error'

interface MyProduct {
  productId: number
  memberId: number
  categoryId: number
  title: string
  price: number
  tradeStatus: TradeStatus
  region: string
  viewCount: number
  favoriteCount: number
  hidden: boolean
}

const TABS: FilterTab[] = ['전체', 'ON_SALE', 'RESERVED', 'COMPLETED']

function priceText(price: number) {
  return price === 0 ? '나눔' : price.toLocaleString('ko-KR') + '원'
}

function stBadgeCls(status: TradeStatus) {
  if (status === 'ON_SALE')  return styles.stSale
  if (status === 'RESERVED') return styles.stReserved
  return styles.stDone
}

export default function MyProductsPage() {
  const [status, setStatus]     = useState<PageStatus>('loading')
  const [products, setProducts] = useState<MyProduct[]>([])
  const [filter,   setFilter]   = useState<FilterTab>('전체')

  const [toastText, setToastText] = useState('')
  const [toastOn,   setToastOn]   = useState(false)
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  function showToast(msg: string) {
    setToastText(msg)
    setToastOn(true)
    if (toastTimer.current) clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToastOn(false), 1900)
  }

  useEffect(() => {
    const token = getAccessToken()
    if (!token) { setStatus('unauthenticated'); return }

    let cancelled = false
    fetch('/api/products/me', { headers: { Authorization: `Bearer ${token}` } })
      .then(async r => {
        if (r.status === 401) { if (!cancelled) setStatus('unauthenticated'); return }
        const data = await r.json().catch(() => null)
        if (!r.ok) throw new Error(data?.message ?? '내 상품을 불러오지 못했습니다.')
        if (!cancelled) {
          setProducts(data?.data ?? [])
          setStatus('ready')
        }
      })
      .catch(() => { if (!cancelled) setStatus('error') })
    return () => { cancelled = true }
  }, [])

  async function changeStatus(productId: number, newStatus: TradeStatus) {
    const token = getAccessToken()
    if (!token) return
    const product = products.find(p => p.productId === productId)
    try {
      const res = await fetch(`/api/products/${productId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ tradeStatus: newStatus }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) { showToast(data?.message ?? '상태 변경 중 오류가 발생했습니다.'); return }
      setProducts(prev => prev.map(p => p.productId === productId ? { ...p, tradeStatus: newStatus } : p))
      if (product) showToast(`"${product.title}" 상태를 ${TRADE_STATUS_LABEL[newStatus]}(으)로 변경했어요`)
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  async function deleteProduct(productId: number, title: string) {
    if (!window.confirm(`"${title}" 상품을 삭제할까요?`)) return
    const token = getAccessToken()
    if (!token) return
    try {
      const res = await fetch(`/api/products/${productId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        showToast(data?.message ?? '삭제 중 오류가 발생했습니다.')
        return
      }
      setProducts(prev => prev.filter(p => p.productId !== productId))
      showToast('상품을 삭제했어요')
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  const filtered = filter === '전체' ? products : products.filter(p => p.tradeStatus === filter)

  const counts = {
    total:    products.length,
    sale:     products.filter(p => p.tradeStatus === 'ON_SALE').length,
    reserved: products.filter(p => p.tradeStatus === 'RESERVED').length,
    done:     products.filter(p => p.tradeStatus === 'COMPLETED').length,
  }

  return (
    <main className={styles.wrap}>
      {/* 페이지 헤더 */}
      <div className={styles.headRow}>
        <div>
          <h1>내 상품 목록</h1>
          <p>내가 등록한 상품을 관리하고 거래 상태를 변경할 수 있어요.</p>
        </div>
      </div>

      {status === 'unauthenticated' && (
        <div className={styles.empty}>
          <p>로그인 후 내 상품을 확인할 수 있어요.</p>
          <Link href="/login" className={styles.emptyBtn}>로그인하기</Link>
        </div>
      )}

      {status === 'loading' && (
        <div className={styles.empty}><p>불러오는 중...</p></div>
      )}

      {status === 'error' && (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>⚠️</div>
          <p>상품 목록을 불러오지 못했어요.</p>
        </div>
      )}

      {status === 'ready' && (
        <>
          {/* 요약 통계 */}
          <div className={styles.summary}>
            <div className={styles.stat}>
              <div className={styles.statV}>{counts.total}</div>
              <div className={styles.statL}>전체 상품</div>
            </div>
            <div className={styles.stat}>
              <div className={styles.statV}>{counts.sale}</div>
              <div className={styles.statL}>판매중</div>
            </div>
            <div className={styles.stat}>
              <div className={styles.statV}>{counts.reserved}</div>
              <div className={styles.statL}>예약중</div>
            </div>
            <div className={styles.stat}>
              <div className={styles.statV}>{counts.done}</div>
              <div className={styles.statL}>거래완료</div>
            </div>
          </div>

          {/* 탭 필터 */}
          <div className={styles.tabs}>
            {TABS.map(tab => (
              <button
                key={tab}
                type="button"
                className={`${styles.tab}${filter === tab ? ' ' + styles.on : ''}`}
                onClick={() => setFilter(tab)}
              >
                {tab === '전체' ? '전체' : TRADE_STATUS_LABEL[tab]}
              </button>
            ))}
          </div>

          {/* 상품 목록 */}
          {filtered.length > 0 ? (
            <div className={styles.grid}>
              {filtered.map(product => (
                <div key={product.productId} className={styles.pcard}>
                  {/* 썸네일 */}
                  <div className={styles.thumb}>
                    <span className={`${styles.stBadge} ${stBadgeCls(product.tradeStatus)}`}>
                      {TRADE_STATUS_LABEL[product.tradeStatus]}
                    </span>
                  </div>

                  {/* 바디 */}
                  <div className={styles.body}>
                    <Link href={`/products/${product.productId}`} className={styles.title}>
                      {product.title}
                    </Link>
                    <div className={`${styles.price}${product.price === 0 ? ' ' + styles.priceFree : ''}`}>
                      {priceText(product.price)}
                    </div>
                    <div className={styles.meta}>
                      <span>{product.region}</span>
                      <span className={styles.dot} />
                      <span>♡ {product.favoriteCount}</span>
                      <span className={styles.dot} />
                      <span>👁 {product.viewCount}</span>
                    </div>

                    {/* 컨트롤 */}
                    <div className={styles.ctrl}>
                      <select
                        className={`${styles.mini} ${styles.miniSelect}`}
                        aria-label="거래 상태 변경"
                        value={product.tradeStatus}
                        onChange={e => changeStatus(product.productId, e.target.value as TradeStatus)}
                      >
                        {(Object.keys(TRADE_STATUS_LABEL) as TradeStatus[]).map(s => (
                          <option key={s} value={s}>{TRADE_STATUS_LABEL[s]}</option>
                        ))}
                      </select>
                      <span className={styles.grow} />
                      <Link href={`/products/${product.productId}/edit`} className={styles.mini}>
                        수정
                      </Link>
                      <button
                        type="button"
                        className={`${styles.mini} ${styles.miniDel}`}
                        onClick={() => deleteProduct(product.productId, product.title)}
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className={styles.empty}>
              <div className={styles.emptyIcon}>🧺</div>
              <p>해당 상태의 상품이 없어요.</p>
              <Link href="/products/new" className={styles.emptyBtn}>상품 등록하기</Link>
            </div>
          )}
        </>
      )}

      <div className={styles.apiNote}>
        GET /api/products/me · PATCH .../status · DELETE /api/products/&#123;id&#125;
      </div>

      {/* 토스트 */}
      <div className={`toast${toastOn ? ' show' : ''}`}>{toastText}</div>
    </main>
  )
}
