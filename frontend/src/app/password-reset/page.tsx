'use client'

import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import { Suspense, useState } from 'react'
import styles from './page.module.css'

const NEW_PW_RE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{10,64}$/

type Hint = { text: string; kind?: string }
type MsgType = 'success' | 'error'

function PasswordResetForm() {
  const searchParams = useSearchParams()
  const token = searchParams.get('token') ?? ''

  const [newPassword, setNewPassword] = useState('')
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('')
  const [newPasswordHint, setNewPasswordHint] = useState<Hint>({ text: '영문·숫자·특수문자를 모두 포함해 10~64자로 입력하세요.' })
  const [newPasswordConfirmHint, setNewPasswordConfirmHint] = useState<Hint>({ text: '' })

  const [formMsg, setFormMsg] = useState<{ text: string; type: MsgType } | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  function validateNewPassword() {
    if (!NEW_PW_RE.test(newPassword)) {
      setNewPasswordHint({ text: '영문·숫자·특수문자를 모두 포함해 10~64자로 입력하세요.', kind: 'err' })
      return false
    }
    setNewPasswordHint({ text: '사용 가능한 비밀번호입니다.', kind: 'ok' })
    return true
  }

  function validateNewPasswordConfirm() {
    if (newPasswordConfirm !== newPassword) {
      setNewPasswordConfirmHint({ text: '비밀번호가 일치하지 않습니다.', kind: 'err' })
      return false
    }
    setNewPasswordConfirmHint({ text: '비밀번호가 일치합니다.', kind: 'ok' })
    return true
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormMsg(null)
    const valid = [validateNewPassword(), validateNewPasswordConfirm()].every(Boolean)
    if (!valid) { setFormMsg({ text: '입력값을 다시 확인해주세요.', type: 'error' }); return }

    setSubmitting(true)
    try {
      const res = await fetch('/api/auth/password-resets/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, newPassword }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) {
        setFormMsg({ text: data?.message ?? '비밀번호 재설정 중 오류가 발생했습니다.', type: 'error' })
        return
      }
      setDone(true)
      setFormMsg({ text: data?.message ?? '비밀번호가 재설정되었습니다. 로그인 화면으로 이동합니다.', type: 'success' })
      setTimeout(() => { window.location.href = '/login' }, 1200)
    } catch {
      setFormMsg({ text: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
    } finally {
      setSubmitting(false)
    }
  }

  const hintClass = (kind?: string) => [styles.hint, kind ? styles[kind] : ''].filter(Boolean).join(' ')
  const msgClass = formMsg ? [styles.formMsg, styles.show, styles[formMsg.type]].join(' ') : styles.formMsg

  if (!token) {
    return (
      <main className={styles.stage}>
        <div className={styles.intro}>
          <span className={styles.badge}>🔑 비밀번호 재설정</span>
          <h1>유효하지 않은 링크예요</h1>
          <p>비밀번호 재설정 링크가 올바르지 않거나 만료됐어요.</p>
        </div>
        <div className={styles.card}>
          <Link href="/find-password" className="btn block">비밀번호 찾기 다시 요청하기</Link>
        </div>
      </main>
    )
  }

  return (
    <main className={styles.stage}>
      {/* 인트로 */}
      <div className={styles.intro}>
        <span className={styles.badge}>🔑 비밀번호 재설정</span>
        <h1>새 비밀번호를 설정하세요</h1>
        <p>새로 사용할 비밀번호를 입력해주세요.</p>
      </div>

      <div className={styles.card}>
        <div className={msgClass} role="alert">{formMsg?.text}</div>

        {!done && (
          <form onSubmit={handleSubmit} className={styles.formCol} noValidate>
            <div className={styles.field}>
              <label htmlFor="newPassword">새 비밀번호<span className={styles.req}>*</span></label>
              <input
                type="password"
                id="newPassword"
                autoComplete="new-password"
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
                onBlur={validateNewPassword}
                aria-invalid={newPasswordHint.kind === 'err' ? 'true' : 'false'}
              />
              <div className={hintClass(newPasswordHint.kind)}>{newPasswordHint.text}</div>
            </div>

            <div className={styles.field}>
              <label htmlFor="newPasswordConfirm">새 비밀번호 확인<span className={styles.req}>*</span></label>
              <input
                type="password"
                id="newPasswordConfirm"
                autoComplete="new-password"
                value={newPasswordConfirm}
                onChange={e => setNewPasswordConfirm(e.target.value)}
                onBlur={validateNewPasswordConfirm}
                aria-invalid={newPasswordConfirmHint.kind === 'err' ? 'true' : 'false'}
              />
              <div className={hintClass(newPasswordConfirmHint.kind)}>{newPasswordConfirmHint.text}</div>
            </div>

            <button type="submit" className="btn block" disabled={submitting}>
              {submitting ? '변경 중...' : '비밀번호 재설정'}
            </button>
          </form>
        )}
      </div>
    </main>
  )
}

export default function PasswordResetPage() {
  return (
    <Suspense fallback={<main className={styles.stage}><div className={styles.card}><p>불러오는 중...</p></div></main>}>
      <PasswordResetForm />
    </Suspense>
  )
}
