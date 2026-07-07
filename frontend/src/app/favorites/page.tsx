'use client'

import Link from 'next/link'
import { useEffect, useRef, useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import { TRADE_STATUS_LABEL, type TradeStatus } from '@/lib/tradeStatus'
import styles from './page.module.css'

interface FavoriteProduct {
  favoriteId: number
  product: {
    productId: number
    title: string
    price: number
    region: string
    tradeStatus: TradeStatus
    thumbnailUrl: string | null
  }
}

type PageStatus = 'loading' | 'ready' | 'error'

function priceText(price: number) {
  return price === 0 ? '나눔' : price.toLocaleString('ko-KR') + '원'
}

function badgeInfo(product: FavoriteProduct['product']): { label: string; cls: string } | null {
  if (product.tradeStatus === 'RESERVED')  return { label: TRADE_STATUS_LABEL.RESERVED,  cls: styles.badgeReserved }
  if (product.tradeStatus === 'COMPLETED') return { label: TRADE_STATUS_LABEL.COMPLETED, cls: styles.badgeDone }
  if (product.price === 0)                 return { label: '나눔', cls: styles.badgeFree }
  return null
}

export default function FavoritesPage() {
  const [status, setStatus]       = useState<PageStatus>('loading')
  const [favorites, setFavorites] = useState<FavoriteProduct[]>([])

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
    let cancelled = false
    apiFetch('/api/members/me/favorites')
      .then(async r => {
        const data = await r.json().catch(() => null)
        if (!r.ok) throw new Error(data?.message ?? '관심 상품을 불러오지 못했습니다.')
        if (!cancelled) {
          setFavorites(data?.data ?? [])
          setStatus('ready')
        }
      })
      .catch(() => { if (!cancelled) setStatus('error') })
    return () => { cancelled = true }
  }, [])

  async function removeFavorite(e: React.MouseEvent, productId: number) {
    e.preventDefault()
    try {
      const res = await apiFetch(`/api/products/${productId}/favorites`, { method: 'DELETE' })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        showToast(data?.message ?? '해제 중 오류가 발생했습니다.')
        return
      }
      setFavorites(prev => prev.filter(f => f.product.productId !== productId))
      showToast('관심 상품에서 뺐어요')
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  return (
    <main className={styles.wrap}>
      {/* 페이지 헤더 */}
      <div className={styles.headRow}>
        <h1>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 20.5l-1.4-1.3C5.4 14.5 2 11.4 2 7.6 2 4.9 4.1 3 6.7 3c1.5 0 3 .7 3.9 1.9L12 6.3l1.4-1.4C14.3 3.7 15.8 3 17.3 3 19.9 3 22 4.9 22 7.6c0 3.8-3.4 6.9-8.6 11.6L12 20.5z" />
          </svg>
          관심 상품
        </h1>
        <p>찜해둔 상품 <b>{favorites.length}</b>개예요. 하트를 다시 누르면 관심에서 빠져요.</p>
      </div>

      {status === 'loading' && (
        <div className={styles.empty}><p>불러오는 중...</p></div>
      )}

      {status === 'error' && (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>⚠️</div>
          <p>관심 상품을 불러오지 못했어요.</p>
        </div>
      )}

      {status === 'ready' && (
        favorites.length > 0 ? (
          <div className={styles.grid}>
            {favorites.map(({ favoriteId, product }) => {
              const badge = badgeInfo(product)
              return (
                <article key={favoriteId} className={styles.pcard}>
                  {/* 썸네일 */}
                  <div className={styles.thumb}>
                    {badge && <span className={`${styles.badgeTag} ${badge.cls}`}>{badge.label}</span>}
                    {product.thumbnailUrl ? (
                      <img src={product.thumbnailUrl} alt={product.title} className={styles.thumbImg} />
                    ) : (
                      <span className={styles.ph}>상품 이미지</span>
                    )}
                    <button
                      className={styles.heart}
                      type="button"
                      aria-label="관심 해제"
                      onClick={e => removeFavorite(e, product.productId)}
                    >
                      <svg width="17" height="17" viewBox="0 0 24 24" strokeWidth="2">
                        <path d="M12 20.5l-1.4-1.3C5.4 14.5 2 11.4 2 7.6 2 4.9 4.1 3 6.7 3c1.5 0 3 .7 3.9 1.9L12 6.3l1.4-1.4C14.3 3.7 15.8 3 17.3 3 19.9 3 22 4.9 22 7.6c0 3.8-3.4 6.9-8.6 11.6L12 20.5z" />
                      </svg>
                    </button>
                  </div>

                  {/* 카드 바디 */}
                  <Link href={`/products/${product.productId}`} className={styles.body}>
                    <div className={styles.title}>{product.title}</div>
                    <div className={`${styles.price}${product.price === 0 ? ' ' + styles.priceFree : ''}`}>
                      {priceText(product.price)}
                    </div>
                    <div className={styles.meta}>
                      <span>{product.region}</span>
                    </div>
                  </Link>
                </article>
              )
            })}
          </div>
        ) : (
          <div className={styles.empty}>
            <div className={styles.emptyIcon}>🤍</div>
            <h2>아직 관심 상품이 없어요</h2>
            <p>마음에 드는 상품에 하트를 눌러 모아보세요.</p>
            <Link href="/products" className={styles.emptyBtn}>상품 둘러보기</Link>
          </div>
        )
      )}

      {/* 토스트 */}
      <div className={`toast${toastOn ? ' show' : ''}`}>{toastText}</div>
    </main>
  )
}
