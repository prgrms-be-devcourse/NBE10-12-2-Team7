'use client'

import Link from 'next/link'
import { useEffect, useRef, useState } from 'react'
import { ACCESS_TOKEN_KEY, getAccessToken } from '@/lib/auth'
import styles from './page.module.css'

type MsgType = 'success' | 'error'
type PageStatus = 'loading' | 'ready' | 'unauthenticated' | 'error'

interface Member {
  memberId: number
  email: string
  nickname: string
  role: string
  status: string
  createdAt: string
}

export default function MyProfilePage() {
  const [status, setStatus] = useState<PageStatus>('loading')
  const [errorMsg, setErrorMsg] = useState('')
  const [member, setMember] = useState<Member | null>(null)

  const [nickname, setNickname] = useState('')
  const [nicknameHint, setNicknameHint] = useState<{ text: string; kind?: string }>({ text: '2~20자로 입력하세요. 다른 이웃에게 보여져요.' })

  const [formMsg, setFormMsg] = useState<{ text: string; type: MsgType } | null>(null)
  const [saving, setSaving] = useState(false)
  const [withdrawing, setWithdrawing] = useState(false)

  const [toastText, setToastText]       = useState('')
  const [toastVisible, setToastVisible] = useState(false)
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  function showToast(msg: string) {
    setToastText(msg)
    setToastVisible(true)
    if (toastTimer.current) clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToastVisible(false), 1900)
  }

  useEffect(() => {
    const token = getAccessToken()
    if (!token) { setStatus('unauthenticated'); return }

    let cancelled = false
    fetch('/api/members/me', { headers: { Authorization: `Bearer ${token}` } })
      .then(async res => {
        if (res.status === 401) { if (!cancelled) setStatus('unauthenticated'); return }
        const data = await res.json().catch(() => null)
        if (!res.ok) throw new Error(data?.message ?? '회원 정보를 불러오지 못했습니다.')
        if (!cancelled) {
          setMember(data?.data)
          setNickname(data?.data?.nickname ?? '')
          setStatus('ready')
        }
      })
      .catch(err => {
        if (cancelled) return
        setErrorMsg(err instanceof Error ? err.message : '회원 정보를 불러오지 못했습니다.')
        setStatus('error')
      })
    return () => { cancelled = true }
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormMsg(null)

    const nv = nickname.trim()
    if (nv.length < 2 || nv.length > 20) {
      setNicknameHint({ text: '닉네임은 2~20자로 입력하세요.', kind: 'err' })
      setFormMsg({ text: '입력값을 다시 확인해주세요.', type: 'error' })
      return
    }
    setNicknameHint({ text: '', kind: 'ok' })

    const token = getAccessToken()
    if (!token) { setStatus('unauthenticated'); return }

    setSaving(true)
    try {
      const res = await fetch('/api/members/me', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ nickname: nv }),
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) {
        setFormMsg({ text: data?.message ?? '수정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
        return
      }
      setMember(data?.data)
      setFormMsg({ text: '회원 정보를 수정했어요.', type: 'success' })
      showToast('수정 완료')
    } catch {
      setFormMsg({ text: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', type: 'error' })
    } finally {
      setSaving(false)
    }
  }

  async function handleWithdraw() {
    if (!window.confirm('정말 탈퇴하시겠어요? 이 작업은 되돌릴 수 없어요.')) return

    const token = getAccessToken()
    if (!token) { setStatus('unauthenticated'); return }

    setWithdrawing(true)
    try {
      const res = await fetch('/api/members/me', {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        showToast(data?.message ?? '탈퇴 처리 중 오류가 발생했습니다.')
        setWithdrawing(false)
        return
      }
      try { localStorage.removeItem(ACCESS_TOKEN_KEY) } catch {}
      showToast('탈퇴 처리되었습니다')
      setTimeout(() => { window.location.href = '/login' }, 1200)
    } catch {
      showToast('서버에 연결할 수 없습니다.')
      setWithdrawing(false)
    }
  }

  const hintClass = (kind?: string) =>
    [styles.hint, kind ? styles[kind] : ''].filter(Boolean).join(' ')

  const msgClass = formMsg
    ? [styles.formMsg, styles.show, styles[formMsg.type]].join(' ')
    : styles.formMsg

  if (status === 'loading') {
    return (
      <main className={styles.wrap}>
        <div className={styles.intro}>
          <h1>내 정보</h1>
          <p>계정 정보를 확인하고 프로필을 수정할 수 있어요.</p>
        </div>
        <div className={styles.notice}><p>불러오는 중...</p></div>
      </main>
    )
  }

  if (status === 'unauthenticated') {
    return (
      <main className={styles.wrap}>
        <div className={styles.intro}>
          <h1>내 정보</h1>
          <p>계정 정보를 확인하고 프로필을 수정할 수 있어요.</p>
        </div>
        <div className={styles.notice}>
          <p>로그인 후 내 정보를 확인할 수 있어요.</p>
          <Link href="/login" className={`btn ${styles.noticeBtn}`}>로그인하기</Link>
        </div>
      </main>
    )
  }

  if (status === 'error' || !member) {
    return (
      <main className={styles.wrap}>
        <div className={styles.intro}>
          <h1>내 정보</h1>
          <p>계정 정보를 확인하고 프로필을 수정할 수 있어요.</p>
        </div>
        <div className={styles.notice}><p>{errorMsg}</p></div>
      </main>
    )
  }

  return (
    <main className={styles.wrap}>
      {/* 인트로 */}
      <div className={styles.intro}>
        <h1>내 정보</h1>
        <p>계정 정보를 확인하고 프로필을 수정할 수 있어요.</p>
      </div>

      {/* 프로필 요약 */}
      <div className={styles.profile}>
        <div className={styles.avatar}>{member.nickname.charAt(0)}</div>
        <div className={styles.who}>
          <div className={styles.nm}>{member.nickname}</div>
          <div className={styles.em}>{member.email}</div>
          <div className={styles.st}>
            <span className="tag active">{member.status === 'ACTIVE' ? '정상 회원' : member.status}</span>
          </div>
        </div>
      </div>

      {/* 프로필 수정 카드 */}
      <div className={styles.card}>
        <h2>프로필 수정</h2>
        <div className={msgClass} role="alert">
          {formMsg?.text}
        </div>
        <form onSubmit={handleSubmit} noValidate>
          <div className={styles.field}>
            <label>이메일<span className={styles.lock}>🔒 변경 불가</span></label>
            <input type="email" value={member.email} disabled />
            <div className={styles.hint}>이메일은 계정 식별자로 변경할 수 없어요.</div>
          </div>

          <div className={styles.field}>
            <label htmlFor="nickname">
              닉네임<span style={{ color: 'var(--primary)', marginLeft: 3 }}>*</span>
            </label>
            <input
              type="text"
              id="nickname"
              value={nickname}
              onChange={e => setNickname(e.target.value)}
              maxLength={20}
              aria-invalid={nicknameHint.kind === 'err' ? 'true' : 'false'}
            />
            <div className={hintClass(nicknameHint.kind)}>
              {nicknameHint.text || '2~20자로 입력하세요. 다른 이웃에게 보여져요.'}
            </div>
          </div>

          <button type="submit" className="btn block" disabled={saving}>
            {saving ? '저장 중...' : '정보 수정'}
          </button>
          <div className={styles.apiNote}>GET /api/members/me · PATCH /api/members/me</div>
        </form>
      </div>

      {/* 계정 관리 카드 */}
      <div className={styles.card}>
        <h2>계정 관리</h2>
        <p className={styles.withdraw}>
          회원 탈퇴 시 계정 상태가 <b>탈퇴(DELETED)</b>로 변경되고 등록한 상품과 정보에 접근할 수
          없게 돼요. 이 작업은 되돌릴 수 없습니다.
        </p>
        <button className="btn danger" onClick={handleWithdraw} type="button" disabled={withdrawing}>
          {withdrawing ? '탈퇴 처리 중...' : '회원 탈퇴'}
        </button>
        <div className={styles.apiNote}>DELETE /api/members/me</div>
      </div>

      {/* 토스트 */}
      <div className={`toast${toastVisible ? ' show' : ''}`}>{toastText}</div>
    </main>
  )
}
