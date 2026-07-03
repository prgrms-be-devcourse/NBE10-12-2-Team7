'use client'

import Link from 'next/link'
import { useParams, useRouter } from 'next/navigation'
import { useEffect, useRef, useState } from 'react'
import { getAccessToken, getCurrentMemberId } from '@/lib/auth'
import { TRADE_STATUS_LABEL, type TradeStatus } from '@/lib/tradeStatus'
import styles from './page.module.css'

interface Category { id: number; name: string }

interface Product {
  productId: number
  memberId: number
  categoryId: number
  title: string
  description: string
  price: number
  tradeStatus: TradeStatus
  region: string
  viewCount: number
  favoriteCount: number
  hidden: boolean
}

interface Comment {
  id: number
  memberId: number
  productId: number
  content: string
  createdAt: string
  updatedAt: string
}

interface MyFavorite {
  favoriteId: number
  createdAt: string
  product: { productId: number }
}

type PageStatus = 'loading' | 'ready' | 'error'

function statusCls(s: TradeStatus) {
  if (s === 'ON_SALE')   return styles.statusSale
  if (s === 'RESERVED')  return styles.statusReserved
  return styles.statusDone
}

function priceText(price: number) {
  return price === 0 ? '나눔' : price.toLocaleString('ko-KR') + '원'
}

