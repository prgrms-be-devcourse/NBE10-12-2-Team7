'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { apiFetch, bootstrapAutoLogin } from '@/lib/apiClient'
import { getAccessToken, getCurrentMemberId } from '@/lib/auth'
import styles from './ProductForm.module.css'

interface Category { id: number; name: string }
interface Region { regionId: number; name: string }

interface Props {
  editId?: string
}

type LoadStatus = 'loading' | 'ready' | 'unauthenticated' | 'forbidden' | 'error'

export default function ProductForm({ editId }: Props) {
  const isEdit = !!editId

  const [loadStatus, setLoadStatus] = useState<LoadStatus>('loading')
  const [categories, setCategories] = useState<Category[]>([])
  const [regions, setRegions] = useState<Region[]>([])

  /* ── 폼 상태 ── */
  const [title,      setTitle]      = useState('')
  const [categoryId, setCategoryId] = useState<number | ''>('')
  const [region,     setRegion]     = useState('')
  const [price,      setPrice]      = useState('')
  const [isFree,     setIsFree]     = useState(false)
  const [desc,       setDesc]       = useState('')
  const [images,         setImages]         = useState<string[]>([''])
  const [thumbnailIndex, setThumbnailIndex] = useState(0)

  /* ── 힌트 ── */
  const [titleHint,    setTitleHint]    = useState<{ text: string; err?: boolean }>({ text: '판매할 상품의 이름을 구체적으로 적어주세요.' })
  const [categoryHint, setCategoryHint] = useState<{ text: string; err?: boolean }>({ text: '' })
  const [regionHint,   setRegionHint]   = useState<{ text: string; err?: boolean }>({ text: '' })
  const [priceHint,    setPriceHint]    = useState<{ text: string; err?: boolean }>({ text: '' })
  const [descHint,     setDescHint]     = useState<{ text: string; err?: boolean }>({ text: '구매자가 궁금해할 정보를 상세히 적을수록 거래가 빨라져요.' })
  const [imagesHint,   setImagesHint]   = useState<{ text: string; err?: boolean }>({ text: '최소 1장, 최대 5장까지 이미지 URL을 등록할 수 있어요.' })

  /* ── 메시지 ── */
  const [formMsg,    setFormMsg]    = useState<{ text: string; type: 'success' | 'error' } | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false

    async function init() {
      // accessToken이 없어도 refreshToken이 남아있으면 재발급을 먼저 시도한다(자동 로그인).
      if (!getAccessToken()) await bootstrapAutoLogin()
      if (cancelled) return
      if (!getAccessToken()) { setLoadStatus('unauthenticated'); return }
      await loadFormData()
    }

    async function loadFormData() {
      const requests: Promise<unknown>[] = [
        fetch('/api/categories').then(r => r.json()),
        fetch('/api/regions').then(r => r.json()),
      ]
      if (isEdit) requests.push(fetch(`/api/products/${editId}`).then(async r => ({ ok: r.ok, data: await r.json().catch(() => null) })))

      await Promise.all(requests).then(results => {
        if (cancelled) return
        const categoriesRes = results[0] as { data?: Category[] }
        setCategories(categoriesRes?.data ?? [])
        const regionsRes = results[1] as { data?: Region[] }
        setRegions(regionsRes?.data ?? [])

        if (isEdit) {
          const productRes = results[2] as { ok: boolean; data: { data?: {
            memberId: number; categoryId: number; title: string; description: string; price: number; region: string
            thumbnailUrl?: string; imageUrls?: string[]
          } } }
          if (!productRes.ok || !productRes.data?.data) {
            setLoadStatus('error')
            return
          }
          const product = productRes.data.data
          const myId = getCurrentMemberId()
          if (myId === null || product.memberId !== myId) {
            setLoadStatus('forbidden')
            return
          }
          setTitle(product.title)
          setCategoryId(product.categoryId)
          setRegion(product.region)
          setPrice(product.price === 0 ? '' : product.price.toLocaleString('ko-KR'))
          setIsFree(product.price === 0)
          setDesc(product.description)
          const fetchedImages = product.imageUrls && product.imageUrls.length > 0 ? product.imageUrls : ['']
          setImages(fetchedImages)
          const thumbIdx = product.thumbnailUrl ? fetchedImages.indexOf(product.thumbnailUrl) : 0
          setThumbnailIndex(thumbIdx === -1 ? 0 : thumbIdx)
        }
        setLoadStatus('ready')
      }).catch(() => { if (!cancelled) setLoadStatus('error') })
    }

    init()
    return () => { cancelled = true }
  }, [isEdit, editId])

  /* ── 가격 콤마 포맷 ── */
  function handlePriceInput(e: React.ChangeEvent<HTMLInputElement>) {
    const digits = e.target.value.replace(/[^\d]/g, '')
    setPrice(digits ? Number(digits).toLocaleString('ko-KR') : '')
  }

  /* ── 나눔 체크 ── */
  function handleFreeChange(checked: boolean) {
    setIsFree(checked)
    if (checked) setPrice('0')
    else setPrice('')
  }

  /* ── 이미지 URL 목록 ── */
  function updateImage(index: number, value: string) {
    setImages(prev => prev.map((v, i) => i === index ? value : v))
  }
  function addImageField() {
    setImages(prev => prev.length >= 5 ? prev : [...prev, ''])
  }
  function removeImageField(index: number) {
    setImages(prev => {
      const next = prev.filter((_, i) => i !== index)
      return next.length > 0 ? next : ['']
    })
    setThumbnailIndex(prev => {
      if (prev === index) return 0
      return prev > index ? prev - 1 : prev
    })
  }

  /* ── 검증 ── */
  function getValidImages() {
    const trimmed = images.map(v => v.trim())
    const nonBlankIndices = trimmed
      .map((v, i) => (v ? i : -1))
      .filter(i => i !== -1)
    return { trimmed, nonBlankIndices }
  }

  function validate() {
    let ok = true
    if (!title.trim()) {
      setTitleHint({ text: '상품명을 입력하세요.', err: true }); ok = false
    } else {
      setTitleHint({ text: '판매할 상품의 이름을 구체적으로 적어주세요.' })
    }
    if (!categoryId) {
      setCategoryHint({ text: '카테고리를 선택하세요.', err: true }); ok = false
    } else {
      setCategoryHint({ text: '' })
    }
    if (!region.trim()) {
      setRegionHint({ text: '거래 지역을 선택하세요.', err: true }); ok = false
    } else {
      setRegionHint({ text: '' })
    }
    if (!isFree && !price.trim()) {
      setPriceHint({ text: '가격을 입력하거나 나눔을 선택하세요.', err: true }); ok = false
    } else {
      setPriceHint({ text: '' })
    }
    if (!desc.trim()) {
      setDescHint({ text: '상품 설명을 입력하세요.', err: true }); ok = false
    } else {
      setDescHint({ text: '구매자가 궁금해할 정보를 상세히 적을수록 거래가 빨라져요.' })
    }
    const { nonBlankIndices } = getValidImages()
    if (nonBlankIndices.length === 0) {
      setImagesHint({ text: '이미지 URL을 최소 1장 입력하세요.', err: true }); ok = false
    } else {
      setImagesHint({ text: '최소 1장, 최대 5장까지 이미지 URL을 등록할 수 있어요.' })
    }
    return ok
  }

  /* ── 제출 ── */
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormMsg(null)
    if (!validate()) {
      setFormMsg({ text: '입력값을 다시 확인해주세요.', type: 'error' })
      return
    }

    const { trimmed, nonBlankIndices } = getValidImages()
    const imageUrls = nonBlankIndices.map(i => trimmed[i])
    const payloadThumbnailIndex = Math.max(0, nonBlankIndices.indexOf(thumbnailIndex))

    const payload = {
      categoryId,
      title: title.trim(),
      description: desc.trim(),
      price: isFree ? 0 : Number(price.replace(/[^\d]/g, '')),
      region: region.trim(),
      imageUrls,
      thumbnailIndex: payloadThumbnailIndex,
    }
    const url    = isEdit ? `/api/products/${editId}` : '/api/products'
    const method = isEdit ? 'PATCH' : 'POST'

    setSubmitting(true)
    try {
      const res = await apiFetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) {
        setFormMsg({ text: data?.message ?? (isEdit ? '수정 중 오류가 발생했습니다.' : '등록 중 오류가 발생했습니다.'), type: 'error' })
        setSubmitting(false)
        return
      }
      setFormMsg({ text: isEdit ? '상품을 수정했어요. 이동합니다.' : '상품을 등록했어요. 이동합니다.', type: 'success' })
      const goId = data?.data?.productId ?? editId
      setTimeout(() => { window.location.href = goId ? `/products/${goId}` : '/products' }, 1000)
    } catch {
      setFormMsg({ text: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
      setSubmitting(false)
    }
  }

  const hintCls = (h: { err?: boolean }) =>
    h.err ? `${styles.hint} ${styles.err}` : styles.hint

  const msgCls = formMsg
    ? [styles.formMsg, styles.show, styles[formMsg.type]].join(' ')
    : styles.formMsg

  const introBlock = (
    <div className={styles.intro}>
      <span className={styles.badge}>{isEdit ? '✏️ 상품 수정' : '🛍️ 새 상품 등록'}</span>
      <h1>{isEdit ? '상품 수정' : '상품 등록'}</h1>
      <p>{isEdit ? '등록한 상품의 정보를 수정하세요.' : '우리 동네 이웃에게 판매할 상품을 올려보세요.'}</p>
    </div>
  )

  if (loadStatus === 'loading') {
    return <main className={styles.wrap}>{introBlock}<div className={styles.notice}><p>불러오는 중...</p></div></main>
  }
  if (loadStatus === 'unauthenticated') {
    return (
      <main className={styles.wrap}>
        {introBlock}
        <div className={styles.notice}>
          <p>로그인 후 이용할 수 있어요.</p>
          <Link href="/login" className={`btn ${styles.noticeBtn}`}>로그인하기</Link>
        </div>
      </main>
    )
  }
  if (loadStatus === 'forbidden') {
    return (
      <main className={styles.wrap}>
        {introBlock}
        <div className={styles.notice}><p>본인이 등록한 상품만 수정할 수 있어요.</p></div>
      </main>
    )
  }
  if (loadStatus === 'error') {
    return (
      <main className={styles.wrap}>
        {introBlock}
        <div className={styles.notice}><p>정보를 불러오지 못했습니다.</p></div>
      </main>
    )
  }

  return (
    <main className={styles.wrap}>
      {/* 인트로 */}
      {introBlock}

      <div className={styles.card}>
        <div className={msgCls} role="alert">{formMsg?.text}</div>

        <form onSubmit={handleSubmit} noValidate>
          {/* 상품 이미지 */}
          <div className={styles.field}>
            <label>상품 이미지<span className={styles.req}>*</span><span className={styles.sub}>URL 최대 5장 · 대표 이미지 선택</span></label>
            <div className={styles.imageList}>
              {images.map((url, i) => (
                <div key={i} className={styles.imageRow}>
                  <div className={styles.imagePreview}>
                    {url.trim() ? <img src={url.trim()} alt={`상품 이미지 ${i + 1}`} /> : <span className={styles.imagePreviewPh}>{i + 1}</span>}
                  </div>
                  <input
                    type="text" className={styles.input}
                    placeholder="https://example.com/image.jpg"
                    value={url}
                    onChange={e => updateImage(i, e.target.value)}
                  />
                  <button
                    type="button"
                    className={`${styles.thumbBtn}${thumbnailIndex === i ? ' ' + styles.thumbBtnOn : ''}`}
                    onClick={() => setThumbnailIndex(i)}
                  >
                    {thumbnailIndex === i ? '대표' : '대표로'}
                  </button>
                  <button
                    type="button"
                    className={styles.imageRmBtn}
                    aria-label="이미지 삭제"
                    onClick={() => removeImageField(i)}
                  >✕</button>
                </div>
              ))}
            </div>
            {images.length < 5 && (
              <button type="button" className={styles.addImageBtn} onClick={addImageField}>
                + 이미지 URL 추가
              </button>
            )}
            <div className={hintCls(imagesHint)}>{imagesHint.text}</div>
          </div>

          {/* 상품명 */}
          <div className={styles.field}>
            <label htmlFor="title">상품명<span className={styles.req}>*</span></label>
            <input
              id="title" type="text" className={styles.input}
              placeholder="상품명을 입력하세요" maxLength={40}
              value={title} onChange={e => setTitle(e.target.value)}
              aria-invalid={titleHint.err ? 'true' : 'false'}
            />
            <div className={hintCls(titleHint)}>{titleHint.text}</div>
          </div>

          {/* 카테고리 + 지역 */}
          <div className={styles.row2}>
            <div className={styles.field}>
              <label htmlFor="category">카테고리<span className={styles.req}>*</span></label>
              <select
                id="category" className={styles.select}
                value={categoryId} onChange={e => setCategoryId(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">카테고리 선택</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
              <div className={hintCls(categoryHint)}>{categoryHint.text}</div>
            </div>
            <div className={styles.field}>
              <label htmlFor="region">거래 지역<span className={styles.req}>*</span></label>
              <select
                id="region" className={styles.select}
                value={region} onChange={e => setRegion(e.target.value)}
                aria-invalid={regionHint.err ? 'true' : 'false'}
              >
                <option value="">지역 선택</option>
                {region && !regions.some(r => r.name === region) && (
                  <option value={region}>{region}</option>
                )}
                {regions.map(r => <option key={r.regionId} value={r.name}>{r.name}</option>)}
              </select>
              <div className={hintCls(regionHint)}>{regionHint.text}</div>
            </div>
          </div>

          {/* 가격 */}
          <div className={styles.field}>
            <label htmlFor="price">가격<span className={styles.req}>*</span></label>
            <div className={styles.priceIn}>
              <input
                id="price" type="text" className={styles.input}
                inputMode="numeric" placeholder="0"
                value={price} onChange={handlePriceInput}
                disabled={isFree}
                aria-invalid={priceHint.err ? 'true' : 'false'}
              />
              <span className={styles.won}>원</span>
            </div>
            <div className={styles.freeRow}>
              <input
                type="checkbox" id="isFree"
                checked={isFree}
                onChange={e => handleFreeChange(e.target.checked)}
              />
              <label htmlFor="isFree">나눔 (무료로 나눠요)</label>
            </div>
            <div className={hintCls(priceHint)}>{priceHint.text}</div>
          </div>

          {/* 상품 설명 */}
          <div className={styles.field}>
            <label htmlFor="desc">상품 설명<span className={styles.req}>*</span></label>
            <textarea
              id="desc" className={styles.textarea}
              placeholder="상품 상태, 사용 기간, 거래 방식 등을 자세히 적어주세요."
              value={desc} onChange={e => setDesc(e.target.value)}
              aria-invalid={descHint.err ? 'true' : 'false'}
            />
            <div className={hintCls(descHint)}>{descHint.text}</div>
          </div>

          {/* 액션 */}
          <div className={styles.actions}>
            <Link href="/products" className={styles.btnGhost}>취소</Link>
            <button type="submit" className={styles.btnSubmit} disabled={submitting}>
              {submitting
                ? (isEdit ? '수정 중...' : '등록 중...')
                : (isEdit ? '수정 완료' : '등록하기')}
            </button>
          </div>
          <div className={styles.apiNote}>
            {isEdit ? `PATCH /api/products/${editId}` : 'POST /api/products'}
          </div>
        </form>
      </div>
    </main>
  )
}
