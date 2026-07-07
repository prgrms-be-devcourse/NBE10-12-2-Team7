'use client'

import Link from 'next/link'
import { useState } from 'react'
import { setAccessToken } from '@/lib/auth'
import styles from './page.module.css'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type Hint = { text: string; kind?: string }
type MsgType = 'success' | 'error'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [autoLogin, setAutoLogin] = useState(false)

  const [emailHint, setEmailHint] = useState<Hint>({ text: '' })
  const [passwordHint, setPasswordHint] = useState<Hint>({ text: '' })

  const [formMsg, setFormMsg] = useState<{ text: string; type: MsgType } | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function validateEmail(v: string) {
    if (!v) { setEmailHint({ text: '이메일을 입력하세요.', kind: 'err' }); return false }
    if (!EMAIL_RE.test(v)) { setEmailHint({ text: '올바른 이메일 형식이 아닙니다.', kind: 'err' }); return false }
    setEmailHint({ text: '' })
    return true
  }

  function validatePassword(v: string) {
    if (!v) { setPasswordHint({ text: '비밀번호를 입력하세요.', kind: 'err' }); return false }
    setPasswordHint({ text: '' })
    return true
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormMsg(null)
    const valid = [validateEmail(email.trim()), validatePassword(password)].every(Boolean)
    if (!valid) { setFormMsg({ text: '이메일과 비밀번호를 확인해주세요.', type: 'error' }); return }

    setSubmitting(true)
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim(), password, autoLogin }),
      })
      const data = await res.json().catch(() => null)

      if (!res.ok) {
        setFormMsg({ text: data?.message ?? '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
        setSubmitting(false)
        return
      }

      const accessToken = data?.data?.accessToken
      if (accessToken) {
        setAccessToken(accessToken)
      }
      setFormMsg({ text: '로그인되었습니다. 이동합니다.', type: 'success' })
      setTimeout(() => { window.location.href = '/products' }, 900)
    } catch {
      setFormMsg({ text: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
      setSubmitting(false)
    }
  }

  const hintClass = (kind?: string) => [styles.hint, kind ? styles[kind] : ''].filter(Boolean).join(' ')
  const msgClass = formMsg ? [styles.formMsg, styles.show, styles[formMsg.type]].join(' ') : styles.formMsg

  return (
    <main className={styles.stage}>
      {/* 인트로 */}
      <div className={styles.intro}>
        <span className={styles.badge}>🌷 다시 오신 걸 환영해요</span>
        <h1>Market<span style={{ color: 'var(--primary)' }}>ON</span> 로그인</h1>
        <p>우리 동네 거래가 가장 활발한 곳, 다시 시작해요.</p>
      </div>

      <div className={styles.card}>
        <div className={msgClass} role="alert">{formMsg?.text}</div>

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
              onBlur={e => e.target.value && validateEmail(e.target.value.trim())}
              aria-invalid={emailHint.kind === 'err' ? 'true' : 'false'}
            />
            <div className={hintClass(emailHint.kind)}>{emailHint.text}</div>
          </div>

          <div className={styles.field}>
            <label htmlFor="password">비밀번호<span className={styles.req}>*</span></label>
            <input
              type="password"
              id="password"
              placeholder="비밀번호를 입력하세요"
              autoComplete="current-password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              aria-invalid={passwordHint.kind === 'err' ? 'true' : 'false'}
            />
            <div className={hintClass(passwordHint.kind)}>{passwordHint.text}</div>
          </div>

          <div className={styles.autoLoginRow}>
            <div className={styles.autoLoginCheck}>
              <input
                type="checkbox"
                id="autoLogin"
                checked={autoLogin}
                onChange={e => setAutoLogin(e.target.checked)}
              />
              <label htmlFor="autoLogin">자동 로그인</label>
            </div>
            <Link href="/find-password" className={styles.forgotLink}>비밀번호 찾기</Link>
          </div>

          <button type="submit" className="btn block" disabled={submitting}>
            {submitting ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <div className={styles.foot}>아직 계정이 없으신가요? <Link href="/signup">회원가입</Link></div>
      </div>
    </main>
  )
}