export default function ProductDetailPage() {
  const { id }  = useParams<{ id: string }>()
  const router  = useRouter()

  const [status, setStatus]   = useState<PageStatus>('loading')
  const [errorMsg, setErrorMsg] = useState('')
  const [product, setProduct] = useState<Product | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [comments, setComments] = useState<Comment[]>([])

  const [selThumb, setSelThumb] = useState(0)
  const [favorited, setFavorited] = useState(false)
  const [selectVal, setSelectVal] = useState<TradeStatus>('ON_SALE')
  const [cInput, setCInput] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editContent, setEditContent] = useState('')

  const [toastText, setToastText] = useState('')
  const [toastOn,   setToastOn]   = useState(false)
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const myMemberId = getCurrentMemberId()
  const isOwner = !!product && myMemberId !== null && product.memberId === myMemberId

  function showToast(msg: string) {
    setToastText(msg); setToastOn(true)
    if (toastTimer.current) clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToastOn(false), 1900)
  }

  useEffect(() => {
    let cancelled = false
    Promise.all([
      fetch(`/api/products/${id}`).then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) })),
      fetch(`/api/products/${id}/comments`).then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) })),
      fetch('/api/categories').then(r => r.json()).catch(() => null),
    ]).then(([productRes, commentsRes, categoriesRes]) => {
      if (cancelled) return
      if (!productRes.ok) {
        setErrorMsg(productRes.data?.message ?? '상품을 불러오지 못했습니다.')
        setStatus('error')
        return
      }
      setProduct(productRes.data.data)
      setSelectVal(productRes.data.data.tradeStatus)
      setComments(commentsRes.ok ? (commentsRes.data?.data ?? []) : [])
      setCategories(categoriesRes?.data ?? [])
      setStatus('ready')
    }).catch(() => {
      if (!cancelled) { setErrorMsg('상품을 불러오지 못했습니다.'); setStatus('error') }
    })
    return () => { cancelled = true }
  }, [id])

  useEffect(() => {
    const token = getAccessToken()
    if (!token || !product) return
    fetch('/api/members/me/favorites', { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.ok ? r.json() : null)
      .then(data => {
        const list: MyFavorite[] = data?.data ?? []
        setFavorited(list.some(f => f.product.productId === product.productId))
      })
      .catch(() => {})
  }, [product])

  /* ── 관심 토글 ── */
  async function toggleFav() {
    if (!product) return
    const token = getAccessToken()
    if (!token) { showToast('로그인 후 이용할 수 있어요'); return }

    const next = !favorited
    try {
      const res = await fetch(`/api/products/${product.productId}/favorites`, {
        method: next ? 'POST' : 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        showToast(data?.message ?? '처리 중 오류가 발생했습니다.')
        return
      }
      setFavorited(next)
      setProduct(p => p ? { ...p, favoriteCount: p.favoriteCount + (next ? 1 : -1) } : p)
      showToast(next ? '관심 상품에 담았어요' : '관심을 해제했어요')
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  /* ── 거래 상태 변경 ── */
  async function applyStatus() {
    if (!product) return
    const token = getAccessToken()
    if (!token) return
    try {
      const res = await fetch(`/api/products/${product.productId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ tradeStatus: selectVal }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) { showToast(data?.message ?? '상태 변경 중 오류가 발생했습니다.'); return }
      setProduct(data.data)
      showToast(`거래 상태를 "${TRADE_STATUS_LABEL[selectVal]}"으로 변경했어요`)
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  /* ── 상품 삭제 ── */
  async function handleDelete() {
    if (!product) return
    if (!window.confirm('이 상품을 삭제할까요? 삭제 후 되돌릴 수 없어요.')) return
    const token = getAccessToken()
    if (!token) return
    try {
      const res = await fetch(`/api/products/${product.productId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        showToast(data?.message ?? '삭제 중 오류가 발생했습니다.')
        return
      }
      showToast('상품을 삭제했어요')
      setTimeout(() => { window.location.href = '/my-products' }, 900)
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  /* ── 댓글 ── */
  async function submitComment() {
    if (!product) return
    const v = cInput.trim()
    if (!v) { showToast('댓글 내용을 입력해주세요'); return }
    const token = getAccessToken()
    if (!token) { showToast('로그인 후 댓글을 작성할 수 있어요'); return }

    try {
      const res = await fetch(`/api/products/${product.productId}/comments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ content: v }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) { showToast(data?.message ?? '댓글 등록 중 오류가 발생했습니다.'); return }
      setComments(prev => [...prev, data.data])
      setCInput('')
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  function startEdit(c: Comment) {
    setEditingId(c.id)
    setEditContent(c.content)
  }

  function cancelEdit() {
    setEditingId(null)
    setEditContent('')
  }

  async function saveEdit(commentId: number) {
    const v = editContent.trim()
    if (!v) { showToast('댓글 내용을 입력해주세요'); return }
    const token = getAccessToken()
    if (!token) { showToast('로그인 후 이용할 수 있어요'); return }

    try {
      const res = await fetch(`/api/comments/${commentId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ content: v }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) { showToast(data?.message ?? '수정 중 오류가 발생했습니다.'); return }
      setComments(prev => prev.map(c => c.id === commentId ? data.data : c))
      cancelEdit()
      showToast('댓글을 수정했어요')
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  async function deleteComment(commentId: number) {
    const token = getAccessToken()
    if (!token) return
    try {
      const res = await fetch(`/api/comments/${commentId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        showToast(data?.message ?? '삭제 중 오류가 발생했습니다.')
        return
      }
      setComments(prev => prev.filter(c => c.id !== commentId))
      showToast('댓글을 삭제했어요')
    } catch {
      showToast('서버에 연결할 수 없습니다.')
    }
  }

  if (status === 'loading') {
    return <main className={styles.wrap}><div className={styles.notice}><p>불러오는 중...</p></div></main>
  }
  if (status === 'error' || !product) {
    return (
      <main className={styles.wrap}>
        <div className={styles.notice}>
          <p>{errorMsg || '상품을 불러오지 못했습니다.'}</p>
          <Link href="/products" className={styles.btnGhost}>목록으로</Link>
        </div>
      </main>
    )
  }

  const categoryName = categories.find(c => c.id === product.categoryId)?.name ?? `카테고리 #${product.categoryId}`

  return (
    <main className={styles.wrap}>
      {/* 브레드크럼 */}
      <div className={styles.crumb}>
        <Link href="/products">상품목록</Link>
        <span className={styles.crumbSep}>›</span>
        <span>{categoryName}</span>
        <span className={styles.crumbSep}>›</span>
        <span>{product.title}</span>
      </div>

      {/* 갤러리 + 정보 */}
      <div className={styles.top}>
        {/* 갤러리 */}
        <div className={styles.gallery}>
          <div className={styles.photo}>
            <span className={`${styles.statusBadge} ${statusCls(product.tradeStatus)}`}>
              {TRADE_STATUS_LABEL[product.tradeStatus]}
            </span>
            <span className={styles.photoPh}>상품 이미지</span>
          </div>
          <div className={styles.thumbs}>
            {Array.from({ length: 4 }, (_, i) => (
              <div
                key={i}
                className={`${styles.thumb}${selThumb === i ? ' ' + styles.sel : ''}`}
                onClick={() => setSelThumb(i)}
              />
            ))}
          </div>
        </div>

        {/* 상품 정보 */}
        <div className={styles.info}>
          <h1>{product.title}</h1>
          <div className={styles.price}>{priceText(product.price)}</div>
          <div className={styles.meta}>
            <span>{categoryName}</span>
            <span className={styles.metaDot} />
            <span>{product.region}</span>
          </div>

          {/* 판매자 */}
          <div className={styles.seller}>
            <div className={styles.sellerAvatar}>회</div>
            <div className={styles.sellerWho}>
              <div className={styles.sellerNm}>회원 #{product.memberId}</div>
            </div>
            {!isOwner && (
              <button
                type="button"
                className={styles.sellerReport}
                aria-label="사용자 신고"
                onClick={() => router.push(`/report?type=member&id=${product.memberId}`)}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M4 22V4h13l-2 4 2 4H4" />
                </svg>
                신고
              </button>
            )}
          </div>

          {/* 통계 */}
          <div className={styles.statsRow}>
            <div className={styles.stat}>
              <div className={styles.statV}>{product.favoriteCount}</div>
              <div className={styles.statL}>관심</div>
            </div>
            <div className={styles.stat}>
              <div className={styles.statV}>{product.viewCount}</div>
              <div className={styles.statL}>조회</div>
            </div>
          </div>

          {/* 액션 */}
          <div className={styles.actions}>
            <button
              type="button"
              className={`${styles.btnFav}${favorited ? ' ' + styles.on : ''}`}
              onClick={toggleFav}
              aria-pressed={favorited}
              aria-label="관심 상품 토글"
            >
              <svg className={styles.heart} width="19" height="19" viewBox="0 0 24 24" strokeWidth="2">
                <path d="M12 20.5l-1.4-1.3C5.4 14.5 2 11.4 2 7.6 2 4.9 4.1 3 6.7 3c1.5 0 3 .7 3.9 1.9L12 6.3l1.4-1.4C14.3 3.7 15.8 3 17.3 3 19.9 3 22 4.9 22 7.6c0 3.8-3.4 6.9-8.6 11.6L12 20.5z" />
              </svg>
            </button>
            <button type="button" className={styles.btnPrimary} onClick={() => showToast('채팅 기능은 준비 중이에요')}>
              채팅으로 거래하기
            </button>
            <button
              type="button"
              className={styles.btnReport}
              aria-label="상품 신고"
              onClick={() => router.push(`/report?type=product&id=${id}`)}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M4 22V4h13l-2 4 2 4H4" />
              </svg>
            </button>
          </div>
          <div className={styles.apiNote}>
            GET /api/products/&#123;productId&#125; · POST · DELETE /api/products/&#123;productId&#125;/favorites
          </div>
        </div>
      </div>

      {/* 상품 정보 섹션 */}
      <div className={styles.section}>
        <h2>상품 정보</h2>
        <p className={styles.desc}>{product.description}</p>
      </div>

      {/* 판매자 전용 패널 */}
      {isOwner && (
        <div className={styles.section}>
          <div className={styles.panel}>
            <div className={styles.ownerNote}>🔒 판매자 본인에게만 보여요</div>
            <div className={styles.ownerRow}>
              <Link href={`/products/${id}/edit`} className={styles.btnGhost}>상품 수정</Link>
              <button type="button" className={styles.btnDanger} onClick={handleDelete}>상품 삭제</button>
              <span className={styles.grow} />
              <select
                className={styles.statusSelect}
                aria-label="거래 상태 변경"
                value={selectVal}
                onChange={e => setSelectVal(e.target.value as TradeStatus)}
              >
                {(Object.keys(TRADE_STATUS_LABEL) as TradeStatus[]).map(s => (
                  <option key={s} value={s}>{TRADE_STATUS_LABEL[s]}</option>
                ))}
              </select>
              <button type="button" className={styles.btnStatus} onClick={applyStatus}>상태 변경</button>
            </div>
            <div className={styles.apiNote}>
              PATCH /api/products/&#123;productId&#125; · PATCH .../status · DELETE /api/products/&#123;productId&#125;
            </div>
          </div>
        </div>
      )}

      {/* 댓글 섹션 */}
      <div className={styles.section}>
        <h2>
          댓글 <span className={styles.cCountAccent}>{comments.length}</span>
        </h2>
        {getAccessToken() ? (
          <div className={styles.cbox}>
            <textarea
              className={styles.cTextarea}
              placeholder="궁금한 점을 댓글로 남겨보세요."
              value={cInput}
              onChange={e => setCInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), submitComment())}
            />
            <button type="button" className={styles.cSubmitBtn} onClick={submitComment}>등록</button>
          </div>
        ) : (
          <p className={styles.loginNotice}>로그인 후 댓글을 작성할 수 있어요.</p>
        )}
        <div>
          {comments.map(c => {
            const isMine = myMemberId !== null && c.memberId === myMemberId
            const isSeller = c.memberId === product.memberId
            const who = isMine ? '나' : `회원 #${c.memberId}`
            return (
              <div key={c.id} className={styles.comment}>
                <div className={`${styles.cAvatar}${isMine || isSeller ? ' ' + styles.cAvatarMine : ''}`}>
                  {who.charAt(0)}
                </div>
                <div className={styles.cBody}>
                  <div className={styles.cHead}>
                    <span className={styles.cWho}>{who}</span>
                    {isSeller && <span className={styles.cSeller}>판매자</span>}
                    {isMine   && <span className={styles.cMine}>내 댓글</span>}
                    <span className={styles.cWhen}>{c.createdAt.slice(0, 10)}</span>
                  </div>
                  {editingId === c.id ? (
                    <div className={styles.cEditBox}>
                      <textarea
                        className={styles.cTextarea}
                        value={editContent}
                        onChange={e => setEditContent(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), saveEdit(c.id))}
                      />
                      <div className={styles.cCtrl}>
                        <button type="button" onClick={() => saveEdit(c.id)}>저장</button>
                        <button type="button" onClick={cancelEdit}>취소</button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className={styles.cText}>{c.content}</div>
                      {isMine && (
                        <div className={styles.cCtrl}>
                          <button type="button" onClick={() => startEdit(c)}>수정</button>
                          <button type="button" onClick={() => deleteComment(c.id)}>삭제</button>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            )
          })}
        </div>
        <div className={styles.apiNote}>
          GET · POST /api/products/&#123;productId&#125;/comments · PATCH · DELETE /api/comments/&#123;commentId&#125;
        </div>
      </div>

      {/* 토스트 */}
      <div className={`toast${toastOn ? ' show' : ''}`}>{toastText}</div>
    </main>
  )
}
