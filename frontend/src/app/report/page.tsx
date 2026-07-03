'use client'

import Link from 'next/link'
import { Suspense, useEffect, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { apiFetch, bootstrapAutoLogin } from '@/lib/apiClient'
import { getAccessToken } from '@/lib/auth'
import { REPORT_REASONS, type ReportReason } from '@/lib/reportReasons'
import styles from './page.module.css'

type LoadStatus = 'checking' | 'ready' | 'unauthenticated'

type TargetType = 'product' | 'member'

const PRODUCT_ICON = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M3 9l9-6 9 6v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    <path d="M9 21V12h6v9" />
  </svg>
)
const MEMBER_ICON = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
  </svg>
)

function ReportForm() {
  const params     = useSearchParams()
  const type       = (params.get('type') === 'member' ? 'member' : 'product') as TargetType
  const targetId   = params.get('id') ?? ''

  const isProduct  = type === 'product'
  const cancelHref = isProduct ? `/products/${targetId}` : `/products`
  const apiEndpoint = isProduct
    ? `POST /api/products/${targetId}/reports`
    : `POST /api/members/${targetId}/reports`

  const targetLabel = isProduct ? '상품 신고' : '사용자 신고'
  const targetName = `${isProduct ? '상품' : '회원'} #${targetId}`

  const [reason,  setReason]  = useState<ReportReason | ''>('')
  const [content, setContent] = useState('')
  const [reasonErr,  setReasonErr]  = useState('')
  const [contentErr, setContentErr] = useState('')
  const [formMsg, setFormMsg] = useState<{ text: string; type: 'success' | 'error' } | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [loadStatus, setLoadStatus] = useState<LoadStatus>('checking')

  useEffect(() => {
    let cancelled = false
    async function init() {
      // accessToken이 없어도 refreshToken이 남아있으면 재발급을 먼저 시도한다(자동 로그인).
      if (!getAccessToken()) await bootstrapAutoLogin()
      if (!cancelled) setLoadStatus(getAccessToken() ? 'ready' : 'unauthenticated')
    }
    init()
    return () => { cancelled = true }
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormMsg(null)
    let valid = true

    if (!reason) {
      setReasonErr('신고 사유를 선택하세요.')
      valid = false
    } else {
      setReasonErr('')
    }
    if (!content.trim()) {
      setContentErr('신고 내용을 입력하세요.')
      valid = false
    } else {
      setContentErr('')
    }
    if (!valid) {
      setFormMsg({ text: '신고 사유와 내용을 확인해주세요.', type: 'error' })
      return
    }

    if (!targetId) {
      setFormMsg({ text: '신고 대상을 확인할 수 없습니다. 목록에서 다시 시도해주세요.', type: 'error' })
      return
    }

    setSubmitting(true)
    const url = isProduct
      ? `/api/products/${targetId}/reports`
      : `/api/members/${targetId}/reports`

    try {
      const res = await apiFetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason, content: content.trim() }),
      })

      const data = await res.json().catch(() => null)

      if (!res.ok) {
        setFormMsg({ text: data?.message ?? '신고 접수 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
        setSubmitting(false)
        return
      }

      setFormMsg({ text: '신고가 접수되었어요. 내 신고 내역에서 처리 상태를 확인할 수 있어요.', type: 'success' })
      setTimeout(() => { window.location.href = '/my-reports' }, 1200)
    } catch {
      setFormMsg({ text: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
      setSubmitting(false)
    }
  }

  const msgClass = formMsg
    ? [styles.formMsg, styles.show, styles[formMsg.type]].join(' ')
    : styles.formMsg

  if (loadStatus === 'checking') {
    return (
      <main className={styles.wrap}>
        <div className={styles.intro}>
          <span className={styles.badge}>🚨 신고 접수</span>
          <h1>신고 작성</h1>
          <p>부적절한 상품이나 사용자를 신고해주세요. 접수된 신고는 운영팀이 검토합니다.</p>
        </div>
        <div className={styles.card}><p>불러오는 중...</p></div>
      </main>
    )
  }

  if (loadStatus === 'unauthenticated') {
    return (
      <main className={styles.wrap}>
        <div className={styles.intro}>
          <span className={styles.badge}>🚨 신고 접수</span>
          <h1>신고 작성</h1>
          <p>부적절한 상품이나 사용자를 신고해주세요. 접수된 신고는 운영팀이 검토합니다.</p>
        </div>
        <div className={styles.card}>
          <p className={styles.loginNotice}>로그인 후 신고를 접수할 수 있어요.</p>
          <Link href="/login" className="btn block">로그인하기</Link>
        </div>
      </main>
    )
  }

  return (
    <main className={styles.wrap}>
      {/* 인트로 */}
      <div className={styles.intro}>
        <span className={styles.badge}>🚨 신고 접수</span>
        <h1>신고 작성</h1>
        <p>부적절한 상품이나 사용자를 신고해주세요. 접수된 신고는 운영팀이 검토합니다.</p>
      </div>

      <div className={styles.card}>
        {/* 폼 메시지 */}
        <div className={msgClass} role="alert">{formMsg?.text}</div>

        {/* 신고 대상 */}
        <div className={styles.target}>
          <div className={styles.targetIc}>
            {isProduct ? PRODUCT_ICON : MEMBER_ICON}
          </div>
          <div className={styles.targetInfo}>
            <div className={styles.targetKind}>{targetLabel}</div>
            <div className={styles.targetNm}>{targetName}</div>
          </div>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          {/* 신고 사유 */}
          <div className={styles.field}>
            <label>신고 사유<span className={styles.req}>*</span></label>
            <div className={styles.reasons}>
              {REPORT_REASONS.map(r => (
                <div
                  key={r.value}
                  className={`${styles.reason}${reason === r.value ? ' ' + styles.sel : ''}`}
                  onClick={() => { setReason(r.value); setReasonErr('') }}
                  role="radio"
                  aria-checked={reason === r.value}
                  tabIndex={0}
                  onKeyDown={e => e.key === 'Enter' && setReason(r.value)}
                >
                  <span className={styles.radio}>
                    {reason === r.value && <span className={styles.radioInner} />}
                  </span>
                  <span className={styles.reasonTxt}>{r.description}</span>
                </div>
              ))}
            </div>
            {reasonErr && <div className={`${styles.hint} ${styles.err}`}>{reasonErr}</div>}
          </div>

          {/* 신고 내용 */}
          <div className={styles.field}>
            <label htmlFor="content">
              신고 내용<span className={styles.req}>*</span>
            </label>
            <textarea
              id="content"
              className={styles.textarea}
              maxLength={500}
              placeholder="신고 사유를 구체적으로 작성해주세요. 자세히 적을수록 빠르게 처리돼요."
              value={content}
              onChange={e => { setContent(e.target.value); setContentErr('') }}
              aria-invalid={!!contentErr ? 'true' : 'false'}
            />
            <div className={styles.counter}>{content.length} / 500</div>
            {contentErr && <div className={`${styles.hint} ${styles.err}`}>{contentErr}</div>}
          </div>

          {/* 안내 */}
          <div className={styles.notice}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 8v5M12 16.5v.5" />
            </svg>
            <span>허위 신고나 악의적 반복 신고는 이용 제한 사유가 될 수 있어요. 신고 내용은 운영팀만 확인합니다.</span>
          </div>

          {/* 액션 */}
          <div className={styles.actions}>
            <Link href={cancelHref} className={styles.btnGhost}>취소</Link>
            <button type="submit" className={styles.btnSubmit} disabled={submitting}>
              {submitting ? '신고 접수 중...' : '신고하기'}
            </button>
          </div>
          <div className={styles.apiNote}>{apiEndpoint}</div>
        </form>
      </div>
    </main>
  )
}

/* useSearchParams는 Suspense 경계 안에서만 사용 가능 */
export default function ReportPage() {
  return (
    <Suspense>
      <ReportForm />
    </Suspense>
  )
}
