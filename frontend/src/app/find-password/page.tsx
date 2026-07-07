'use client'

import Link from 'next/link'
import { useState } from 'react'
import styles from './page.module.css'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type Hint = { text: string; kind?: string }
type MsgType = 'success' | 'error'

export default function FindPasswordPage() {
  const [email, setEmail] = useState('')
  const [emailHint, setEmailHint] = useState<Hint>({ text: '가입할 때 사용한 이메일을 입력하세요.' })
  const [formMsg, setFormMsg] = useState<{ text: string; type: MsgType } | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [sent, setSent] = useState(false)

  function validateEmail() {
    const v = email.trim()
    if (!v) { setEmailHint({ text: '이메일을 입력하세요.', kind: 'err' }); return false }
    if (!EMAIL_RE.test(v)) { setEmailHint({ text: '올바른 이메일 형식이 아닙니다.', kind: 'err' }); return false }
    setEmailHint({ text: '' })
    return true
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormMsg(null)
    if (!validateEmail()) return

    setSubmitting(true)
    try {
      const res = await fetch('/api/auth/password-resets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim() }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) {
        setFormMsg({ text: data?.message ?? '요청 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
        return
      }
      setSent(true)
      setFormMsg({ text: data?.message ?? '해당 이메일로 가입된 계정이 있다면 비밀번호 재설정 메일을 발송했습니다.', type: 'success' })
    } catch {
      setFormMsg({ text: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
    } finally {
      setSubmitting(false)
    }
  }

  const hintClass = (kind?: string) => [styles.hint, kind ? styles[kind] : ''].filter(Boolean).join(' ')
  const msgClass = formMsg ? [styles.formMsg, styles.show, styles[formMsg.type]].join(' ') : styles.formMsg

  return (
    <main className={styles.stage}>
      {/* 인트로 */}
      <div className={styles.intro}>
        <span className={styles.badge}>🔑 비밀번호 찾기</span>
        <h1>비밀번호를 잊으셨나요?</h1>
        <p>가입한 이메일로 비밀번호 재설정 링크를 보내드릴게요.</p>
      </div>

      <div className={styles.card}>
        <div className={msgClass} role="alert">{formMsg?.text}</div>

        {!sent ? (
          <form onSubmit={handleSubmit} className={styles.formCol} noValidate>
            <div className={styles.field}>
              <label htmlFor="email">이메일<span className={styles.req}>*</span></label>
              <input
                type="email"
                id="email"
                placeholder="example@email.com"
                autoComplete="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                onBlur={validateEmail}
                aria-invalid={emailHint.kind === 'err' ? 'true' : 'false'}
              />
              <div className={hintClass(emailHint.kind)}>{emailHint.text}</div>
            </div>

            <button type="submit" className="btn block" disabled={submitting}>
              {submitting ? '발송 중...' : '재설정 메일 보내기'}
            </button>
          </form>
        ) : (
          <Link href="/login" className="btn ghost block">로그인으로 돌아가기</Link>
        )}

        <div className={styles.foot}>
          <p>비밀번호가 기억나셨나요?</p>
          <Link href="/login" className="btn ghost block">로그인하기</Link>
        </div>
      </div>
    </main>
  )
}
