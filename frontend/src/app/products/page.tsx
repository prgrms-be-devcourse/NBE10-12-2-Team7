'use client'

import Link from 'next/link'
import { useEffect, useMemo, useState } from 'react'
import { TRADE_STATUS_LABEL, type TradeStatus } from '@/lib/tradeStatus'
import styles from './page.module.css'

interface Category {
  id: number
  name: string
}

interface Product {
  productId: number
  memberId: number
  categoryId: number
  title: string
  price: number
  tradeStatus: TradeStatus
  region: string
  viewCount: number
  favoriteCount: number
  thumbnailUrl: string | null
  hidden: boolean
}

type PageStatus = 'loading' | 'ready' | 'error'

const SORT_OPTIONS = [
  { value: 'latest',     label: '최신순' },
  { value: 'likes',      label: '관심 많은순' },
  { value: 'price_asc',  label: '낮은 가격순' },
  { value: 'price_desc', label: '높은 가격순' },
]

function priceText(price: number) {
  return price === 0 ? '나눔' : price.toLocaleString('ko-KR') + '원'
}

function badgeInfo(product: Product): { label: string; cls: string } | null {
  if (product.tradeStatus === 'RESERVED')  return { label: TRADE_STATUS_LABEL.RESERVED,  cls: styles.badgeReserved }
  if (product.tradeStatus === 'COMPLETED') return { label: TRADE_STATUS_LABEL.COMPLETED, cls: styles.badgeDone }
  if (product.price === 0)                 return { label: '나눔', cls: styles.badgeFree }
  return null
}

export default function ProductsPage() {
  const [status, setStatus] = useState<PageStatus>('loading')
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])

  const [search,     setSearch]     = useState('')
  const [query,      setQuery]      = useState('')
  const [categoryId, setCategoryId] = useState<number | 'all'>('all')
  const [sort,       setSort]       = useState('latest')

  useEffect(() => {
    Promise.all([
      fetch('/api/products').then(r => r.json()),
      fetch('/api/categories').then(r => r.json()),
    ])
      .then(([productsRes, categoriesRes]) => {
        setProducts(productsRes?.data ?? [])
        setCategories(categoriesRes?.data ?? [])
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [])

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    setQuery(search.trim())
  }

  const filtered = useMemo(() => {
    let list = products
    if (categoryId !== 'all') list = list.filter(p => p.categoryId === categoryId)
    if (query) {
      const q = query.toLowerCase()
      list = list.filter(p => p.title.toLowerCase().includes(q) || p.region.includes(query))
    }
    switch (sort) {
      case 'likes':      list = [...list].sort((a, b) => b.favoriteCount - a.favoriteCount); break
      case 'price_asc':  list = [...list].sort((a, b) => a.price - b.price); break
      case 'price_desc': list = [...list].sort((a, b) => b.price - a.price); break
    }
    return list
  }, [products, categoryId, query, sort])

  return (
    <main className={styles.wrap}>
      {/* 페이지 헤더 */}
      <div className={styles.headRow}>
        <div>
          <h1>상품 목록</h1>
          <p>우리 동네 중고 거래 <b>{filtered.length}개</b>의 상품이 있어요.</p>
        </div>
      </div>

      {/* 검색 바 */}
      <form className={styles.searchBar} onSubmit={handleSearch}>
        <input
          type="search"
          className={styles.searchInput}
          placeholder="상품명 또는 지역으로 검색"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <button type="submit" className={styles.searchBtn}>검색</button>
      </form>

      {/* 카테고리 탭 + 정렬 */}
      <div className={styles.filterRow}>
        <div className={styles.tabs}>
          <button
            type="button"
            className={`${styles.tab}${categoryId === 'all' ? ' ' + styles.on : ''}`}
            onClick={() => setCategoryId('all')}
          >
            전체
          </button>
          {categories.map(cat => (
            <button
              key={cat.id}
              type="button"
              className={`${styles.tab}${categoryId === cat.id ? ' ' + styles.on : ''}`}
              onClick={() => setCategoryId(cat.id)}
            >
              {cat.name}
            </button>
          ))}
        </div>
        <select
          className={styles.sortSelect}
          value={sort}
          onChange={e => setSort(e.target.value)}
          aria-label="정렬 기준"
        >
          {SORT_OPTIONS.map(o => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      </div>

      {status === 'loading' && (
        <div className={styles.empty}><p>불러오는 중...</p></div>
      )}

      {status === 'error' && (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>⚠️</div>
          <h2>상품 목록을 불러오지 못했어요</h2>
          <p>잠시 후 다시 시도해주세요.</p>
        </div>
      )}

      {status === 'ready' && (
        filtered.length > 0 ? (
          <div className={styles.grid}>
            {filtered.map(product => {
              const badge = badgeInfo(product)
              return (
                <Link key={product.productId} href={`/products/${product.productId}`} className={styles.pcard}>
                  {/* 썸네일 */}
                  <div className={styles.thumb}>
                    {badge && <span className={`${styles.badgeTag} ${badge.cls}`}>{badge.label}</span>}
                    {product.thumbnailUrl ? (
                      <img src={product.thumbnailUrl} alt={product.title} className={styles.thumbImg} />
                    ) : (
                      <span className={styles.thumbPh}>상품 이미지</span>
                    )}
                  </div>

                  {/* 바디 */}
                  <div className={styles.body}>
                    <div className={styles.title}>{product.title}</div>
                    <div className={`${styles.price}${product.price === 0 ? ' ' + styles.priceFree : ''}`}>
                      {priceText(product.price)}
                    </div>
                    <div className={styles.meta}>
                      <span>{product.region}</span>
                    </div>
                    <div className={styles.foot}>
                      <span>♡ {product.favoriteCount}</span>
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>
        ) : (
          <div className={styles.empty}>
            <div className={styles.emptyIcon}>🔍</div>
            <h2>검색 결과가 없어요</h2>
            <p>다른 검색어나 카테고리를 시도해보세요.</p>
          </div>
        )
      )}

      <div className={styles.apiNote}>GET /api/products · GET /api/categories</div>
    </main>
  )
}
