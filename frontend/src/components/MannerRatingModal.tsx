'use client'

import { useState } from 'react'
import { apiFetch } from '@/lib/apiClient'
import styles from './MannerRatingModal.module.css'

const HINTS: Record<number, string> = {
  1: '아쉬운 거래였어요',
  2: '조금 아쉬웠어요',
  3: '보통이었어요',
  4: '좋은 거래였어요',
  5: '최고의 거래였어요!',
}

interface Props {
  productId: number
  productTitle: string
  rateeNickname: string
  onClose: () => void
  onSubmitted: () => void
}

export default function MannerRatingModal({ productId, productTitle, rateeNickname, onClose, onSubmitted }: Props) {
  const [score, setScore] = useState(0)
  const [hoverScore, setHoverScore] = useState(0)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  const shown = hoverScore || score

  async function submit() {
    if (score === 0 || submitting) return
    setSubmitting(true)
    setErrorMsg('')
    try {
      const res = await apiFetch('/api/manner/ratings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productId, score }),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        setErrorMsg(data?.message ?? '후기 등록 중 오류가 발생했습니다.')
        return
      }
      setDone(true)
      onSubmitted()
    } catch {
      setErrorMsg('서버에 연결할 수 없습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.overlay} onClick={e => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        {!done ? (
          <>
            <div className={styles.productLine}>{productTitle} · 거래완료</div>
            <h3>{rateeNickname}님과의 거래는 어떠셨나요?</h3>
            <div className={styles.stars}>
              {[1, 2, 3, 4, 5].map(v => (
                <button
                  key={v}
                  type="button"
                  className={`${styles.star}${v <= shown ? ' ' + styles.on : ''}`}
                  onMouseEnter={() => setHoverScore(v)}
                  onMouseLeave={() => setHoverScore(0)}
                  onClick={() => setScore(v)}
                  aria-label={`${v}점`}
                >
                  ★
                </button>
              ))}
            </div>
            <div className={styles.hint}>{shown ? HINTS[shown] : ''}</div>
            {errorMsg && <div className={styles.error}>{errorMsg}</div>}
            <div className={styles.actions}>
              <button type="button" onClick={onClose} disabled={submitting}>다음에 할게요</button>
              <button type="button" className={styles.submit} onClick={submit} disabled={score === 0 || submitting}>
                {submitting ? '등록 중...' : '등록하기'}
              </button>
            </div>
          </>
        ) : (
          <>
            <div className={styles.check}>✓</div>
            <h3>소중한 후기 감사해요</h3>
            <p className={styles.doneMsg}>{rateeNickname}님의 매너온도에 반영됐어요.</p>
            <div className={styles.actions}>
              <button type="button" className={styles.submit} style={{ flex: 1 }} onClick={onClose}>닫기</button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
